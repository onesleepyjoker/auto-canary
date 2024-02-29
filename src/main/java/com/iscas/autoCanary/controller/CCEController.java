package com.iscas.autoCanary.controller;

import cn.hutool.core.lang.UUID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iscas.autoCanary.common.BaseResponse;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.common.ResultUtils;
import com.iscas.autoCanary.contant.UserConstant;
import com.iscas.autoCanary.exception.BusinessException;
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
//        0.创建task对象
        Task task = new Task();
        task.setDescription("实现灰度发布");
        task.setUserId(userService.getLoginUser(request).getId());
        ArrayList<String> imageList = new ArrayList<>();
        for (Map<String, String> map : mapList) {
            imageList.add(map.get("image_id"));
        }
        task.setImageList(imageList.toString());

//        1.切断灰度版本的流量  变成tester
        try {
            cceService.cutCanaryFlow();
        } catch (ApiException e) {
            task.setLogInformation("切断灰度版本的流量失败，灰度发布失败");
            task.setIsSuccess(1);//1代表任务失败
            taskService.save(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切断灰度版本的流量失败，请稍后重试");
        }
        task.setLogInformation("1.切断灰度版本的流量成功，开始部署服务     ");

        //  2.根据服务id和镜像名称替换镜像版本
        for (Map<String, String> map : mapList) {
            String serviceName = map.get("service_name");
            String imageId = map.get("image_id");
            Image image = imageService.getById(imageId);
            String imageUrl = image.getImageUrl();
            try {
                cceService.updateDeployment(serviceName, imageUrl);
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"  "+"更新deployment失败，灰度发布失败");
                task.setIsSuccess(1);//1代表任务失败
                taskService.save(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新deployment失败，请检查服务名称是否正确稍后重试");
            }
        }


//        3.镜像部署完成进行点火测试
        String s = task.getLogInformation();
        task.setLogInformation(s+"     "+"2.镜像部署完成，开始进行点火测试");
        Boolean flag= null;
        while (flag==null){
            try {
                flag=cceService.fireTest();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (flag==null){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "轮询失败");
                }
            } else if (flag==false) {
                task.setLogInformation("点火测试失败，请稍后重试");
                task.setIsSuccess(1);
                break;
            }else {
                String information = task.getLogInformation();
                task.setLogInformation(information+"  "+"点火测试通过");
            }
        }

        //  4.灰度版本测试通过，进行流量切换，支持灰度版本的内测用户
        if (flag != null && flag) {
            String information = task.getLogInformation();
            task.setLogInformation(information+"     "+"3.灰度版本测试通过，开始进行灰度发布");
            try {
                cceService.resumeCanaryFlow();
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"  "+"流量更新失败，灰度发布失败");
                task.setIsSuccess(1);
                taskService.save(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流量更新失败，灰度发布失败");
            }
        }
        String logInformation = task.getLogInformation();
        task.setLogInformation(logInformation+"  "+"4.流量切换成功,成功发布！！！");
        task.setIsSuccess(0);
        taskService.save(task);

        return ResultUtils.success(task.getId());
    }


    //    稳定版发布接口
    @PostMapping("/stable")
    public BaseResponse<Long> deployStable(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
//        0.创建task对象
        Task task = new Task();
        task.setDescription("实现稳定版发布");
        task.setUserId(userService.getLoginUser(request).getId());

        ArrayList<Long> imageList = new ArrayList<>();
        Map<Long, String> newMap = new HashMap<>();//封装所有灰度版本的镜像map（id和imageUrl）

//        1.获取到所有的镜像id列表存储到task任务当中
        try {
            Map<Long,String> map = cceService.NewImageList();
            newMap=map;
            for (Long id : map.keySet()) {
                imageList.add(id);
            }
        } catch (ApiException e) {
            task.setLogInformation("无法获取到灰度版本镜像列表，稳定版发布失败");
            task.setIsSuccess(1);
            taskService.save(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取镜像列表失败，稳定版本发布失败");
        }catch (BusinessException e) {
            task.setLogInformation("无法获取到灰度版本镜像列表，稳定版发布失败");
            task.setIsSuccess(1);
            taskService.save(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取新版镜像列表失败，稳定版本发布失败");
        }
        task.setImageList(imageList.toString());
        task.setLogInformation("1.成功获取到所有新版镜像列表");


//        2.获取到所有的稳定版负载的镜像id和负载名称
        Map<Long, String> oldMap = new HashMap<>();
        try {
            Map<Long, String> map = cceService.oldImageListAndDeploymentName();
            oldMap = map;
        } catch (ApiException e) {
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"     "+"无法获取到稳定版负载的镜像id和负载名称，稳定版发布失败");
            task.setIsSuccess(1);
            taskService.save(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取稳定版镜像列表失败，稳定版本发布失败");
        }
        String logInformation3 = task.getLogInformation();
        task.setLogInformation(logInformation3+"     "+"2.成功获取到稳定版负载的镜像id和负载名称");

//        3.根据id对两个map进行匹配创建一个新的mapList来实现镜像的替换部署

        Map<String, String> mapList = new HashMap<>();
        for (Long oldImageId : oldMap.keySet()) {
            for (Long newImageId : newMap.keySet()) {
                if (oldImageId.equals(newImageId)){
                    mapList.put(oldMap.get(oldImageId), newMap.get(newImageId));//负载名称和镜像url
                }
            }
        }
//        4.切断灰度版本的流量  变成tester
        try {
            cceService.cutStableFlow();
        } catch (ApiException e) {
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"     "+"切断稳定版本的流量失败，灰度发布失败");
            task.setIsSuccess(1);//1代表任务失败
            taskService.save(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切断稳定版本的流量失败，请稍后重试");
        }
        String logInformation2 = task.getLogInformation();
        task.setLogInformation(logInformation2+"     "+"3.切断稳定版本的流量成功");

        //  5.根据服务名称和镜像URL替换镜像版本
        for (Map.Entry<String, String> entry : mapList.entrySet()) {
            String serviceName = entry.getKey();
            String imageUrl = entry.getValue();
            try {
                cceService.updateDeployment(serviceName, imageUrl);
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"  "+"更新deployment失败，稳定版发布失败");
                task.setIsSuccess(1);//1代表任务失败
                taskService.save(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新deployment失败，请检查服务名称是否正确稍后重试");
            }
        }

//        6.镜像部署完成进行点火测试
        String s = task.getLogInformation();
        task.setLogInformation(s+"     "+"4.镜像部署完成，开始进行点火测试");
        Boolean flag= null;
        while (flag==null){
            try {
                flag=cceService.fireTest();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (flag==null){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "轮询失败");
                }
            } else if (flag==false) {
                task.setLogInformation("点火测试失败，请稍后重试");
                task.setIsSuccess(1);
                break;
            }else {
                String information = task.getLogInformation();
                task.setLogInformation(information+"  "+"点火测试通过");
            }
        }

        //  7.灰度版本测试通过，进行流量切换，支持灰度版本的内测用户
        if (flag != null && flag) {
            String information = task.getLogInformation();
            task.setLogInformation(information+"     "+"5.稳定版本测试通过，开始进行稳定版发布");
            try {
                cceService.resumeCanaryFlow();
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"  "+"流量更新失败，稳定版发布失败");
                task.setIsSuccess(1);
                taskService.save(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流量更新失败，稳定版本发布失败");
            }
        }
        String logInformation = task.getLogInformation();
        task.setLogInformation(logInformation+"  "+"6.流量切换成功,成功发布！！！");
        task.setIsSuccess(0);
        taskService.save(task);

        return ResultUtils.success(task.getId());
    }

//    todo 还没有进行测试
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

        if (historyTask==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"task不存在");
        }

