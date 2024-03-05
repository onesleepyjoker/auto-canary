package com.iscas.autoCanary.model.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.iscas.autoCanary.common.PageRequest;
import lombok.Data;

import java.util.Date;

/**
 * task和username的合并类
 * @author 一只小小丑
 * @date 2024/3/4 15:02
 *
 *
 */
@Data
public class TaskDto{
    /**
     * id
     */
    private Long id;

    /**
     * 是否成功
     */
    private Integer isSuccess;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 用户名称
     */
    private String username;


}
