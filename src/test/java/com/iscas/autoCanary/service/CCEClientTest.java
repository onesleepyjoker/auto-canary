package com.iscas.autoCanary.service;


import cn.hutool.core.lang.UUID;
import com.google.gson.Gson;
import com.huaweicloud.sdk.swr.v2.model.ShowReposResp;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.pojo.Image;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.Yaml;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.annotation.XmlType;
import java.io.*;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author windpo
 * @description 测试对CCE集群的操作
 */
@SpringBootTest
public class CCEClientTest {
    //ingress名称常量
    final String NAMESPACE = "default";
    final String STABLE_INGRESS_NAME = "project";
    final String CANARY_INGRESS_NAME = "new-project";
    //灰度发布相关常量
    final String HEADER = "canary";
    final String CANARY_HEADER_TEST_PATTERN = "^tester$";
    final String CANARY_HEADER_NORMAL_PATTERN = "^(new|tester)$";
    final String STABLE_HEADER_PATTERN = "^tester$";

    @Autowired
    private ImageRepoService imageRepoService;

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
     * 获取ingess
     */
    @Test
    public void getIngress() {
        try {
            // 创建 Networking API 实例
            NetworkingV1Api networkingV1Api = new NetworkingV1Api();

            // 查看 Ingress
            V1Ingress canaryIngress = networkingV1Api.readNamespacedIngress(CANARY_INGRESS_NAME, NAMESPACE, null);
            V1Ingress stableIngress = networkingV1Api.readNamespacedIngress(STABLE_INGRESS_NAME, NAMESPACE, null);

            //print
            System.out.println("canary ingress info:: " + canaryIngress);
            System.out.println("stable ingress info:: " + canaryIngress);

        } catch (ApiException e) {
            System.err.println("Kubernetes API exception: " + e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * 打印两个ingress的注解
     *
     * @throws ApiException
     */
    @Test
    public void printGetAnnotations() throws ApiException {
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);

        System.out.println("stable-annotations:");
        stableAnnotations.forEach((k, v) -> System.out.println(k + ":" + v));
        System.out.println("canary-annotations:");
        canaryAnnotations.forEach((k, v) -> System.out.println(k + ":" + v));

        String version = getVersion(stableAnnotations, canaryAnnotations);
        System.out.println(version);
//        stable-annotations:
//nginx.ingress.kubernetes.io/canary:false
//nginx.ingress.kubernetes.io/canary-by-header:canary
//nginx.ingress.kubernetes.io/canary-by-header-value:tester
//      canary-annotations:
//nginx.ingress.kubernetes.io/canary:true
//nginx.ingress.kubernetes.io/canary-by-header:canary
//nginx.ingress.kubernetes.io/canary-by-header-pattern:^(new|tester)$
    }

    public String getVersion(Map<String, String> statbleAnnotations, Map<String, String> canaryAnnotations) {
        List<String> stableValues = statbleAnnotations.values().stream().collect(Collectors.toList());
        List<String> canaryValues = canaryAnnotations.values().stream().collect(Collectors.toList());
        if (stableValues.get(0).equals("false") && canaryValues.get(0).equals("true")) {
            if (canaryValues.get(2).equals("^(new|tester)$")) {
                return "当前阶段：正常运行阶段";
            } else if (canaryValues.get(2).equals("^tester$")) {
                return "当前阶段：灰度版本测试阶段";
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请求头规则出错，请联系工作人员");
            }
        } else if (stableValues.get(0).equals("true") && canaryValues.get(0).equals("false")) {
            if (stableValues.get(2).equals("^(new|tester)$")) {
                return "当前阶段：正常运行阶段";
            } else if (stableValues.get(2).equals("^tester$")) {
                return "当前阶段：稳定版本测试阶段";
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请求头规则出错，请联系工作人员");
            }
        }
        return "查询失败";
    }


    /**
     * 用于稳定版开始测试按钮
     */
    @Test
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

        printGetAnnotations();
        String version = getVersion(stableAnnotations, canaryAnnotations);
        System.out.println(version);

        System.out.println("update success");
    }

    /**
     * 用于稳定版正式发布按钮
     */
    @Test
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

        printGetAnnotations();
        String version = getVersion(stableAnnotations, canaryAnnotations);
        System.out.println(version);

        System.out.println("update success");
    }

    /**
     * 用于灰度版开始测试按钮
     *
     * @throws ApiException
     */
    @Test
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

        printGetAnnotations();
        String version = getVersion(stableAnnotations, canaryAnnotations);
        System.out.println(version);

