package com.iscas.autoCanary.controller;

import ch.qos.logback.core.util.FileUtil;
import cn.hutool.core.lang.UUID;
import com.iscas.autoCanary.common.BaseResponse;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.common.ResultUtils;
import com.iscas.autoCanary.contant.UserConstant;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.model.domain.User;
import com.iscas.autoCanary.model.domain.request.UserRegisterRequest;
import com.iscas.autoCanary.service.CCEService;
import com.iscas.autoCanary.service.UserService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.Yaml;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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

    @Autowired
    private UserService userService;

    @PostMapping("/stable/cutFlow")
    public BaseResponse<String> cutStableFlow(HttpServletRequest request) throws ApiException {
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
    public BaseResponse<String> resumeStableFlow(HttpServletRequest request) throws ApiException {
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
    public BaseResponse<String> cutCanaryFlow(HttpServletRequest request) throws ApiException {
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
    public BaseResponse<String> resumeCanaryFlow(HttpServletRequest request) throws ApiException {
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
    public BaseResponse<String> getIngressStatus(HttpServletRequest request) throws ApiException {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String ingressStatus = cceService.getIngressStatus();
        return ResultUtils.success(ingressStatus);
    }

    //    选择华为云镜像仓库的镜像，创建工作负载
    @SneakyThrows
    @PostMapping("deployment/create")
    public BaseResponse<String> createDeployment(HttpServletRequest request, String deploymentName, @RequestParam("file")MultipartFile multipartFile) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

//        判断deployment是否已经存在
        List<String> deploymentList = cceService.getDeploymentList();
        if (deploymentList.contains(deploymentName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deployment已存在，请不要重复添加");
        }

        String uuid = UUID.randomUUID().toString();
        String rootPath = System.getProperty("user.dir");
        // 创建临时文件
        File file = new File(rootPath+"\\src\\main\\resources\\static"+"\\"+uuid+"_"+multipartFile.getOriginalFilename());
        try {
            multipartFile.transferTo(file);
            cceService.createDeployment(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "保存文件失败");
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建deployment失败");
        }finally {
            new Thread(() ->{
                try {
                    Thread.sleep(2*3600);
                } catch (InterruptedException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "线程休眠失败");
                }
                if (file.delete()) {
                    System.out.println("文件删除成功");
                } else {
                    System.out.println("文件删除失败");
                }
            });
        }
        return ResultUtils.success("镜像创建成功");
    }


    //    获取工作负载的状态
    @GetMapping("deployment/status")
    public BaseResponse<String> getDeploymentStatus(HttpServletRequest request, String deploymentName) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        String result;
        try {
            String deploymentstatus = cceService.getDeployment(deploymentName);
            result = deploymentstatus;
            System.out.println(deploymentstatus);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

        return ResultUtils.success(result);
    }

    //    删除工作负载
    @PostMapping("deployment/delete")
    public BaseResponse<String> deleteDeployment(HttpServletRequest request, String deploymentName) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        try {
            cceService.getDeployment(deploymentName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deployment不存在，或者类型错误");
        }

        try {
            cceService.deleteDeployment(deploymentName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除工作负载失败");
        }

        return ResultUtils.success("deployment成功删除");
    }

    //    替换deployment镜像列表  更新
//    image参数可以是镜像的地址 也可以是华为云仓库相同地区的镜像名称加上版本号
    @PostMapping("deployment/update")
    public BaseResponse<String> replaceDeployment(HttpServletRequest request, String deploymentName, String imageURL) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        try {
            cceService.updateDeployment(deploymentName, imageURL);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "替换镜像失败");
        }
        return ResultUtils.success("替换镜像成功");
    }

    //    statefulset 查询
    @GetMapping("statefulset/status")
    public BaseResponse<String> getStatefulsetStatus(HttpServletRequest request, String statefulsetName) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String status = null;
        try {
            status = cceService.getStatefulSet(statefulsetName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询statefulset状态失败");
        }
        return ResultUtils.success(status);
    }

    //    statefulset 删除
    @PostMapping("statefulset/delete")
    public BaseResponse<String> deleteStatefulset(HttpServletRequest request, String statefulsetName) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        try {
            cceService.getStatefulSet(statefulsetName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "statefulset不存在");
        }
        try {
            cceService.deleteStatefulSet(statefulsetName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除statefulset失败");
        }
        return ResultUtils.success("删除statefulset成功");
    }

    //    statefulset 切换镜像  更新
    @PostMapping("statefulset/update")
    public BaseResponse<String> updateStatefulset(HttpServletRequest request, String statefulsetName, String imageURL) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        try {
            cceService.updateStatefulSet(statefulsetName, imageURL);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新statefulset失败");
        }
        return ResultUtils.success("更新statefulset成功");
    }

//        statefulset 创建
    @PostMapping("statefulset/create")
    public BaseResponse<String> createStatefulset(HttpServletRequest request, String statefulsetName, @RequestParam("file")MultipartFile multipartFile) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        String uuid = UUID.randomUUID().toString();
        String rootPath = System.getProperty("user.dir");
        // 保存文件到本地
        File file = new File(rootPath+"\\src\\main\\resources\\static"+"\\"+uuid+"_"+multipartFile.getOriginalFilename());
        try {
            multipartFile.transferTo(file);
            cceService.createStatefulSet(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "保存文件失败");
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建statefulset失败");
        } finally {
            boolean delete = file.delete();
            if (!delete){
                System.out.println("文件删除失败");
            }
        }
        return ResultUtils.success("镜像创建成功");
    }
}
