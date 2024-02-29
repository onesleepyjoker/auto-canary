package com.iscas.autoCanary.controller;

import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iscas.autoCanary.common.BaseResponse;
import com.iscas.autoCanary.common.DeleteRequest;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.common.ResultUtils;
import com.iscas.autoCanary.contant.UserConstant;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.model.domain.request.ImageCreateRequest;
import com.iscas.autoCanary.model.domain.request.ImageUpdateRequest;
import com.iscas.autoCanary.model.dto.ImageQuery;
import com.iscas.autoCanary.pojo.Image;
import com.iscas.autoCanary.pojo.User;
import com.iscas.autoCanary.service.ImageService;
import com.iscas.autoCanary.service.UserService;
import io.kubernetes.client.openapi.ApiException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * TODO 类描述
 *
 * @author 一只小小丑
 * @date 2024/2/2 17:27
 */
@RestController
@RequestMapping("/img")
public class ImageController {

    @Autowired
    private ImageService imageService;

    @Autowired
    private UserService userService;

    //    数据库同步 （将华为云数据库当中镜像导入到本地数据库当中）
    @PostMapping("/synchronize")
    public BaseResponse<Integer> synchronizeImage(HttpServletRequest request, @RequestParam(required = false) String namespace) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        return ResultUtils.success(imageService.synchronizeImage(request, namespace));
    }

    //    增加镜像
    @PostMapping("/create")
    public BaseResponse<Long> createImage(HttpServletRequest request, @RequestBody ImageCreateRequest imageCreateRequest) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Image image = new Image();
        BeanUtils.copyProperties(imageCreateRequest, image);

        long imageId = imageService.createImage(image, loginUser);
        return ResultUtils.success(imageId);
    }

    @PostMapping("/update")
    public BaseResponse<Long> updateImage(HttpServletRequest request, @RequestBody ImageUpdateRequest imageUpdateRequest) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Image image = new Image();
        BeanUtils.copyProperties(imageUpdateRequest, image);

        long imageId = imageService.updateImage(image, loginUser);
        return ResultUtils.success(imageId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> updateImage(HttpServletRequest request, @RequestBody DeleteRequest deleteRequest) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        boolean b = imageService.deleteImage(deleteRequest.getId(), loginUser);
        return ResultUtils.success(b);
    }

    //    分页查询
    @PostMapping("/list/page")
    public BaseResponse<Page<Image>> listImagebyPage(HttpServletRequest request, @RequestBody ImageQuery imageQuery) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        if (imageQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

//        分页查询逻辑
        int pageNum = imageQuery.getPageNum();
        int pageSize = imageQuery.getPageSize();
        if (pageSize > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "单页查询数量过多");//限制爬虫
        }
        Image image = new Image();
        BeanUtils.copyProperties(imageQuery, image);
        Page<Image> imagePage = imageService.page(new Page<>(
                        pageNum, pageSize),
                new QueryWrapper<>(image));
        return ResultUtils.success(imagePage);
    }

    //    存储镜像的配置文件
    @PostMapping("/save/yaml")
    public BaseResponse<String> createDeployment(HttpServletRequest request, long id, @RequestParam("file") MultipartFile multipartFile) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        String yaml = "";
        try {
            // 将MultipartFile转换为字符串
            String fileContent = new String(multipartFile.getBytes(), "UTF-8");
            yaml = fileContent;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件解析失败");
        }
        Image image = imageService.getById(id);
        image.setYaml(yaml);
        imageService.updateById(image);
        return ResultUtils.success("镜像配置文件上传成功");
    }

    //    查询镜像的yaml配置文件
    @GetMapping("/get/yaml")
    public BaseResponse<String> getStatefulsetStatus(HttpServletRequest request, long id) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        Image image = imageService.getById(id);
        return ResultUtils.success(image.getYaml());
    }
}