//        2.取出task列表里面的所有镜像，部署到对应的集群当中
        String imageList = historyTask.getImageList();
        Gson gson = new Gson();
        List<Long> imageIdList= gson.fromJson(imageList, new TypeToken<List<Long>>() {}.getType());
        if(tag!=null||tag.equals("latest")){
            task.setImageList(imageList);//把当时任务的镜像版本塞进去
            task.setDescription("将灰度版本回滚历史版本"+id);

//            如果是new的话表示需要获取到所有tag为new的负载名称和对应的镜像id，然后根据镜像id部署指定版本的将镜像
            Map<Long, String> newList = new HashMap<>();
            try {
                Map<Long, String> map = cceService.newImageListAndDeploymentName();
            } catch (ApiException e) {
                task.setLogInformation("无法读取当前灰度版本的镜像信息");
                task.setIsSuccess(1);
                taskService.save(task);
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"无法读取当前灰度版本的镜像信息");
            }

//          3.根据获取到的负载信息匹配对应的指定版本镜像
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<Long, String> entry : newList.entrySet()) {
                String deploymentName = entry.getValue();
                Long newImageId = entry.getKey();
                for (Long imageId : imageIdList) {
                    if (imageId.equals(newImageId)){
                        map.put(deploymentName,imageId.toString());
                    }
                }
            }
            //        4.切断灰度版本的流量  变成tester
            try {
                cceService.cutCanaryFlow();
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"     "+"切断灰度版本的流量失败，回滚失败");
                task.setIsSuccess(1);//1代表任务失败
                taskService.save(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切断灰度版本的流量失败，请稍后重试");
            }
            String logInformation2 = task.getLogInformation();
            task.setLogInformation(logInformation2+"     "+"3.切断灰度版本的流量成功");

            //  5.根据服务名称和镜像URL替换镜像版本
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String serviceName = entry.getKey();
                String imageUrl = entry.getValue();
                try {
                    cceService.updateDeployment(serviceName, imageUrl);
                } catch (ApiException e) {
                    String logInformation = task.getLogInformation();
                    task.setLogInformation(logInformation+"  "+"更新deployment失败，灰度版发布失败");
                    task.setIsSuccess(1);//1代表任务失败
                    taskService.save(task);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新deployment失败，请检查服务名称是否正确稍后重试");
                }
            }

