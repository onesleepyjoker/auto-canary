package com.iscas.autoCanary.controller;


import com.iscas.autoCanary.common.BaseResponse;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.common.ResultUtils;
import com.iscas.autoCanary.contant.UserConstant;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.pojo.Image;
import com.iscas.autoCanary.pojo.User;
import com.iscas.autoCanary.pojo.output.ImageOutput;
import com.iscas.autoCanary.pojo.output.MarkLineOutput;
import com.iscas.autoCanary.service.ImageCapabilitiesService;
import io.kubernetes.client.openapi.ApiException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/img/version")
public class ImageCapabilitiesController {

    @Resource
    ImageCapabilitiesService imageCapabilitiesService;


    @PostMapping("addMarkLine")
    public BaseResponse addMark(HttpServletRequest request,
                                @RequestParam(value = "description",required = false) String description,
                                @RequestParam(value = "id",required = false) Long imageMappingId ,
                                @RequestBody List<Long> imageIdList){
        //验证登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = currentUser.getId();

        imageCapabilitiesService.addMark(userId,description,imageIdList,imageMappingId);
        return ResultUtils.success(null);
    }

    @PostMapping("delMarkLine")
    public BaseResponse delMark(HttpServletRequest request,@RequestParam Long id){
        //验证登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        imageCapabilitiesService.delMark(id);
        return ResultUtils.success(null);
    }

    @GetMapping("getMarks")
    public BaseResponse<List<MarkLineOutput>> getMark(HttpServletRequest request, Long imageId){
        //验证登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        List<MarkLineOutput> res;
        if(imageId!=null){
            //获取指定image的所有兼容版本
            res=imageCapabilitiesService.getMark(imageId);
        }else{
            //获取最近的几个标记
            res=imageCapabilitiesService.getMarks();
        }
        return ResultUtils.success(res);
    }

    @GetMapping("incompatible")
    public BaseResponse<List<ImageOutput>> getIncompatibleVersion(HttpServletRequest request, Long imageId) throws ApiException {
        //验证登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        List<ImageOutput> res = imageCapabilitiesService.getIncompatibleVersion(imageId);

        return ResultUtils.success(res);
    }

}
