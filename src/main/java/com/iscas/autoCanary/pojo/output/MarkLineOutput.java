package com.iscas.autoCanary.pojo.output;


import com.iscas.autoCanary.pojo.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
/**
 * @author windpo
 * @description 镜像标签
 */
public class MarkLineOutput {

    /**
     * 标记id
     */
    private Long id;

    /**
     * 对该条标记的描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建镜像关系的用户id
     */
    private Long userId;

    /**
     * 一条标记路径
     */
    private List<ImageOutput> markLine;


}
