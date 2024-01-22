package com.iscas.autoCanary.config;

import com.huaweicloud.sdk.cce.v3.CceClient;
import com.huaweicloud.sdk.core.HttpListener;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.http.HttpConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.stream.Collectors;

/**
 * @author windpo
 * @description 初始化CCE客户端相关配置
 */
@Configuration
public class CCEConfig {
    /**
     * 相关配置常量
     */
    // 用于指定集群endpoint
    private final String CCE_ENDPOINT = "cce.cn-east-3.myhuaweicloud.com";

    @Value("${HUAWEICLOUD_SDK_AK}")
    private String ak;
    @Value("${HUAWEICLOUD_SDK_SK}")
    private String sk;
    public CceClient cceClient(){
        // 配置认证信息
        ICredential auth = new BasicCredentials()
                .withAk(ak)
                .withSk(sk);

        HttpConfig httpConfig = HttpConfig.getDefaultHttpConfig();
        httpConfig.withIgnoreSSLVerification(true);
        // 注册监听器后打印原始请求信息,请勿用于生产环境
        HttpListener requestListener = HttpListener.forRequestListener(listener ->
                System.out.printf("> Request %s %s\n> Headers:\n%s\n> Body: %s\n",
                        listener.httpMethod(),
                        listener.uri(),
                        listener.headers().entrySet().stream()
                                .flatMap(entry -> entry.getValue().stream().map(
                                        value -> "\t" + entry.getKey() + ": " + value))
                                .collect(Collectors.joining("\n")),
                        listener.body().orElse("")));
        httpConfig.addHttpListener(requestListener);
        // 注册监听器后打印原始响应信息,请勿用于生产环境
        HttpListener responseListener = HttpListener.forResponseListener(listener ->
                System.out.printf("< Response %s %s %s\n< Headers:\n%s\n< Body: %s\n",
                        listener.httpMethod(),
                        listener.uri(),
                        listener.statusCode(),
                        listener.headers().entrySet().stream()
                                .flatMap(entry -> entry.getValue().stream().map(
                                        value -> "\t" + entry.getKey() + ": " + value))
                                .collect(Collectors.joining("\n")),
                        listener.body().orElse("")));
        httpConfig.addHttpListener(responseListener);


        // 创建服务客户端
        return CceClient.newBuilder()
                .withHttpConfig(httpConfig)
                .withCredential(auth)
                .withEndpoint(CCE_ENDPOINT)
                .build();
    }
}
