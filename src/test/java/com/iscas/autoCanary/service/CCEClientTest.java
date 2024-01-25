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

    @Test
    public void getIngress() {
        try {
            // 创建 Networking API 实例
            NetworkingV1Api networkingV1Api = new NetworkingV1Api();

            // 查看 Ingress
            String namespace = "default";
            String ingressName = "project";
            V1Ingress ingress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null);
            System.out.println("Ingress before update: " + ingress);
        } catch (ApiException e) {
            System.err.println("Kubernetes API exception: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Test
    public void getIngressAnnotations() throws ApiException {
        //创建network API实例
        NetworkingV1Api networkingV1Api = new NetworkingV1Api();

        String namespace = "default";
        String stableIngressName = "project";
        String canaryIngressName="new-project";
        V1Ingress stableIngress = networkingV1Api.readNamespacedIngress(stableIngressName, namespace, null);
        // 读取 Annotations
        V1ObjectMeta stableMetadata = Optional.ofNullable(stableIngress)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_INGRESS))
                .getMetadata();
        Map<String, String> stableAnnotations = Optional.ofNullable(stableMetadata)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR))
                .getAnnotations();
        //打印annotations
        Optional.ofNullable(stableAnnotations)
                .orElseThrow(()->new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR))
                .forEach((k,v)->System.out.println("key:"+k+",value:"+v));

    }

    @Test
    public void testCutCanaryIngress() throws IOException, ApiException {
        // 加载 kubeconfig
        String kubeConfigPath = "src/main/resources/static/canary-test-kubeconfig.yaml";
        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
        Configuration.setDefaultApiClient(client);

        // 创建 Networking API 实例
        NetworkingV1Api networkingV1Api = new NetworkingV1Api();

        // 读取 Ingress
        String namespace = "default";
        String ingressName = "new-project";
        V1Ingress ingress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null);
        if (ingress == null) {
            //todo 读取失败，返回无法找到该ingress资源
            System.out.println("读取失败，无法找到该ingress资源");
        }
        // 读取 Annotations
        V1ObjectMeta metadata = ingress.getMetadata();
        if (metadata == null) {
            //todo 读取metadata失败，请修改配置文件
            System.out.println("读取metadata失败，请修改配置文件");
        }
        // 移除旧的注解
        Map<String, String> annotations = metadata.getAnnotations();
        if (annotations == null) {
            //todo 读取ingress配置文件失败，请修改配置文件
            System.out.println("读取annotations失败，请修改配置文件");
        }

        // 移除旧的注解
        if (annotations.get("nginx.ingress.kubernetes.io/canary-by-header-pattern") != null) {
            annotations.remove("nginx.ingress.kubernetes.io/canary-by-header-pattern");
        }
        // 更新注解
        annotations.put("nginx.ingress.kubernetes.io/canary-by-header-value", "tester");


        // 设置更新后的注解
        ingress.getMetadata().setAnnotations(annotations);
        // 更新 Ingress
        networkingV1Api.replaceNamespacedIngress(ingressName, namespace, ingress, null, null, null, null);
        System.out.println("Ingress annotations updated successfully");
    }

    @Test
    public void testResumeIngress() throws IOException, ApiException {
        // 加载 kubeconfig
        String kubeConfigPath = "src/main/resources/static/canary-test-kubeconfig.yaml";
        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
        Configuration.setDefaultApiClient(client);

        // 创建 Networking API 实例
        NetworkingV1Api networkingV1Api = new NetworkingV1Api();

        // 读取 Ingress
        String namespace = "default";
        String ingressName = "new-project";
        V1Ingress ingress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null);
        if (ingress == null) {
            //todo 读取失败，返回无法找到该ingress资源
            System.out.println("读取失败，无法找到该ingress资源");
        }
        // 读取 Annotations
        V1ObjectMeta metadata = ingress.getMetadata();
        if (metadata == null) {
            //todo 读取metadata失败，请修改配置文件
            System.out.println("读取metadata失败，请修改配置文件");
        }
        // 移除旧的注解
        Map<String, String> annotations = metadata.getAnnotations();
        if (annotations == null) {
            //todo 读取ingress配置文件失败，请修改配置文件
            System.out.println("读取metadata失败，请修改配置文件");
        }

        // 移除旧的注解
        if (annotations.get("nginx.ingress.kubernetes.io/canary-by-header-value") != null) {
            annotations.remove("nginx.ingress.kubernetes.io/canary-by-header-value");
        }
        // 更新注解
        annotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern", "^(new|tester)$");


        // 设置更新后的注解
        ingress.getMetadata().setAnnotations(annotations);
        // 更新 Ingress
        networkingV1Api.replaceNamespacedIngress(ingressName, namespace, ingress, null, null, null, null);
        System.out.println("Ingress annotations updated successfully");
    }
}
