package com.iscas.autoCanary.model.dto;

import com.iscas.autoCanary.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * 队伍查询封装类
 *
 * @author <a href="https://github.com/onesleepyjoker/friend-backend">一只小小丑</a>
 * @from <a href="https://github.com/onesleepyjoker/friend-backend">一只小小丑</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ImageQuery extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * 镜像名称
     */
    private String imageName;

    /**
     * 版本信息
     */
    private String version;

    /**
     * 命名空间
     */
    private String namespace;



//    存储地址
    private String imageURL;
}
