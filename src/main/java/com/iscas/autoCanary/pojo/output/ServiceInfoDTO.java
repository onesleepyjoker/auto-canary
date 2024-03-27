package com.iscas.autoCanary.pojo.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.iscas.autoCanary.pojo.Image;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author windpo
 * @description 线上运行的Deployment信息以及本地数据库中的镜像列表信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceInfoDTO {
    @JsonProperty("service_name")
    private String serviceName;
    @JsonProperty("current_version")
    private String imageCurVersion;
    @JsonProperty("image_name")
    private String imageName;
    @JsonProperty("images")
    private List<Image> images;
}
