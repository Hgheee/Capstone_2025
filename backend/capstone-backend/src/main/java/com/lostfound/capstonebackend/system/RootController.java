package com.lostfound.capstonebackend.system;

import com.lostfound.capstonebackend.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public ApiResponse<String> root() {
        return ApiResponse.ok("Lost & Found Backend API v1.0 - Server is running");
    }
}
