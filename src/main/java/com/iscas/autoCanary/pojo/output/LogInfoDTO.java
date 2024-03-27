package com.iscas.autoCanary.pojo.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class LogInfoDTO {
    @JsonProperty("log")
    private List<String> log;
    @JsonProperty("isSuccess")
    private Boolean isSuccess;
}
