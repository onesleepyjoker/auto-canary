package com.iscas.autoCanary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.SerializableString;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.mapper.TaskMapper;
import com.iscas.autoCanary.pojo.Image;
import com.iscas.autoCanary.pojo.Task;
import com.iscas.autoCanary.pojo.output.ImageOutput;
import com.iscas.autoCanary.service.CCEService;
import com.iscas.autoCanary.service.ImageService;
import com.iscas.autoCanary.service.TaskService;
import com.iscas.autoCanary.service.UserService;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author windpo
 * @description CCE相关接口实现类
 */
@Service
@Slf4j
public class CCEServiceImpl implements CCEService {
    //ingress名称常量
    final String NAMESPACE = "default";
    final String STABLE_INGRESS_NAME = "project";
    final String CANARY_INGRESS_NAME = "new-project";
    //灰度发布相关常量
    final String HEADER = "canary";
    final String CANARY_HEADER_TEST_PATTERN = "^tester$";
    final String CANARY_HEADER_NORMAL_PATTERN = "^(new|tester)$";
    final String STABLE_HEADER_PATTERN = "^tester$";
    //记录发布任务的执行状态
    public final Map<Long,Boolean> executionStatus = new ConcurrentHashMap<>();

    @Value("${secret.name}")
    private String SECRET_NAME;

    @Autowired
    private ImageService imageService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;


    /**
     * 获取ingress的注解
     *
     * @param namespace
     * @param ingressName
     * @return nonNullable
     */
    protected Map<String, String> getAnnotations(String namespace, String ingressName) throws ApiException {
        // 创建 Networking API 实例
        NetworkingV1Api networkingV1Api = new NetworkingV1Api();
        V1Ingress stableIngress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null);
        // 读取 Annotations
        V1ObjectMeta stableMetadata = Optional.ofNullable(stableIngress)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_INGRESS))
                .getMetadata();
        Map<String, String> stableAnnotations = Optional.ofNullable(stableMetadata)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR))
                .getAnnotations();
        //返回annotations，annotations==null, throw Exception
        return Optional.ofNullable(stableAnnotations)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR));
    }

    /**
     * 更新ingress的注解
     *
     * @param namespace
     * @param ingressName
     * @param annotations
     * @throws ApiException
     */
    protected void updateAnnotations(String namespace, String ingressName, Map<String, String> annotations) throws ApiException {
        // 创建 Networking API 实例
        NetworkingV1Api networkingV1Api = new NetworkingV1Api();
        V1Ingress ingress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null);
        V1ObjectMeta metadata = Optional.ofNullable(ingress)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_INGRESS))
                .getMetadata();
        Optional.ofNullable(metadata)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR))
                .setAnnotations(annotations);
        networkingV1Api.replaceNamespacedIngress(ingressName, namespace, ingress, null, null, null, null);
    }

    /**
     * 用于稳定版开始测试按钮   --
     */
    @Override
    public void cutStableFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //切断内侧用户流量
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary", "false");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary", "true");
        // 保证状态为目标状态
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header", HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header", HEADER);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern", CANARY_HEADER_NORMAL_PATTERN);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern", STABLE_HEADER_PATTERN);

        // 更新注解
        updateAnnotations(NAMESPACE, CANARY_INGRESS_NAME, canaryAnnotations);
        updateAnnotations(NAMESPACE, STABLE_INGRESS_NAME, stableAnnotations);
    }

    /**
     * 用于稳定版正式发布按钮
     */
    @Override
    public void resumeStableFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //切断内侧用户流量
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary", "false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary", "true");
        // 保证状态为目标状态
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header", HEADER);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header", HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value", STABLE_HEADER_PATTERN);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern", CANARY_HEADER_NORMAL_PATTERN);

        // 更新注解
        updateAnnotations(NAMESPACE, STABLE_INGRESS_NAME, stableAnnotations);
        updateAnnotations(NAMESPACE, CANARY_INGRESS_NAME, canaryAnnotations);
    }

    /**
     * 用于灰度版开始测试按钮
     *
     * @throws ApiException
     */
    @Override
    public void cutCanaryFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //切断内侧用户流量
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern", CANARY_HEADER_TEST_PATTERN);
        // 保证状态为目标状态
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary", "true");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary", "false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header", HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header", HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value", STABLE_HEADER_PATTERN);

        // 更新注解
        updateAnnotations(NAMESPACE, CANARY_INGRESS_NAME, canaryAnnotations);
        updateAnnotations(NAMESPACE, STABLE_INGRESS_NAME, stableAnnotations);
    }

    /**
     * 用于灰度版正式发布按钮
     */
    @Override
    public void resumeCanaryFlow() throws ApiException {
//        将镜像部署到灰度环境当中


//     流量切换操作
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //恢复内侧用户流量
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern", CANARY_HEADER_NORMAL_PATTERN);
        //保证状态为目标状态
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary", "true");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary", "false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header", HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header", HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value", STABLE_HEADER_PATTERN);

        // 更新注解
        updateAnnotations(NAMESPACE, CANARY_INGRESS_NAME, canaryAnnotations);
        updateAnnotations(NAMESPACE, STABLE_INGRESS_NAME, stableAnnotations);

