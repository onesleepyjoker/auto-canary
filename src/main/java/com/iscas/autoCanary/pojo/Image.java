package com.iscas.autoCanary.pojo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * 镜像表
 * @TableName image
 */
@TableName(value ="image")
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Image implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 镜像名称
     */
    @TableField("imageName")
    private String imageName;

    /**
     * 版本
     */
    private String version;

    /**
     * 版本
     */
    private String namespace;

    /**
     * 是否可用
     */
    private Integer imageStatus;

    /**
     * 镜像地址
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    @TableField("createTime")
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 配置文件
     */
    private String yaml;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建镜像的用户id
     */
    private Long userId;

    /**
     * 版本号（git一次commit的ID）
     */
    private String commitId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 对象去重
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return Objects.equals(imageName, image.imageName) && Objects.equals(version, image.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageName, version);
    }
}