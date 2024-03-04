package com.iscas.autoCanary.controller;

import cn.hutool.core.lang.UUID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iscas.autoCanary.common.BaseResponse;
import com.iscas.autoCanary.common.DeleteRequest;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.common.ResultUtils;
import com.iscas.autoCanary.contant.UserConstant;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.model.domain.request.ImageCreateRequest;
import com.iscas.autoCanary.pojo.Image;
import com.iscas.autoCanary.pojo.Task;
import com.iscas.autoCanary.pojo.User;
import com.iscas.autoCanary.pojo.output.ImageOutput;
import com.iscas.autoCanary.service.CCEService;
import com.iscas.autoCanary.service.ImageService;
import com.iscas.autoCanary.service.TaskService;
import com.iscas.autoCanary.service.UserService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private ImageService imageService;

    @Autowired
    private TaskService taskService;

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
    @PostMapping("deployment/create")
    public BaseResponse<String> createDeployment(HttpServletRequest request, String deploymentName, @RequestParam("file") MultipartFile multipartFile) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

//        判断deployment是否已经存在
        List<String> deploymentList = null;
        try {
            deploymentList = cceService.getDeploymentList();
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取deployment列表失败");
        }
        if (deploymentList.contains(deploymentName)) {
            throw new BusinessException(ErrorCode.CREATE_ERROR, deploymentName + "已存在");
        }
        String yaml = "";
        try {
            // 将MultipartFile转换为字符串
            String fileContent = new String(multipartFile.getBytes(), "UTF-8");
            yaml = fileContent;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件解析失败");
        }
        try {
            cceService.createDeployment(yaml);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建deployment失败");
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建deployment失败");
        }


        return ResultUtils.success(deploymentName + "镜像创建成功");
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, deploymentName + "负载不存在");
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
//        先查看需要删除的负载是否存在
        try {
            cceService.getDeployment(deploymentName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deployment不存在，或者类型错误");
        }
//      删除
        try {
            cceService.deleteDeployment(deploymentName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除工作负载失败");
        }

        return ResultUtils.success(deploymentName + "deployment成功删除");
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
        //        先查看需要更新的负载是否存在
        try {
            cceService.getDeployment(deploymentName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "deployment不存在，或者类型错误");
        }
//        更新负载
        try {
            cceService.updateDeployment(deploymentName, imageURL);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "替换镜像失败");
        }
        return ResultUtils.success(deploymentName + "替换镜像成功");
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
            throw new BusinessException(ErrorCode.PARAMS_ERROR, statefulsetName + "不存在或者类型错误");
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
//        先查询statefulset是否存在
        try {
            cceService.getStatefulSet(statefulsetName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "statefulset不存在");
        }
//        删除负载
        try {
            cceService.deleteStatefulSet(statefulsetName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除statefulset失败");
        }
        return ResultUtils.success(statefulsetName + "删除statefulset成功");
    }

    //    statefulset 切换镜像  更新
    @PostMapping("statefulset/update")
    public BaseResponse<String> updateStatefulset(HttpServletRequest request, String statefulsetName, String imageURL) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
//      先查看更新的负载是否存在
        try {
            cceService.getStatefulSet(statefulsetName);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "statefulset不存在");
        }
//        更新镜像
        try {
            cceService.updateStatefulSet(statefulsetName, imageURL);
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新statefulset失败");
        }
        return ResultUtils.success(statefulsetName + "更新statefulset成功");
    }

    //        statefulset 创建
    @PostMapping("statefulset/create")
    public BaseResponse<String> createStatefulset(HttpServletRequest request, String statefulsetName, @RequestParam("file") MultipartFile multipartFile) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