//        返回成功部署的镜像id
    }

    @Override
    public String getIngressStatus() throws ApiException {
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        List<String> stableValues = stableAnnotations.values().stream().collect(Collectors.toList());
        List<String> canaryValues = canaryAnnotations.values().stream().collect(Collectors.toList());
        if (stableValues.get(0).equals("false") && canaryValues.get(0).equals("true")) {
            if (canaryValues.get(2).equals("^(new|tester)$")) {
                return "正常运行阶段";
            } else if (canaryValues.get(2).equals("^tester$")) {
                return "灰度版本内部测试阶段";
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请求头规则出错，请联系工作人员");
            }
        } else if (stableValues.get(0).equals("true") && canaryValues.get(0).equals("false")) {
            if (stableValues.get(2).equals("^tester$")) {
                return "稳定版本内部测试阶段";
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请求头规则出错，请联系工作人员");
            }
        }
        return "查询失败";
    }

    //  获取deployment的相关信息 （查）
    public String getDeployment(String deploymentName) throws ApiException {
        // 创建 apps API 实例
        AppsV1Api appsV1Api = new AppsV1Api();
        V1Deployment canaryDeployment = appsV1Api.readNamespacedDeployment(deploymentName, NAMESPACE, null);
        // 返回 deployment 信息
        return canaryDeployment.toString();
    }

    //    替换镜像deployment （改）
    public int updateDeployment(String deploymentName, String imagesURL) throws ApiException {
        // 创建 apps API 实例
        AppsV1Api appsV1Api = new AppsV1Api();

        // 获取到deployment
        V1Deployment stableDeployment = appsV1Api.readNamespacedDeployment(deploymentName, NAMESPACE, null);
//            更新镜像
        stableDeployment.getSpec().getTemplate().getSpec().
                setImagePullSecrets(Arrays.asList(new V1LocalObjectReference().name(SECRET_NAME)));
        //从华为云镜像仓库当中拉取镜像直接走默认的拉取镜像规则即可
        List<V1Container> containers = stableDeployment.getSpec().getTemplate().getSpec().getContainers();
        for (V1Container container : containers) {
            container.setImage(imagesURL);
            System.out.println("成功更新镜像");
        }

        // 更新 deployment
        stableDeployment.getSpec().getTemplate().getSpec().setContainers(containers);
        appsV1Api.replaceNamespacedDeployment(deploymentName, "default", stableDeployment, null, null, null, null);
        return 1;
    }

    //    根据yaml创建deployment  （增）
    public int createDeployment(String stringYaml) throws IOException, ApiException {
        // 创建 AppsV1Api 对象
        AppsV1Api appsV1Api = new AppsV1Api();

        // 从 YAML 文件中加载 Deployment 对象
        V1Deployment deployment = (V1Deployment) Yaml.load(stringYaml);
        V1Deployment createdDeployment = appsV1Api.createNamespacedDeployment(NAMESPACE, deployment, null, null, null, null);
        System.out.println("Deployment created successfully: " + createdDeployment.getMetadata().getName());
        return 1;
    }

    //    删除deployment  （删）
    public int deleteDeployment(String deploymentName) throws ApiException {
        // 创建 AppsV1Api 对象
        AppsV1Api appsV1Api = new AppsV1Api();

        // 删除 Deployment
        appsV1Api.deleteNamespacedDeployment(deploymentName, NAMESPACE, null, null, null, null, null, null);
        System.out.println("Deployment deleted successfully");
        return 1;
    }

    //    根据yaml创建statefulset  （增）
    public int createStatefulSet(File file) throws IOException, ApiException {
        // 创建 AppsV1Api 对象
        AppsV1Api appsV1Api = new AppsV1Api();
        // 从 YAML 文件中加载 Deployment 对象
        V1StatefulSet statefulSet = (V1StatefulSet) Yaml.load(file);
        appsV1Api.createNamespacedStatefulSet(NAMESPACE, statefulSet, null, null, null, null);
        System.out.println("创建成功");
        return 1;
    }


    //    替换镜像stateful   （改）
    public int updateStatefulSet(String statefulSetName, String imagesURL) throws ApiException {
//            获取stateful对象和镜像列表
        AppsV1Api appsV1Api = new AppsV1Api();
        V1StatefulSet stableStatefulSet = appsV1Api.readNamespacedStatefulSet(statefulSetName, NAMESPACE, null);
        V1Container v1Container = stableStatefulSet.getSpec().getTemplate().getSpec().getContainers().get(0);
//            将新的镜像和拉取镜像的secret策略set进去
        stableStatefulSet.getSpec().getTemplate().getSpec().setImagePullSecrets(Arrays.asList(new V1LocalObjectReference().name(SECRET_NAME)));
        v1Container.setImage(imagesURL);//填写第三方库镜像的网址  （或者默认填写官方的的镜像名称 nginx：latest）
//                        更新statefulset
        appsV1Api.replaceNamespacedStatefulSet(statefulSetName, NAMESPACE, stableStatefulSet, null, null, null, null);
        System.out.println("镜像更换成功");
        return 1;
    }

    //    删除statefulset
    public int deleteStatefulSet(String statefulSetName) throws ApiException {
        // 创建 AppsV1Api 对象
        AppsV1Api appsV1Api = new AppsV1Api();

        // 删除 Deployment
        appsV1Api.deleteNamespacedStatefulSet(statefulSetName, NAMESPACE, null, null, null, null, null, null);
        System.out.println("Deployment deleted successfully");
        return 1;
    }

    //      查询statefulSet信息  （查）
    public String getStatefulSet(String StatefulSetName) throws ApiException {
        // 创建 apps API 实例
        AppsV1Api appsV1Api = new AppsV1Api();
        V1StatefulSet canaryDeployment = appsV1Api.readNamespacedStatefulSet(StatefulSetName, NAMESPACE, null);
        //print
        System.out.println("canary deployment info:: " + canaryDeployment);
        return canaryDeployment.toString();//返回查询的信息
    }

    //    查询secret信息  （查）
    public String getSecret(String secretName) throws ApiException {
        CoreV1Api coreV1Api = new CoreV1Api();
        V1Secret v1Secret = coreV1Api.readNamespacedSecret(secretName, NAMESPACE, null);
        return v1Secret.toString();
    }

    //        创建secret  （增）
    public void createSecret(String secretName, String secretValue) throws ApiException {
        CoreV1Api coreV1Api = new CoreV1Api();
        V1Secret v1Secret = new V1Secret();
        v1Secret.putDataItem(secretName, secretValue.getBytes());
        coreV1Api.createNamespacedSecret(NAMESPACE, v1Secret, null, null, null, null);
        System.out.println("Secret created successfully");
    }

    //        查询deployment信息拿到列表 （查）
    public List<String> getDeploymentList() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, null,
                null, null, null, null,
                null, null);
        List<String> deploymentList = v1DeploymentList.getItems().stream()
                .map(v1Deployment -> v1Deployment.getMetadata().getName())
                .collect(Collectors.toList());
        return deploymentList;
    }

    //            查询statefulset信息拿到列表  （查）
    public List<String> getStatefulSetList() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
        V1StatefulSetList v1StatefulSetList = appsV1Api.listNamespacedStatefulSet(
                NAMESPACE, null, null, null,
                null, null, null, null,
                null, null, null, null);
        List<String> deploymentList = v1StatefulSetList.getItems().stream()
                .map(v1Deployment -> v1Deployment.getMetadata().getName())
                .collect(Collectors.toList());
        return deploymentList;
    }

    //    获取到所有正在运行的标签为new的灰度版本镜像列表
    @Override
    public List<ImageOutput> getImageList() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
