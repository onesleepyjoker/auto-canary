package com.iscas.autoCanary.model.domain.request;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 镜像添加请求封装类
 * @TableName image
 */
@Data
public class ImageCreateRequest implements Serializable {

    /**
     * 镜像名称
     */
    private String imageName;

    /**
     * 版本
     */
    private String version;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 镜像地址
     */
    private String imageUrl;

    private static final long serialVersionUID = 1L;

}