        System.out.println("update success");
    }

    /**
     * 用于灰度版正式发布按钮
     */
    @Test
    public void resumeCanaryFlow() throws ApiException {
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

        printGetAnnotations();
        String version = getVersion(stableAnnotations, canaryAnnotations);
        System.out.println(version);

        System.out.println("update success");
    }

    //    通过读取配置文件的方法加载deployment
    @Test
    void createByYaml() throws IOException, ApiException {

        String path = "D:\\中科院实习\\user-center-backend\\user-center-backend\\src\\main\\resources\\static\\nginx-deployment.yaml";

        // 创建 AppsV1Api 对象
        AppsV1Api appsV1Api = new AppsV1Api();

        // 从 YAML 文件中加载 Deployment 对象
        V1Deployment deployment = (V1Deployment) Yaml.load(new File(path));
        String kind = deployment.getKind();
        System.out.println(kind);
        // 在指定命名空间创建 Deployment
        String namespace2 = "default";
        V1Deployment createdDeployment = appsV1Api.createNamespacedDeployment(namespace2, deployment, null, null, null, null);

        System.out.println("Deployment created successfully: " + createdDeployment.getMetadata().getName());
    }

    @Test
    void deleteDeployment() {
        // 创建 AppsV1Api 对象
        AppsV1Api appsV1Api = new AppsV1Api();

        // 删除 Deployment
        String namespace = "default";
        String deploymentName = "my-nginx-deployment";
        try {
            appsV1Api.deleteNamespacedDeployment(deploymentName, namespace, null, null, null, null, null, null);
            System.out.println("Deployment deleted successfully.");
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    //    获取到deployment实例的具体信息
    @Test
    public void getDeployment() {
        try {
            // 创建 apps API 实例
            AppsV1Api appsV1Api = new AppsV1Api();

            // 查看 Ingress
            V1Deployment canaryDeployment = appsV1Api.readNamespacedDeployment("project-canary", "default", null);
            V1Deployment stableDeployment = appsV1Api.readNamespacedDeployment("project", "default", null);

            //print
            System.out.println("stable deployment info:: " + stableDeployment);

        } catch (ApiException e) {
            System.err.println("Kubernetes API exception: " + e.getMessage());
            e.printStackTrace();
        }

    }

    //    替换deployment的镜像
    @Test
    public void updateDeployment() {
        try {
            // 创建 apps API 实例
            AppsV1Api appsV1Api = new AppsV1Api();

            // 查看 Ingress
            V1Deployment canaryDeployment = appsV1Api.readNamespacedDeployment("project-canary", "default", null);
            V1Deployment stableDeployment = appsV1Api.readNamespacedDeployment("project", "default", null);

//            替换镜像
            V1Container v1Container = stableDeployment.getSpec().getTemplate().getSpec().getContainers().get(0);
            System.out.println(v1Container);
            v1Container.setImage("nginx:latest");
            stableDeployment.getSpec().getTemplate().getSpec().setContainers(Arrays.asList(v1Container));
//            getSpec就是获取到前面对象的标准对象，每次拿到一个对象之后都需要调用这个方法
            appsV1Api.replaceNamespacedDeployment("project", "default", stableDeployment, null, null, null, null);

            System.out.println("----------------------------------------------------------------------");

        } catch (ApiException e) {
            System.err.println("Kubernetes API exception: " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Test
        //    获取有状态负载信息
    void getStatefulSet() {
        try {
            // 创建 apps API 实例
            AppsV1Api appsV1Api = new AppsV1Api();

            // 查看 statefulset信息
            V1StatefulSet mysqlStatefulSet = appsV1Api.readNamespacedStatefulSet("mysql", "default", null);

            System.out.println(mysqlStatefulSet);

        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    //    根据yaml创建有状态负载
    @Test
    void createStatefulSet() {
        try {
            String path = "D:\\中科院实习\\user-center-backend\\user-center-backend\\src\\main\\resources\\static\\mysql-statefutset.yml";
            // 创建 apps API 实例
            AppsV1Api appsV1Api = new AppsV1Api();

            // 读取 yaml 文件
            InputStream yamlStream = getClass().getClassLoader
                    ().getResourceAsStream("mysql-statefulset.yaml");

            // 创建 StatefulSet 对象
            V1StatefulSet statefulSet = (V1StatefulSet) Yaml.load(new File(path));

            // 创建 StatefulSet
            V1StatefulSet createdStatefulSet = appsV1Api.createNamespacedStatefulSet("default", statefulSet, null, null, null, null);
            List<V1PersistentVolumeClaim> volumeClaimTemplates = createdStatefulSet.getSpec().getVolumeClaimTemplates();
            System.out.println("----------------------------------------------------------------------");

            System.out.println("Created StatefulSet: " + createdStatefulSet.getMetadata().getName());
        } catch (ApiException | IOException e) {
            System.err.println("Kubernetes API exception: " + e.getMessage());
            e.printStackTrace();
        }

    }

//    获取到当前节点的所有负载deployment
    @Test
    public void getDeployments() throws ApiException {
        AppsV1Api appsV1Api = new AppsV1Api();
        V1DeploymentList v1DeploymentList = appsV1Api.listDeploymentForAllNamespaces(null, null,
                null, null, null, null,
                null, null, null,
                null, null);
        for (V1Deployment deployment : v1DeploymentList.getItems()) {
            System.out.println(deployment.getMetadata().getName());
        }
    }

    @Value("${secret.name}")
    private String SECRET_NAME;

    @Test
    void testReadyaml(){
        System.out.println("secret name: " + SECRET_NAME);
        System.out.println(UUID.fastUUID().toString());
    }

    @Test
    void createimage() {
//        获取华为云仓库当中的所有镜像列表
        List<ShowReposResp> cceImgList = imageRepoService.getCCEImgList(null);
        System.out.println(cceImgList.get(0).getUrl());
        System.out.println(cceImgList.get(0).getPath());
    }

}