//        获取到所有的有状态负载 （运行当中）
        V1StatefulSetList v1StatefulSetList = appsV1Api.listNamespacedStatefulSet(
                NAMESPACE, null, null, null,
                null, null, null, null,
                null, null, null, null);
//        获取到所有的无状态负载 （没有运行当中的负载不会出现在k8s当中）
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, "version=new",
                null, null, null, null,
                null, null);

        ArrayList<ImageOutput> list = new ArrayList<>();
        for (V1Deployment item : v1DeploymentList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {
                String image = container.getImage();//rabbitmq：3.12
                String[] split = image.split(":", 2);
                ImageOutput imageOutput = new ImageOutput();
                imageOutput.setImageName(split[0]);
                if(split.length>1){
                    imageOutput.setVersion(split[1]);
                }else{
                    throw new BusinessException(ErrorCode.DEPLOYMENT_CONFIG_ERROR);
                }
                list.add(imageOutput);
            }
        }
        for (V1StatefulSet item : v1StatefulSetList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {
                String image = container.getImage();//rabbitmq：3.12
                String[] split = image.split(":", 2);
                ImageOutput imageOutput = new ImageOutput();
                imageOutput.setImageName(split[0]);
                if(split.length>1){
                    imageOutput.setVersion(split[1]);
                }else{
                    throw new BusinessException(ErrorCode.DEPLOYMENT_CONFIG_ERROR);
                }
                list.add(imageOutput);
            }
        }
        return list;
    }

    //    获取到所有正在运行的标签为new的灰度版本镜像列表
    @Override
    public List<ImageOutput> getNewImageList() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
//        获取到所有的有状态负载 （运行当中）
        V1StatefulSetList v1StatefulSetList = appsV1Api.listNamespacedStatefulSet(
                NAMESPACE, null, null, null,
                null, "version=new", null, null,
                null, null, null, null);
