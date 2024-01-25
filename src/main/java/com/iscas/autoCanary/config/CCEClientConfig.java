package com.iscas.autoCanary.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
    @Bean
    public void cceClient() throws IOException {
        // 加载 kubeconfig
        String kubeConfigPath = "src/main/resources/static/canary-test-kubeconfig.yaml";
        ApiClient client = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath))).build();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);

    }
}