//        6.镜像部署完成进行点火测试
            String s = task.getLogInformation();
            task.setLogInformation(s+"     "+"4.镜像部署完成，开始进行点火测试");
            Boolean flag= null;
            while (flag==null){
                try {
                    flag=cceService.fireTest();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (flag==null){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "轮询失败");
                    }
                } else if (flag==false) {
                    task.setLogInformation("点火测试失败，请稍后重试");
                    task.setIsSuccess(1);
                    break;
                }else {
                    String information = task.getLogInformation();
                    task.setLogInformation(information+"  "+"点火测试通过");
                }
            }

            //  7.灰度版本测试通过，进行流量切换，支持灰度版本的内测用户
            if (flag != null && flag) {
                String information = task.getLogInformation();
                task.setLogInformation(information+"     "+"5.灰度版本测试通过，开始回滚");
                try {
                    cceService.resumeCanaryFlow();
                } catch (ApiException e) {
                    String logInformation = task.getLogInformation();
                    task.setLogInformation(logInformation+"  "+"流量更新失败，回滚失败");
                    task.setIsSuccess(1);
                    taskService.save(task);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流量更新失败，回滚失败");
                }
            }
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"  "+"6.流量切换成功,回滚！！！");
            task.setIsSuccess(0);
            taskService.save(task);
        }

//        实现稳定版本的回滚
        if(tag!=null||tag.equals("stable")){
            task.setImageList(imageList);//把当时任务的镜像版本塞进去
            task.setDescription("将稳定版本回滚历史版本"+id);

//            如果是new的话表示需要获取到所有tag为new的负载名称和对应的镜像id，然后根据镜像id部署指定版本的将镜像
            Map<Long, String> newList = new HashMap<>();
            try {
                Map<Long, String> map = cceService.oldImageListAndDeploymentName();
            } catch (ApiException e) {
                task.setLogInformation("无法读取当前灰度版本的镜像信息");
                task.setIsSuccess(1);
                taskService.save(task);
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"无法读取当前稳定版本的镜像信息");
            }

//          3.根据获取到的负载信息匹配对应的指定版本镜像
            Map<String, String> map = new HashMap<>();
            for (Map.Entry<Long, String> entry : newList.entrySet()) {
                String deploymentName = entry.getValue();
                Long newImageId = entry.getKey();
                for (Long imageId : imageIdList) {
                    if (imageId.equals(newImageId)){
                        map.put(deploymentName,imageId.toString());
                    }
                }
            }
            //        4.切断灰度版本的流量  变成tester
            try {
                cceService.cutCanaryFlow();
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"     "+"切断稳定版本的流量失败，回滚失败");
                task.setIsSuccess(1);//1代表任务失败
                taskService.save(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切断稳定版本的流量失败，请稍后重试");
            }
            String logInformation2 = task.getLogInformation();
            task.setLogInformation(logInformation2+"     "+"3.切断稳定版本的流量成功");

            //  5.根据服务名称和镜像URL替换镜像版本
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String serviceName = entry.getKey();
                String imageUrl = entry.getValue();
                try {
                    cceService.updateDeployment(serviceName, imageUrl);
                } catch (ApiException e) {
                    String logInformation = task.getLogInformation();
                    task.setLogInformation(logInformation+"  "+"更新deployment失败，稳定版发布失败");
                    task.setIsSuccess(1);//1代表任务失败
                    taskService.save(task);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新deployment失败，请检查服务名称是否正确稍后重试");
                }
            }

//        6.镜像部署完成进行点火测试
            String s = task.getLogInformation();
            task.setLogInformation(s+"     "+"4.镜像部署完成，开始进行点火测试");
            Boolean flag= null;
            while (flag==null){
                try {
                    flag=cceService.fireTest();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (flag==null){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "轮询失败");
                    }
                } else if (flag==false) {
                    task.setLogInformation("点火测试失败，请稍后重试");
                    task.setIsSuccess(1);
                    break;
                }else {
                    String information = task.getLogInformation();
                    task.setLogInformation(information+"  "+"点火测试通过");
                }
            }

            //  7.灰度版本测试通过，进行流量切换，支持灰度版本的内测用户
            if (flag != null && flag) {
                String information = task.getLogInformation();
                task.setLogInformation(information+"     "+"5.稳定版本测试通过，开始进行回滚");
                try {
                    cceService.resumeCanaryFlow();
                } catch (ApiException e) {
                    String logInformation = task.getLogInformation();
                    task.setLogInformation(logInformation+"  "+"流量更新失败，稳定版发布失败");
                    task.setIsSuccess(1);
                    taskService.save(task);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流量更新失败，回滚失败");
                }
            }
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"  "+"6.流量切换成功,回滚！！！");
            task.setIsSuccess(0);
            taskService.save(task);
        }
        return ResultUtils.success(task.getId());
    }

}