//        获取到所有的无状态负载 （没有运行当中的负载不会出现在k8s当中）
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, "version=new",
                null, null, null, null,
                null, null);

        ArrayList<ImageOutput> list = new ArrayList<>();
        for (V1Deployment item : v1DeploymentList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {
                String image = container.getImage();//rabbitmq：3.12
                String[] split = image.split(":", 2);
                ImageOutput imageOutput = new ImageOutput();
                imageOutput.setImageName(split[0]);
                if(split.length>1){
                    imageOutput.setVersion(split[1]);
                }else{
                    throw new BusinessException(ErrorCode.DEPLOYMENT_CONFIG_ERROR);
                }
                list.add(imageOutput);
            }
        }
        for (V1StatefulSet item : v1StatefulSetList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {
                String image = container.getImage();//rabbitmq：3.12
                String[] split = image.split(":", 2);
                ImageOutput imageOutput = new ImageOutput();
                imageOutput.setImageName(split[0]);
                if(split.length>1){
                    imageOutput.setVersion(split[1]);
                }else{
                    throw new BusinessException(ErrorCode.DEPLOYMENT_CONFIG_ERROR);
                }
                list.add(imageOutput);
            }
        }
        return list;
    }

    @Override
    public List<ImageOutput> getOldImageList() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();


//        获取到所有的有状态负载 （运行当中）
        V1StatefulSetList v1StatefulSetList = appsV1Api.listNamespacedStatefulSet(
                NAMESPACE, null, null, null,
                null, "version=old", null, null,
                null, null, null, null);
//        获取到所有的无状态负载 （没有运行当中的负载不会出现在k8s当中）
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, "version=old",
                null, null, null, null,
                null, null);

        ArrayList<ImageOutput> list = new ArrayList<>();
        for (V1Deployment item : v1DeploymentList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {
                String image = container.getImage();//rabbitmq：3.12
                String[] split = image.split(":", 2);
                ImageOutput imageOutput = new ImageOutput();
                imageOutput.setImageName(split[0]);
                if(split.length>1){
                    imageOutput.setVersion(split[1]);
                }else{
                    throw new BusinessException(ErrorCode.DEPLOYMENT_CONFIG_ERROR);
                }
                list.add(imageOutput);
            }
        }
        for (V1StatefulSet item : v1StatefulSetList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {
                String image = container.getImage();//rabbitmq：3.12
                String[] split = image.split(":", 2);
                ImageOutput imageOutput = new ImageOutput();
                imageOutput.setImageName(split[0]);
                if(split.length>1){
                    imageOutput.setVersion(split[1]);
                }else{
                    throw new BusinessException(ErrorCode.DEPLOYMENT_CONFIG_ERROR);
                }
                list.add(imageOutput);
            }
        }
        return list;
    }

    //    对标签为version=new的所有服务进行镜像的版本替换
    @Override
    public int updateNewImageList(Map<String, String> imageNameAndUrl) throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();


//        获取到所有的有状态负载 （运行当中）
        V1StatefulSetList v1StatefulSetList = appsV1Api.listNamespacedStatefulSet(
                NAMESPACE, null, null, null,
                null, "version=new", null, null,
                null, null, null, null);
//        获取到所有的无状态负载 （没有运行当中的负载不会出现在k8s当中）
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, "version=new",
                null, null, null, null,
                null, null);

        for (V1Deployment item : v1DeploymentList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {//一个负载会有多个镜像容器
                String image = container.getImage();//rabbitmq：3.12
                String[] split = image.split(":", 2);
                if (imageNameAndUrl.containsKey(split[0])) {
                    container.setImage(imageNameAndUrl.get(split[0]));//根据名字填入新的容器地址
                    System.out.println(imageNameAndUrl.get(split[0]) + "镜像替换成功");
                }
            }
        }
        for (V1StatefulSet item : v1StatefulSetList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {
                String image = container.getImage();//rabbitmq：3.12
                String[] split = image.split(":", 2);
                String imageName = split[0];
                if (imageNameAndUrl.containsKey(imageName)) {
                    container.setImage(imageNameAndUrl.get(imageName));//根据名字填入新的容器地址
                    System.out.println(imageNameAndUrl.get(imageName) + "镜像替换成功");
                }
            }
        }
        return 1;
    }

    //    模拟点火测试的接口方法
    @Override
    public Boolean fireTest() throws InterruptedException {
        Thread.sleep(5000);
        return true;
    }

    //    返回所有标签为new的正在运行的镜像id和镜像url
    @Override
    public Map<Long, String> NewImageList() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
        Map<Long, String> res = new HashMap<>();
//        获取标签为new 的有状态负载 （运行当中）
//        V1StatefulSetList v1StatefulSetList = appsV1Api.listNamespacedStatefulSet(
//                NAMESPACE, null, null, null,
//                null, "version=new", null, null,
//                null, null, null, null);
//        获取到所有的无状态负载 （没有运行当中的负载不会出现在k8s当中）
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, "version=new",
                null, null, null, null,
                null, null);

        for (V1Deployment item : v1DeploymentList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {//一个负载会有多个镜像容器
                String imageUrl = container.getImage();//swr.cn-east-3.myhuaweicloud.com/isrc-test-develop-shanghai/rabbitmq:3.12-management
                QueryWrapper<Image> queryWrapper = new QueryWrapper<>();//根据镜像名称和版本查询镜像id
                queryWrapper.eq(imageUrl != null, "imageUrl", imageUrl);//直接根据url来查询id
                Image newImage = imageService.getOne(queryWrapper);
                if (newImage == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "查不到灰度版本的镜像信息");
                }
                Long id = newImage.getId();
                res.put(id, newImage.getImageUrl());
            }
        }
        return res;
    }

    //      返回标签为old的镜像id和镜像url
    @Override
    public Map<Long, String> oldImageList() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
        Map<Long, String> res = new HashMap<>();
