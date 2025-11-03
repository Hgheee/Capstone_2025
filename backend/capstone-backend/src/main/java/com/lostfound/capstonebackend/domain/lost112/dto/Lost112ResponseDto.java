package com.lostfound.capstonebackend.domain.lost112.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Data;

import java.util.List;

/**
 * LOST112 API의 전체 응답 구조를 나타내는 최상위 DTO입니다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lost112ResponseDto {

    /**
     * API 응답의 루트 요소
     */
    @JsonProperty("response")
    private Response response;

    /**
     * API 응답의 'response' 필드를 나타냅니다. 헤더와 바디를 포함합니다.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {

        /**
         * 응답 헤더
         */
        @JsonProperty("header")
        private Header header;

        /**
         * 응답 본문
         */
        @JsonProperty("body")
        @JsonDeserialize(using = BodyDeserializer.class)
        private Body body;
    }

    /**
     * API 응답의 'header' 필드를 나타냅니다. API 호출의 결과 코드를 포함합니다.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {

        /**
         * API 처리 결과 코드 (예: "00"은 성공)
         */
        @JsonProperty("resultCode")
        private String resultCode;

        /**
         * API 처리 결과 메시지
         */
        @JsonProperty("resultMsg")
        private String resultMsg;
    }

    /**
     * API 응답의 'body' 필드를 나타냅니다. 실제 데이터와 페이지네이션 정보를 포함합니다.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {

        /**
         * 분실물 데이터 목록을 감싸는 컨테이너
         */
        @JsonProperty("items")
        private Items items;

        /**
         * 한 페이지에 표시되는 항목의 수
         */
        @JsonProperty("numOfRows")
        private Integer numOfRows;

        /**
         * 현재 페이지 번호
         */
        @JsonProperty("pageNo")
        private Integer pageNo;

        /**
         * 전체 검색 결과의 수
         */
        @JsonProperty("totalCount")
        private Integer totalCount;
    }

    /**
     * 분실물 데이터 목록을 감싸는 'items' 필드를 나타냅니다.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {

        /**
         * 실제 분실물 데이터 DTO({@link Lost112ItemDto})의 리스트
         */
        @JsonProperty("item")
        private List<Lost112ItemDto> item;
    }

    /**
     * LOST112 응답에서 body 필드가 빈 문자열인 경우 null 로 처리하기 위한 디시리얼라이저.
     */
    public static class BodyDeserializer extends StdDeserializer<Body> {

        public BodyDeserializer() {
            super(Body.class);
        }

        @Override
        public Body deserialize(JsonParser p, DeserializationContext ctxt) throws java.io.IOException {
            JsonToken token = p.currentToken();
            if (token == JsonToken.VALUE_STRING) {
                String text = p.getValueAsString();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
            }

            ObjectCodec codec = p.getCodec();
            JsonNode node = codec.readTree(p);
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isTextual() && node.asText().trim().isEmpty()) {
                return null;
            }

            Body body = new Body();

            JsonNode itemsNode = node.get("items");
            if (itemsNode != null && !itemsNode.isNull()) {
                body.setItems(codec.treeToValue(itemsNode, Items.class));
            }

            JsonNode numOfRowsNode = node.get("numOfRows");
            if (numOfRowsNode != null && !numOfRowsNode.isNull()) {
                body.setNumOfRows(parseInteger(numOfRowsNode));
            }

            JsonNode pageNoNode = node.get("pageNo");
            if (pageNoNode != null && !pageNoNode.isNull()) {
                body.setPageNo(parseInteger(pageNoNode));
            }

            JsonNode totalCountNode = node.get("totalCount");
            if (totalCountNode != null && !totalCountNode.isNull()) {
                body.setTotalCount(parseInteger(totalCountNode));
            }

            return body;
        }

        private Integer parseInteger(JsonNode node) {
            if (node.isInt() || node.isIntegralNumber()) {
                return node.intValue();
            }
            if (node.isTextual()) {
                String text = node.asText();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                try {
                    return Integer.parseInt(text.trim());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }
    }
}
