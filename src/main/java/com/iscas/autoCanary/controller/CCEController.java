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
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author windpo
 * @description 实现对集群自动化操作的逻辑
 */
@RestController
@RequestMapping("/cce")
public class CCEController {
    //    稳定最新版测试
    @Autowired
    private CCEService cceService;

    @PostMapping("/stable/cutFlow")
    public BaseResponse cutStableFlow(HttpServletRequest request) throws ApiException {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        cceService.cutStableFlow();
        System.out.println("稳定版本测试");
        cceService.getIngressStatus();
        return ResultUtils.success("稳定版测试阶段部署完成");
    }
//    稳定最新版发布

    @PostMapping("/stable/resumeFlow")
    public BaseResponse resumeStableFlow(HttpServletRequest request) throws ApiException {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        cceService.resumeStableFlow();
        System.out.println("稳定版本发布");
        cceService.getIngressStatus();
        return ResultUtils.success("稳定版发布完成");
    }

    //    灰度版本测试
    @PostMapping("/canary/cutFlow")
    public BaseResponse cutCanaryFlow(HttpServletRequest request) throws ApiException {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        cceService.cutCanaryFlow();
        System.out.println("灰度版本测试");
        cceService.getIngressStatus();
        return ResultUtils.success("灰度版本测试阶段部署完成");
    }

    //  灰度版本发布

    @PostMapping("/canary/resumeFlow")
    public BaseResponse resumeCanaryFlow(HttpServletRequest request) throws ApiException {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        cceService.resumeCanaryFlow();
        System.out.println("灰度版本发布");
        cceService.getIngressStatus();
        return ResultUtils.success("灰度版本发布完成");
    }

    @PostMapping("ingress/getStatus")
    public BaseResponse getIngressStatus(HttpServletRequest request) throws ApiException {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String ingressStatus = cceService.getIngressStatus();
        return ResultUtils.success(ingressStatus);
    }

//    选择华为云镜像仓库的镜像，创建工作负载
    @PostMapping("deployment/create")
    public BaseResponse createDeployment(HttpServletRequest request, @RequestParam("file")MultipartFile file) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

//        创建一个临时的文件用来封装yaml配置文件
        File tmpFile = new File("/tmp/deployment.yaml");
        try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
            // 将 MultipartFile 中的数据写入到临时文件
            InputStream is = file.getInputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件失败");
        }
        try {
            cceService.createDeployment(tmpFile);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建Deployment失败");
        }finally {
            // 删除临时文件
            tmpFile.delete();
        }
        return ResultUtils.success("镜像创建成功");
    }


}