//        获取标签为new 的有状态负载 （运行当中）
//        V1StatefulSetList v1StatefulSetList = appsV1Api.listNamespacedStatefulSet(
//                NAMESPACE, null, null, null,
//                null, "version=new", null, null,
//                null, null, null, null);
//        获取到所有的无状态负载 （没有运行当中的负载不会出现在k8s当中）
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, "version=old",
                null, null, null, null,
                null, null);

        for (V1Deployment item : v1DeploymentList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {//一个负载会有多个镜像容器
                String imageUrl = container.getImage();//swr.cn-east-3.myhuaweicloud.com/isrc-test-develop-shanghai/rabbitmq:3.12-management
                QueryWrapper<Image> queryWrapper = new QueryWrapper<>();//根据镜像名称和版本查询镜像id
                queryWrapper.eq(imageUrl != null, "imageUrl", imageUrl);//直接根据url来查询id
                Image newImage = imageService.getOne(queryWrapper);
                if (newImage == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "查不到灰度版本的镜像信息");
                }
                Long id = newImage.getId();
                res.put(id, newImage.getImageUrl());
            }
        }
        return res;
    }

    //    返回所有标签为old的正在运行的镜像id和服务名称
    @Override
    public Map<Long, String> oldImageListAndDeploymentName() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
        Map<Long, String> res = new HashMap<>();

//        获取到所有标签为old无状态负载 （没有运行当中的负载不会出现在k8s当中）
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, "version=old",
                null, null, null, null,
                null, null);

        for (V1Deployment item : v1DeploymentList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {//一个负载会有多个镜像容器
                String imageUrl = container.getImage();//。。。。。。rabbitmq：3.12
                QueryWrapper<Image> queryWrapper = new QueryWrapper<>();//根据镜像名称和版本查询镜像id
                queryWrapper.eq("imageUrl", imageUrl);
                Image newImage = imageService.getOne(queryWrapper);
                if (newImage == null) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据库中没有该镜像");
                }
                Long id = newImage.getId();
                res.put(id, item.getMetadata().getName());
            }
        }
        return res;
    }

    //    返回标签为new的无状态负载的镜像id和负载名称
    @Override
    public Map<Long, String> newImageListAndDeploymentName() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
        Map<Long, String> res = new HashMap<>();

//        获取到所有标签为old无状态负载 （没有运行当中的负载不会出现在k8s当中）
        V1DeploymentList v1DeploymentList = appsV1Api.listNamespacedDeployment(NAMESPACE, null,
                null, null, null, "version=new",
                null, null, null, null,
                null, null);

        for (V1Deployment item : v1DeploymentList.getItems()) {
            List<V1Container> containers = item.getSpec().getTemplate().getSpec().getContainers();
            for (V1Container container : containers) {//一个负载会有多个镜像容器
                String imageUrl = container.getImage();//。。。。。。rabbitmq：3.12
                QueryWrapper<Image> queryWrapper = new QueryWrapper<>();//根据镜像名称和版本查询镜像id
                queryWrapper.eq("imageUrl", imageUrl);
                Image newImage = imageService.getOne(queryWrapper);
                if (newImage == null) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据库中没有该镜像");
                }
                Long id = newImage.getId();
                res.put(id, item.getMetadata().getName());
            }
        }
        return res;
    }

    //    回滚灰度集群的负载镜像，将回滚信息记录到日志当中
    @Override
    public long latestRollback(Task task, List<Long> imageIdList) {

//            如果是new的话表示需要获取到所有tag为new的负载名称和对应的镜像id，然后根据镜像id部署指定版本的将镜像
        Map<Long, String> newList = new HashMap<>();
        try {
            Map<Long, String> map = this.newImageListAndDeploymentName();
            newList=map;
        } catch (ApiException e) {
            task.setLogInformation("无法读取当前灰度版本的镜像信息");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取当前灰度版本的镜像信息");
        }

//          3.根据获取到的负载信息匹配对应的指定版本镜像
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Long, String> entry : newList.entrySet()) {
            String deploymentName = entry.getValue();
            Long newImageId = entry.getKey();
            for (Long imageId : imageIdList) {
                if (imageId.equals(newImageId)) {
                    map.put(deploymentName, imageId.toString());
                }
            }
        }
        String logInformation1 = task.getLogInformation();
        task.setLogInformation(logInformation1 + "     " + "2.根据获取到的负载信息匹配对应的指定版本镜像");
        //        4.切断灰度版本的流量  变成tester
        try {
            this.cutCanaryFlow();
        } catch (ApiException e) {
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation + "     " + "切断灰度版本的流量失败，回滚失败");
            task.setIsSuccess(1);//1代表任务失败
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切断灰度版本的流量失败，请稍后重试");
        }
        String logInformation2 = task.getLogInformation();
        task.setLogInformation(logInformation2 + "     " + "3.切断灰度版本的流量成功");

        //  5.根据服务名称和镜像URL替换镜像版本
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String serviceName = entry.getKey();
            String imageUrl = entry.getValue();
            try {
                this.updateDeployment(serviceName, imageUrl);
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation + "  " + "更新deployment失败，灰度版发布失败");
                task.setIsSuccess(1);//1代表任务失败
                taskService.saveOrUpdate(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新deployment失败，请检查服务名称是否正确稍后重试");
            }
        }

