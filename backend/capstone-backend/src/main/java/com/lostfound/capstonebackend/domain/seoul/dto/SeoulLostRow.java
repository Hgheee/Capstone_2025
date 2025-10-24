package com.lostfound.capstonebackend.domain.seoul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SeoulLostRow {

    @JsonProperty("LOST_MNG_NO")
    private String lostMngNo;

    @JsonProperty("LOST_NM")
    private String lostNm;

    @JsonProperty("LGS_DTL_CN")
    private String lgsDtlCn;

    @JsonProperty("LOST_KND")
    private String lostKnd;

    @JsonProperty("CSTD_PLC")
    private String cstdPlc;

    @JsonProperty("REG_YMD")
    private String regYmd;

    @JsonProperty("RCV_YMD")
    private String rcvYmd;

    @JsonProperty("RCPL")
    private String rcpl;

    @JsonProperty("LOST_STTS")
    private String lostStts;

    @JsonProperty("INQ_CNT")
    private String inqCnt;
}
