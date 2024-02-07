package com.iscas.autoCanary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.iscas.autoCanary.model.dto.ImageQuery;
import com.iscas.autoCanary.pojo.Image;
import com.baomidou.mybatisplus.extension.service.IService;
import com.iscas.autoCanary.pojo.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 一只小小丑
 * @description 针对表【image(镜像表)】的数据库操作Service
 * @createDate 2024-02-02 09:48:54
 */
public interface ImageService extends IService<Image> {

    /**
     * 镜像同步
     *
     * @return int 1成功
     */
    Integer synchronizeImage(HttpServletRequest request, String namespace);


    //    镜像查询
    List<Image> listImages(ImageQuery imageQuery);

    //    镜像添加
    long  createImage(Image image, User userLogin);

    //    镜像删除
    boolean deleteImage(long id, User userLogin);

    //    镜像修改
    long updateImage(Image image, User userLogin);

    //    拼接查询语句
    QueryWrapper<Image> getQueryWrapper(ImageQuery imageQuery);


}