//        6.镜像部署完成进行点火测试
        //设置任务执行状态
        taskService.saveOrUpdate(task);
        executionStatus.put(task.getId(),true);
        String s = task.getLogInformation();
        task.setLogInformation(s + "     " + "4.镜像部署完成，开始进行点火测试");
        Boolean flag = null;
        //executionStatus记录该任务状态
        while (executionStatus.get(task.getId())&&flag == null) {
            try {
                flag = this.fireTest();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (flag == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "轮询失败");
                }
            } else if (flag == false) {
                task.setLogInformation("点火测试失败，请稍后重试");
                task.setIsSuccess(1);
                throw new BusinessException(ErrorCode.TEST_ERR);
            } else {
                String information = task.getLogInformation();
                task.setLogInformation(information + "  " + "点火测试通过");
            }
        }
        //任务被手动停止
        if (!executionStatus.get(task.getId())) {
            executionStatus.remove(task.getId());
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"     "+"该任务被手动停止，请手动回滚或者操作集群后手动恢复流量");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException("该任务被手动停止，请手动回滚或者操作集群后手动恢复流量",5000,"");
        }

        //  7.灰度版本测试通过，进行流量切换，支持灰度版本的内测用户
        if (flag != null && flag) {
            String information = task.getLogInformation();
            task.setLogInformation(information + "     " + "5.灰度版本测试通过，开始回滚");
            try {
                this.resumeCanaryFlow();
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation + "  " + "流量更新失败，回滚失败");
                task.setIsSuccess(1);
                taskService.saveOrUpdate(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流量更新失败，回滚失败");
            }
        }
        //删除记录任务状态的全局变量的值
        executionStatus.remove(task.getId());
        String logInformation = task.getLogInformation();
        task.setLogInformation(logInformation + "  " + "6.流量切换成功,回滚！！！");
        task.setIsSuccess(0);
        taskService.saveOrUpdate(task);
        return task.getId();
    }

    @Override
    public long stableRollback(Task task, List<Long> imageIdList) {

        //            如果是old的话表示需要获取到所有tag为old的负载名称和对应的镜像id，然后根据镜像id部署指定版本的将镜像
        Map<Long, String> newList = new HashMap<>();
        try {
            Map<Long, String> map = this.oldImageListAndDeploymentName();
            newList=map;
        } catch (ApiException e) {
            task.setLogInformation("无法读取当前灰度版本的镜像信息");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取当前稳定版本的镜像信息");
        }
//          3.根据获取到的负载信息匹配对应的指定版本镜像
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<Long, String> entry : newList.entrySet()) {
            String deploymentName = entry.getValue();
            Long newImageId = entry.getKey();
            for (Long imageId : imageIdList) {
                if (imageId.equals(newImageId)) {
                    map.put(deploymentName, imageId.toString());
                }
            }
        }
        String logInformation1 = task.getLogInformation();
        task.setLogInformation(logInformation1 + "     " + "2.匹配稳定版本镜像成功");
        //        4.切断灰度版本的流量  变成tester
        try {
            this.cutCanaryFlow();
        } catch (ApiException e) {
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation + "     " + "切断稳定版本的流量失败，回滚失败");
            task.setIsSuccess(1);//1代表任务失败
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切断稳定版本的流量失败，请稍后重试");
        }
        String logInformation2 = task.getLogInformation();
        task.setLogInformation(logInformation2 + "     " + "3.切断稳定版本的流量成功");

        //  5.根据服务名称和镜像URL替换镜像版本
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String serviceName = entry.getKey();
            String imageUrl = entry.getValue();
            try {
                this.updateDeployment(serviceName, imageUrl);
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation + "  " + "更新deployment失败，稳定版发布失败");
                task.setIsSuccess(1);//1代表任务失败
                taskService.saveOrUpdate(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新deployment失败，请检查服务名称是否正确稍后重试");
            }
        }

