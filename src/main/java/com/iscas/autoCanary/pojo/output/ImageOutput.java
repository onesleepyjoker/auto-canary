package com.iscas.autoCanary.pojo.output;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageOutput{
    /**
     * 镜像名称
     */
    private String imageName;

    /**
     * 版本
     */
    private String version;
}
