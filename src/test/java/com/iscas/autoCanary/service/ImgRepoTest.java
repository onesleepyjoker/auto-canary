package com.iscas.autoCanary.service;

import com.google.protobuf.DescriptorProtos;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.swr.v2.SwrClient;
import com.huaweicloud.sdk.swr.v2.model.ListReposDetailsRequest;
import com.huaweicloud.sdk.swr.v2.model.ListReposDetailsResponse;
import com.huaweicloud.sdk.swr.v2.model.ShowReposResp;
import com.huaweicloud.sdk.swr.v2.region.SwrRegion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试获取镜像列表
 */
@SpringBootTest(properties = "spring.profiles.active=windpo")
public class ImgRepoTest {

    @Resource
    SwrClient swrClient;

    @Test
    public void getImgList(){
        ListReposDetailsRequest request = new ListReposDetailsRequest();
        request.withNamespace("isrc-test-develop");
        try {
            ListReposDetailsResponse response = swrClient.listReposDetails(request);
            List<ShowReposResp> res = response.getBody();
            List<ShowReposResp> newRes=res.stream().map((value)->{
                value.setCategory(null);
                value.setSize(null);
                value.setIsPublic(null);
                value.setNumImages(null);
                value.setNumDownload(null);
                value.setLogo(null);
                value.setUrl(null);
                value.setInternalPath(null);
                value.setDomainName(null);
                value.setStatus(null);
                return value;
            }).collect(Collectors.toList());
            System.out.println(newRes.toString());
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (RequestTimeoutException e) {
            e.printStackTrace();
        } catch (ServiceResponseException e) {
            e.printStackTrace();
            System.out.println(e.getHttpStatusCode());
            System.out.println(e.getRequestId());
            System.out.println(e.getErrorCode());
            System.out.println(e.getErrorMsg());
        }
    }

}