//        6.镜像部署完成进行点火测试
        //设置任务执行状态
        taskService.saveOrUpdate(task);
        executionStatus.put(task.getId(),true);
        String s = task.getLogInformation();
        task.setLogInformation(s + "     " + "4.镜像部署完成，开始进行点火测试");
        Boolean flag = null;
        //executionStatus记录该任务状态
        while (executionStatus.get(task.getId())&&flag == null) {
            try {
                flag = this.fireTest();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (flag == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "轮询失败");
                }
            } else if (flag == false) {
                task.setLogInformation("点火测试失败，请稍后重试");
                task.setIsSuccess(1);
                throw new BusinessException(ErrorCode.TEST_ERR);
            } else {
                String information = task.getLogInformation();
                task.setLogInformation(information + "  " + "点火测试通过");
            }
        }
        //任务被手动停止
        if (!executionStatus.get(task.getId())) {
            executionStatus.remove(task.getId());
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"     "+"该任务被手动停止，请手动回滚或者操作集群后手动恢复流量");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException("该任务被手动停止，请手动回滚或者操作集群后手动恢复流量",5000,"");
        }

        //  7.灰度版本测试通过，进行流量切换，支持灰度版本的内测用户
        if (flag != null && flag) {
            String information = task.getLogInformation();
            task.setLogInformation(information + "     " + "5.稳定版本测试通过，开始进行回滚");
            try {
                this.resumeCanaryFlow();
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation + "  " + "流量更新失败，稳定版发布失败");
                task.setIsSuccess(1);
                taskService.saveOrUpdate(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流量更新失败，回滚失败");
            }
        }
        executionStatus.remove(task.getId());
        String logInformation = task.getLogInformation();
        task.setLogInformation(logInformation + "  " + "6.流量切换成功,回滚！！！");
        task.setIsSuccess(0);
        taskService.saveOrUpdate(task);
        return task.getId();
    }

//    稳定版发布业务逻辑
    public long stableDeploy(HttpServletRequest request){
        //        0.创建task对象
        Task task = new Task();
        task.setDescription("实现稳定版发布");
        task.setUserId(userService.getLoginUser(request).getId());

        ArrayList<Long> imageList = new ArrayList<>();
        Map<Long, String> newMap = new HashMap<>();//封装所有灰度版本的镜像map（id和imageUrl）

//        1.获取到所有的镜像id列表存储到task任务当中
        try {
            Map<Long,String> map = this.NewImageList();
            newMap=map;
            for (Long id : map.keySet()) {
                imageList.add(id);
            }
        } catch (ApiException e) {
            task.setLogInformation("无法获取到灰度版本镜像列表，稳定版发布失败");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取镜像列表失败，稳定版本发布失败");
        }catch (BusinessException e) {
            task.setLogInformation("无法获取到灰度版本镜像列表，稳定版发布失败");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取新版镜像列表失败，稳定版本发布失败");
        }
        task.setImageList(imageList.toString());
        task.setLogInformation("1.成功获取到所有新版镜像列表");


//        2.获取到所有的稳定版负载的镜像id和负载名称
        Map<Long, String> oldMap = new HashMap<>();
        try {
            Map<Long, String> map = this.oldImageListAndDeploymentName();
            oldMap = map;
        } catch (ApiException e) {
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"     "+"无法获取到稳定版负载的镜像id和负载名称，稳定版发布失败");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取稳定版镜像列表失败，稳定版本发布失败");
        }
        String logInformation3 = task.getLogInformation();
        task.setLogInformation(logInformation3+"     "+"2.成功获取到稳定版负载的镜像id和负载名称");

//        3.根据id对两个map进行匹配创建一个新的mapList来实现镜像的替换部署

        Map<String, String> mapList = new HashMap<>();
        for (Long oldImageId : oldMap.keySet()) {//镜像id和负载名称
            for (Long newImageId : newMap.keySet()) {//镜像id和镜像url
                String oldImageName = imageService.getById(oldImageId).getImageName();
                String newImageName = imageService.getById(newImageId).getImageName();
                if (oldImageName.equals(newImageName)){
                    mapList.put(oldMap.get(oldImageId),newMap.get(newImageId));
                }
            }
        }
//        4.切断灰度版本的流量  变成tester
        try {
            this.cutStableFlow();
        } catch (ApiException e) {
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"     "+"切断稳定版本的流量失败，灰度发布失败");
            task.setIsSuccess(1);//1代表任务失败
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切断稳定版本的流量失败，请稍后重试");
        }
        String logInformation2 = task.getLogInformation();
        task.setLogInformation(logInformation2+"     "+"3.切断稳定版本的流量成功");

        //  5.根据服务名称和镜像URL替换镜像版本
        for (Map.Entry<String, String> entry : mapList.entrySet()) {
            String serviceName = entry.getKey();
            String imageUrl = entry.getValue();
            try {
                this.updateDeployment(serviceName, imageUrl);
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"  "+"更新deployment失败，稳定版发布失败");
                task.setIsSuccess(1);//1代表任务失败
                taskService.saveOrUpdate(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新deployment失败，请检查服务名称是否正确稍后重试");
            }
        }

