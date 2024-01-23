package com.iscas.autoCanary.service;


import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * @author windpo
 * @description 测试对CCE集群的操作
 */
//@SpringBootTest
public class CCEClientTest {
//    @Resource
//    CceClient cceClient;
    private final String CLUSTER_ID="31c93047-b355-11ee-ad2b-0255ac1002c8";

    @Test
    public void getIngress(){
        try {
            // 加载 kubeconfig
            String kubeConfigPath = "src/main/resources/static/canary-test-kubeconfig.yaml";
            ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
            Configuration.setDefaultApiClient(client);

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
        } catch (IOException e) {
            System.err.println("File IO exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void reformConfigMap(){

    }
}
