package com.lostfound.capstonebackend.domain.seoul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class SeoulLostResponse {

    @JsonProperty("lostArticleInfo")
    private LostArticleInfo lostArticleInfo;

    @Data
    public static class LostArticleInfo {
        @JsonProperty("list_total_count")
        private Integer listTotalCount;

        @JsonProperty("RESULT")
        private Result result;

        @JsonProperty("row")
        private List<SeoulLostRow> row;
    }

    @Data
    public static class Result {
        @JsonProperty("CODE")
        private String code;

        @JsonProperty("MESSAGE")
        private String message;
    }
}