//        先查看是否已经存在该名称的负载
        try {
            List<String> statefulSetList = cceService.getStatefulSetList();
            if (statefulSetList.contains(statefulsetName)) {
                throw new BusinessException(ErrorCode.CREATE_ERROR, statefulsetName + "已存在");
            }
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取statefulset列表失败");
        }
        String uuid = UUID.randomUUID().toString();
        String rootPath = System.getProperty("user.dir");
        // 保存文件到本地
        File file = new File(rootPath + "\\src\\main\\resources\\static" + "\\" + uuid + "_" + multipartFile.getOriginalFilename());
        try {
            multipartFile.transferTo(file);
            cceService.createStatefulSet(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "保存文件失败");
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建statefulset失败");
        } finally {
            new Thread(() -> {
                while (file.exists()) {
                    try {
                        Thread.sleep(2 * 1000 * 60);
                    } catch (InterruptedException e) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "线程休眠失败，无法删除临时文件");
                    }
                    if (file.delete()) {
                        System.out.println("临时文件删除成功");
                    } else {
                        System.out.println("临时文件删除失败");
                    }
                }
            }).start();
        }
        return ResultUtils.success(statefulsetName + "镜像创建成功");
    }

    //    展示当前已经部署的有负载镜像
    @GetMapping("/statefulset/list")
    public BaseResponse<List<String>> listStatefulSets(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        List<String> statefulSetList = null;
        try {
            statefulSetList = cceService.getStatefulSetList();
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取StatefulSet列表失败，请稍后重试");
        }
        return ResultUtils.success(statefulSetList);
    }

    //    展示当前已经部署的无负载镜像
    @GetMapping("/deployment/list")
    public BaseResponse<List<String>> listDeployment(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        List<String> deploymentList = null;
        try {
            deploymentList = cceService.getDeploymentList();
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取deployment列表失败，请稍后重试");
        }
        return ResultUtils.success(deploymentList);
    }

    //    返回当前正在运行的所有镜像列表
    @GetMapping("/image/list")
    public BaseResponse<List<ImageOutput>> listImage(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        List<ImageOutput> imageList = null;
        try {
            imageList = cceService.getNewImageList();
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取image列表失败，请稍后重试");
        }

        return ResultUtils.success(imageList);
    }

    //    最新版发布接口
    @PostMapping("/latest")
    public BaseResponse<Long> deployLatest(HttpServletRequest request,
                                           @RequestBody List<Map<String,String>> mapList) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long id = cceService.latestDeploy(request, mapList);
        return ResultUtils.success(id);
    }


    //    稳定版发布接口
    @PostMapping("/stable")
    public BaseResponse<Long> deployStable(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long id = cceService.stableDeploy(request);
        return ResultUtils.success(id);
    }

//    回滚接口   （指定集群回滚到指定的tsk任务版本）
    @PostMapping("/rollback")
    public BaseResponse<Long> rollback(HttpServletRequest request, String tag, Long id) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

//        1.查看对应的task是否存在
        Task historyTask = taskService.getById(id);
        Task task = new Task();
        task.setUserId(userService.getLoginUser(request).getId());
        if (historyTask == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "task不存在");
        }

        //        2.取出task列表里面的所有镜像，部署到对应的集群当中
        String imageList = historyTask.getImageList();
        Gson gson = new Gson();
        List<Long> imageIdList = gson.fromJson(imageList, new TypeToken<List<Long>>() {
        }.getType());
        if (tag != null && tag.equals("latest")) {
            task.setImageList(imageList);//把当时任务的镜像版本塞进去
            task.setDescription("将灰度版本回滚历史版本" + id);
            task.setLogInformation("1.将灰度版本回滚历史版本" + id);
            cceService.latestRollback(task, imageIdList);
            return ResultUtils.success(task.getId());
        }

        if (tag != null && tag.equals("stable")) {
            task.setImageList(imageList);//把当时任务的镜像版本塞进去
            task.setDescription("将稳定版本回滚历史版本" + id);
            task.setLogInformation("1.将稳定版本回滚历史版本" + id);
            cceService.stableRollback(task, imageIdList);
            return ResultUtils.success(task.getId());
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    @PostMapping("/delete/task")
    public BaseResponse<Long> createTask(HttpServletRequest request, @RequestBody DeleteRequest deleteRequest) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        long id = deleteRequest.getId();
        // 删除任务
        boolean b = taskService.removeById(id);
        if (!b){
            throw new BusinessException(ErrorCode.PARAMS_ERROR ,"任务已经删除，或者不存在");
        }
        return ResultUtils.success(id);
    }

}