//        6.镜像部署完成进行点火测试
        //设置任务执行状态
        taskService.saveOrUpdate(task);
        executionStatus.put(task.getId(),true);
        String s = task.getLogInformation();
        task.setLogInformation(s+"     "+"4.镜像部署完成，开始进行点火测试");
        Boolean flag= null;
        //executionStatus记录该任务状态
        while (executionStatus.get(task.getId())&&flag == null) {
            try {
                flag=this.fireTest();
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
                throw new BusinessException(ErrorCode.TEST_ERR);
            }else {
                String information = task.getLogInformation();
                task.setLogInformation(information+"  "+"点火测试通过");
            }
        }
        //任务被手动停止
        if (!executionStatus.get(task.getId())) {
            executionStatus.remove(task.getId());
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"     "+"该任务被手动停止，请手动回滚或者操作集群后手动恢复流量");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException("该任务被手动停止，请手动回滚或者操作集群后手动恢复流量",5000,"");
        }

        //  7.灰度版本测试通过，进行流量切换，支持灰度版本的内测用户
        if (flag != null && flag) {
            String information = task.getLogInformation();
            task.setLogInformation(information+"     "+"5.稳定版本测试通过，开始进行稳定版发布");
            try {
                this.resumeCanaryFlow();
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"  "+"流量更新失败，稳定版发布失败");
                task.setIsSuccess(1);
                taskService.saveOrUpdate(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流量更新失败，稳定版本发布失败");
            }
        }
        executionStatus.remove(task.getId());
        String logInformation = task.getLogInformation();
        task.setLogInformation(logInformation+"  "+"6.流量切换成功,成功发布！！！");
        task.setIsSuccess(0);
        taskService.saveOrUpdate(task);
        return task.getId();
    }

//        灰度发布   根据给定好的map列表进行发布
    public long latestDeploy(HttpServletRequest request,List<Map<String,String>> mapList) throws ApiException {

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
            this.cutCanaryFlow();
        } catch (ApiException e) {
            task.setLogInformation("切断灰度版本的流量失败，灰度发布失败");
            task.setIsSuccess(1);//1代表任务失败
            taskService.saveOrUpdate(task);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "切断灰度版本的流量失败，请稍后重试");
        }
        task.setLogInformation("1.切断灰度版本的流量成功，开始部署服务     ");

        //  记录标签为new的latest版本现在正在运行的服务：镜像
        Map<Long, String> onlineImages = newImageListAndDeploymentName();


        //  2.根据服务名称和镜像名称替换镜像版本
        for (Map<String, String> map : mapList) {
            String serviceName = map.get("service_name");
            String imageId = map.get("image_id");
            Image image = imageService.getById(imageId);
            String imageUrl = image.getImageUrl();
            try {
                this.updateDeployment(serviceName, imageUrl);
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"  "+"更新deployment失败，灰度发布失败");
                task.setIsSuccess(1);//1代表任务失败
                taskService.saveOrUpdate(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新deployment失败，请检查服务名称是否正确稍后重试");
            }
        }


//        3.镜像部署完成进行点火测试
        //设置任务执行状态
        taskService.saveOrUpdate(task);
        executionStatus.put(task.getId(),true);
        String s = task.getLogInformation();
        task.setLogInformation(s+"     "+"2.镜像部署完成，开始进行点火测试");
        Boolean flag= null;
        //executionStatus记录该任务状态
        while (executionStatus.get(task.getId())&&flag == null) {
            try {
                flag=this.fireTest();
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
                throw new BusinessException(ErrorCode.TEST_ERR);
            }else {
                String information = task.getLogInformation();
                task.setLogInformation(information+"  "+"点火测试通过");
            }
        }
        //任务被手动停止
        if (!executionStatus.get(task.getId())) {
            executionStatus.remove(task.getId());
            String logInformation = task.getLogInformation();
            task.setLogInformation(logInformation+"     "+"该任务被手动停止，请手动回滚或者操作集群后手动恢复流量");
            task.setIsSuccess(1);
            taskService.saveOrUpdate(task);
            throw new BusinessException("该任务被手动停止，请手动回滚或者操作集群后手动恢复流量",5000,"");
        }

        //  4.灰度版本测试通过，进行流量切换，支持灰度版本的内测用户
        if (flag != null && flag) {
            String information = task.getLogInformation();
            task.setLogInformation(information+"     "+"3.灰度版本测试通过，开始进行灰度发布");
            try {
                this.resumeCanaryFlow();
            } catch (ApiException e) {
                String logInformation = task.getLogInformation();
                task.setLogInformation(logInformation+"  "+"流量更新失败，灰度发布失败");
                task.setIsSuccess(1);
                taskService.saveOrUpdate(task);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "流量更新失败，灰度发布失败");
            }
        }
        executionStatus.remove(task.getId());
        String logInformation = task.getLogInformation();
        task.setLogInformation(logInformation+"  "+"4.流量切换成功,成功发布！！！");
        task.setIsSuccess(0);
        taskService.saveOrUpdate(task);
        return task.getId();
    }

    @Override
    public void bypassDeploy(Long taskId, String reason) {
        //记录任务失败原因
        taskService.recordTaskFail(taskId,reason);
        //恢复流量
        try {
            resumeStableFlow();
        } catch (ApiException e) {
            throw new BusinessException(ErrorCode.RESUME_FLOW_ERR);
        }
    }


    @Override
    public void stopTask(Long taskId, String reason) {
        //记录任务失败原因
        taskService.recordTaskFail(taskId,reason);
        //停止点火测试
        executionStatus.put(taskId,false);
    }


}

