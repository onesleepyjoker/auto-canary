package com.iscas.autoCanary.controller;

import com.huaweicloud.sdk.swr.v2.model.ShowReposResp;
import com.iscas.autoCanary.common.BaseResponse;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.common.ResultUtils;
import com.iscas.autoCanary.contant.UserConstant;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.pojo.User;
import com.iscas.autoCanary.service.ImageRepoService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/imgRepo")
public class ImageRepoController {

    @Resource
    ImageRepoService imageRepoService;

    @GetMapping("/getCCEImgList")
    public BaseResponse<List<ShowReposResp>> getCCEImgList(HttpServletRequest request, String namespace) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return imageRepoService.getCCEImgList(namespace);
    }

    //    解析列表返回前端
    @PostMapping("/getCCEImgList2")
    public BaseResponse<List<String>> getCCEImgList2(HttpServletRequest request,String namespace){
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        BaseResponse<List<ShowReposResp>> cceImgList = imageRepoService.getCCEImgList(namespace);
        List<ShowReposResp> data = cceImgList.getData();
        List<String> list = data.stream().map(showReposResp -> {
            StringBuilder stringBuilder = new StringBuilder();
            for (String tag : showReposResp.getTags()) {
                String name = showReposResp.getName();
                stringBuilder.append(name).append(":").append(tag).append(";").append(" ");
            }
            return stringBuilder.toString();
        }).collect(Collectors.toList());
        return ResultUtils.success(list);
    }
}
