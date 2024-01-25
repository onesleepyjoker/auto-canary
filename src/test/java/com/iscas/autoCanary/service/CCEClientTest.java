package com.iscas.autoCanary.service;


import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.exception.BusinessException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author windpo
 * @description 测试对CCE集群的操作
 */
@SpringBootTest
public class CCEClientTest {
    //ingress名称常量
    final String NAMESPACE="default";
    final String STABLE_INGRESS_NAME="project";
    final String CANARY_INGRESS_NAME="new-project";
    //灰度发布相关常量
    final String HEADER="canary";
    final String CANARY_HEADER_TEST_PATTERN="^tester$";
    final String CANARY_HEADER_NORMAL_PATTERN="^(new|tester)$";
    final String STABLE_HEADER_VALUE="tester";

    /**
     * 获取ingress的注解
     * @param namespace
     * @param ingressName
     * @return nonNullable
     */
    protected Map<String,String> getAnnotations(String namespace,String ingressName) throws ApiException {
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
                .orElseThrow(()->new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR));
    }

    /**
     * 更新ingress的注解
     * @param namespace
     * @param ingressName
     * @param annotations
     * @throws ApiException
     */
    protected void updateAnnotations(String namespace,String ingressName,Map<String,String> annotations) throws ApiException{
        // 创建 Networking API 实例
        NetworkingV1Api networkingV1Api = new NetworkingV1Api();
        V1Ingress ingress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null);
        V1ObjectMeta metadata = Optional.ofNullable(ingress)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_INGRESS))
                .getMetadata();
        Optional.ofNullable(metadata)
                .orElseThrow(()->new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR))
                .setAnnotations(annotations);
        networkingV1Api.replaceNamespacedIngress(ingressName,namespace,ingress,null,null,null,null);
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
     * @throws ApiException
     */
    @Test
    public void printGetAnnotations() throws ApiException {
        Map<String, String> stableAnnotatinos = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);

        System.out.println("stable-annotations:");
        stableAnnotatinos.forEach((k,v)-> System.out.println(k+":"+v));
        System.out.println("canary-annotations:");
        canaryAnnotations.forEach((k,v)-> System.out.println(k+":"+v));
    }


    /**
     * 用于稳定版开始测试按钮
     */
    @Test
    public void cutStableFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //切断内侧用户流量
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary","false");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary","true");
        // 保证状态为目标状态
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern",CANARY_HEADER_NORMAL_PATTERN);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value",STABLE_HEADER_VALUE);

        // 更新注解
        updateAnnotations(NAMESPACE,CANARY_INGRESS_NAME,canaryAnnotations);
        updateAnnotations(NAMESPACE,STABLE_INGRESS_NAME,stableAnnotations);

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
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary","false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary","true");
        // 保证状态为目标状态
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value",STABLE_HEADER_VALUE);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern",CANARY_HEADER_NORMAL_PATTERN);

        // 更新注解
        updateAnnotations(NAMESPACE,STABLE_INGRESS_NAME,stableAnnotations);
        updateAnnotations(NAMESPACE,CANARY_INGRESS_NAME,canaryAnnotations);

        System.out.println("update success");
    }

    /**
     * 用于灰度版开始测试按钮
     * @throws ApiException
     */
    @Test
    public void cutCanaryFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //切断内侧用户流量
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern",CANARY_HEADER_TEST_PATTERN);
        // 保证状态为目标状态
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary","true");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary","false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value",STABLE_HEADER_VALUE);

        // 更新注解
        updateAnnotations(NAMESPACE,CANARY_INGRESS_NAME,canaryAnnotations);
        updateAnnotations(NAMESPACE,STABLE_INGRESS_NAME,stableAnnotations);

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
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary","true");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary","false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value",STABLE_HEADER_VALUE);

        // 更新注解
        updateAnnotations(NAMESPACE,CANARY_INGRESS_NAME,canaryAnnotations);
        updateAnnotations(NAMESPACE,STABLE_INGRESS_NAME,stableAnnotations);

        System.out.println("update success");
    }


}
