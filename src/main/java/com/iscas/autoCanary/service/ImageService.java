package com.iscas.autoCanary.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.iscas.autoCanary.model.dto.ImageQuery;
import com.iscas.autoCanary.pojo.Image;
import com.baomidou.mybatisplus.extension.service.IService;
import com.iscas.autoCanary.pojo.User;
import com.iscas.autoCanary.pojo.output.ServiceInfoDTO;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.kubernetes.client.openapi.ApiException;

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

    //  查询线上运行服务的镜像版本列表
    List<ServiceInfoDTO> getServingImgs() throws ApiException;

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

//    查询镜像的类型（有负载还是无负载）
    String getImageType(long id);


    List<ServiceInfoDTO> getUnusedImages() throws ApiException;

    void delImages(List<Long> imageList);
}
