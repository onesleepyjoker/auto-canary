package com.iscas.autoCanary.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "task")
@Data
public class Task implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否成功，0代表成功，1代表失败
     */
    private Integer isSuccess;

    /**
     * 任务运行日志
     */
    private String logInformation;

    /**
<<<<<<< HEAD
     * 镜像版本数据
=======
     * 任务镜像列表
>>>>>>> origin/master
     */
    private String imageList;

    /**
     * 创建任务时间
     */
    private Date createTime;

    /**
     *
     */
    private Date updateTime;

    /**
     * 逻辑删除
     */
    private Integer isDelete;

    /**
     * 用户Id
     */
    private Long userId;

    /**
     * 任务失败原因
     */
    private String reason;


}
