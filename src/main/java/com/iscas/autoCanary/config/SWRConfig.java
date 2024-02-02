package com.iscas.autoCanary.config;

import   com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.swr.v2.SwrClient;
import com.huaweicloud.sdk.swr.v2.region.SwrRegion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SWRConfig {
    @Value("${HUAWEICLOUD_SDK_AK}")
    String ak;
    @Value("${HUAWEICLOUD_SDK_SK}")
    String sk;

    //指定镜像仓库的资源区域
    private final String REGION="cn-north-4";

    @Bean
    public SwrClient swrClient(){
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
