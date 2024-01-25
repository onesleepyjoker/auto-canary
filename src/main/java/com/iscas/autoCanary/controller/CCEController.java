package com.iscas.autoCanary.controller;

import com.iscas.autoCanary.common.BaseResponse;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.common.ResultUtils;
import com.iscas.autoCanary.contant.UserConstant;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.model.domain.User;
import com.iscas.autoCanary.model.domain.request.UserRegisterRequest;
import com.iscas.autoCanary.service.CCEService;
import io.kubernetes.client.openapi.ApiException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author windpo
 * @description 实现对集群自动化操作的逻辑
 */
@RestController
@RequestMapping("/cce")
public class CCEController {

    @Autowired
    public CCEService cceService;

    @PostMapping("/stable/cutFlow")
    public BaseResponse cutStableFlow(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // todo 修改configMap，切断稳定版本流量
        cceService.cutStableFlow();
        return ResultUtils.success(null);
    }

    @PostMapping("/stable/resumeFlow")
    public BaseResponse resumeStableFlow(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // todo 修改configMap，恢复稳定版本流量
        cceService.resumeStableFlow();
        return ResultUtils.success(null);
    }

    @PostMapping("/canary/cutFlow")
    public BaseResponse cutCanaryFlow(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // todo 修改configMap，切断灰度版本流量
        cceService.cutCanaryFlow();
        return ResultUtils.success(null);
    }


    @PostMapping("/canary/resumeFlow")
    public BaseResponse resumeCanaryFlow(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        // todo 修改configMap，恢复灰度版本流量
        cceService.resumeCanaryFlow();
        return ResultUtils.success(null);
    }

    @GetMapping("ingress/getStatus")
    public BaseResponse getIngressStatus(HttpServletRequest request) throws ApiException {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        cceService.getIngressStatus();
        return ResultUtils.success(null);
    }
}
