package com.iscas.autoCanary.config;

import com.google.protobuf.LazyField;
import   com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.swr.v2.SwrClient;
import com.huaweicloud.sdk.swr.v2.region.SwrRegion;
import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.controller.ImageController;
import com.iscas.autoCanary.exception.BusinessException;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Configuration
public class SWRConfig {
//    @Value("${HUAWEICLOUD_SDK_AK}")
//    String ak;
//    @Value("${HUAWEICLOUD_SDK_SK}")
//    String sk;

    public static final String NAMESPACE="isrc-test-develop-shanghai";

    //指定镜像仓库的资源区域
    private static final String REGION = "cn-east-3";

//    @Bean
//    public SwrClient swrClient() {
//        ICredential auth = new BasicCredentials()
//                .withAk(ak)
//                .withSk(sk);
//
//        SwrClient client = SwrClient.newBuilder()
//                .withCredential(auth)
//                .withRegion(SwrRegion.valueOf(REGION))
//                .build();
//        return client;
//    }

//    通过读取k8s当中的secret从而达到安全的连接华为云仓库当中的数据列表
    @Bean
    public SwrClient swrClient(ApiClient apiClient) {
        CoreV1Api coreV1Api = new CoreV1Api();
        V1Secret secret;
        try {
            secret = coreV1Api.readNamespacedSecret("swr-secret", "default", null);
            System.out.println(secret.getData());
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
// 从 Secret 中获取 AK 和 SK 的值
        String ak = new String(secret.getData().get("HUAWEICLOUD_SDK_AK"), StandardCharsets.UTF_8);
        String sk = new String(secret.getData().get("HUAWEICLOUD_SDK_SK"), StandardCharsets.UTF_8);

// 在应用程序中使用 AK 和 SK
        ICredential auth = new BasicCredentials()
                .withAk(ak)
                .withSk(sk);

        SwrClient client = SwrClient.newBuilder()
                .withCredential(auth)
                .withRegion(SwrRegion.valueOf(REGION))
                .build();
        return client;
    }
}
