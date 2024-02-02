package com.iscas.autoCanary.pojo;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 镜像关系表
 * @TableName imagecapabilities
 */
@TableName(value ="imagecapabilities")
@Data
public class Imagecapabilities implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 是否可用
     */
    private Integer imageStatus;

    /**
     * 镜像版本数据
     */
    private String imageList;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建镜像关系的用户id
     */
    private Integer userId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}