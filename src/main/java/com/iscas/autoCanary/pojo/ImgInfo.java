package com.iscas.autoCanary.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class ImgInfo {
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("path")
    private String path;
    @JsonProperty("namespace")
    private String namespace;
    @JsonProperty("tags")
    private List<String> tags;
    @JsonProperty("total_ranage")
    private Integer totalRanage;
}
