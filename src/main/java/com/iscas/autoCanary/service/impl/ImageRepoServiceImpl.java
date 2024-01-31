package com.iscas.autoCanary.service.impl;

import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.swr.v2.SwrClient;
import com.huaweicloud.sdk.swr.v2.model.ListReposDetailsRequest;
import com.huaweicloud.sdk.swr.v2.model.ListReposDetailsResponse;
import com.huaweicloud.sdk.swr.v2.model.ShowReposResp;
import com.iscas.autoCanary.common.BaseResponse;
import com.iscas.autoCanary.common.ResultUtils;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.service.CCEService;
import com.iscas.autoCanary.service.ImageRepoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.xml.transform.Result;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImageRepoServiceImpl implements ImageRepoService {
    @Resource
    SwrClient swrClient;

    @Override
    public BaseResponse<List<ShowReposResp>> getCCEImgList(String namespace) {
        ListReposDetailsRequest request = new ListReposDetailsRequest();
        if(namespace!=null){
            request.withNamespace(namespace);
        }
        ListReposDetailsResponse response = swrClient.listReposDetails(request);
        List<ShowReposResp> imgsInfo = response.getBody();

        List<ShowReposResp> res=imgsInfo.stream().map((imgInfo)->{
                imgInfo.setCategory(null);
                imgInfo.setSize(null);
                imgInfo.setIsPublic(null);
                imgInfo.setNumImages(null);
                imgInfo.setNumDownload(null);
                imgInfo.setLogo(null);
                imgInfo.setUrl(null);
                imgInfo.setInternalPath(null);
                imgInfo.setDomainName(null);
                imgInfo.setStatus(null);
                return imgInfo;
        }).collect(Collectors.toList());
        return ResultUtils.success(res);
    }
}
