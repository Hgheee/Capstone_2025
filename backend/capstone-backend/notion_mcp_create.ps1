param(
    [Parameter(Mandatory=$true)]
    [string]$NotionToken,
    [string]$AuthToken = "mcp-test-token",
    [int]$Port = 3333
)

$env:NOTION_TOKEN = $NotionToken

$job = Start-Job -ScriptBlock {
    param($token, $authToken, $port)
    $env:NOTION_TOKEN = $token
    npx -y @notionhq/notion-mcp-server --transport http --port $port --auth-token $authToken
} -ArgumentList $NotionToken, $AuthToken, $Port

try {
    $serverReady = $false
    for ($i = 0; $i -lt 30; $i++) {
        Start-Sleep -Milliseconds 500
        try {
            $health = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/health" -Method Get -TimeoutSec 2
            if ($health.StatusCode -eq 200) {
                $serverReady = $true
                break
            }
        } catch {
            continue
        }
    }

    if (-not $serverReady) {
        throw "Notion MCP server failed to start"
    }

    $headers = @{
        Authorization = "Bearer $AuthToken"
    }

    $initPayload = @{
        jsonrpc = "2.0"
        id = 1
        method = "initialize"
        params = @{
            capabilities = @{}
        }
    } | ConvertTo-Json -Depth 10

    $initResponse = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/mcp" -Method Post -Headers $headers -Body $initPayload -ContentType "application/json"

    $sessionId = $initResponse.Headers["mcp-session-id"]
    if (-not $sessionId) {
        $sessionId = $initResponse.Headers["Mcp-Session-Id"]
    }
    if (-not $sessionId) {
        throw "Failed to obtain MCP session id"
    }

    $headers["mcp-session-id"] = $sessionId

    $listPayload = @{
        jsonrpc = "2.0"
        id = 2
        method = "list_tools"
        params = @{}
    } | ConvertTo-Json -Depth 10

    $toolsResponse = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/mcp" -Method Post -Headers $headers -Body $listPayload -ContentType "application/json"
    $toolsData = $toolsResponse.Content | ConvertFrom-Json

    $createTool = $toolsData.result.tools | Where-Object {
        $_.name -match "pages" -and $_.description -match "Create"
    } | Select-Object -First 1

    if (-not $createTool) {
        throw "Create page tool not found"
    }

    $createArguments = @{
        body = @{
            parent = @{
                type = "workspace"
                workspace = $true
            }
            properties = @{
                title = @(
                    @{
                        type = "text"
                        text = @{
                            content = "Capstone Project"
                        }
                    }
                )
            }
            children = @(
                @{
                    object = "block"
                    type = "heading_1"
                    heading_1 = @{
                        rich_text = @(
                            @{
                                type = "text"
                                text = @{ content = "Capstone Project Overview" }
                            }
                        )
                    }
                },
                @{
                    object = "block"
                    type = "paragraph"
                    paragraph = @{
                        rich_text = @(
                            @{
                                type = "text"
                                text = @{ content = "This page was generated automatically via the Notion MCP server." }
                            }
                        )
                    }
                },
                @{
                    object = "block"
                    type = "bulleted_list_item"
                    bulleted_list_item = @{
                        rich_text = @(
                            @{ type = "text"; text = @{ content = "Goals" } }
                        )
                    }
                },
                @{
                    object = "block"
                    type = "bulleted_list_item"
                    bulleted_list_item = @{
                        rich_text = @(
                            @{ type = "text"; text = @{ content = "Timeline" } }
                        )
                    }
                },
                @{
                    object = "block"
                    type = "bulleted_list_item"
                    bulleted_list_item = @{
                        rich_text = @(
                            @{ type = "text"; text = @{ content = "Deliverables" } }
                        )
                    }
                }
            )
        }
    }

    $callPayload = @{
        jsonrpc = "2.0"
        id = 3
        method = "call_tool"
        params = @{
            name = $createTool.name
            arguments = $createArguments
        }
    } | ConvertTo-Json -Depth 10

    $callResponse = Invoke-WebRequest -Uri "http://127.0.0.1:$Port/mcp" -Method Post -Headers $headers -Body $callPayload -ContentType "application/json"
    $callData = $callResponse.Content | ConvertFrom-Json

    if ($null -eq $callData.result) {
        throw "Call tool failed: $($callData | ConvertTo-Json -Depth 10)"
    }

    $notionJson = $callData.result.content[0].text
    $notionData = $notionJson | ConvertFrom-Json

    $output = [PSCustomObject]@{
        tool = $createTool.name
        session = $sessionId
        notion = $notionData
    }

    $output | ConvertTo-Json -Depth 10
}
finally {
    if ($job) {
        if ($job.State -eq "Running") {
            Stop-Job -Job $job -Force | Out-Null
        }
        Remove-Job -Job $job | Out-Null
    }
}
