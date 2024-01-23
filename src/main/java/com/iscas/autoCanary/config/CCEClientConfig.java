package com.iscas.autoCanary.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.util.stream.Collectors;

/**
 * @author windpo
 * @description 初始化CCE客户端相关配置
 */
@Configuration
public class CCEClientConfig {
    /**
     * 相关配置常量
     */
    // 用于指定集群endpoint
    private final String CCE_ENDPOINT = "cce.cn-east-3.myhuaweicloud.com";
//    @Bean
//    public CceClient cceClient(){
//        // 加载 kubeconfig 文件
//        String kubeConfigPath = "path/to/your/kubeconfig";
//        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
//
//        // 创建 Kubernetes API 客户端
//        NetworkingV1Api api = new NetworkingV1Api();
//    }
}
