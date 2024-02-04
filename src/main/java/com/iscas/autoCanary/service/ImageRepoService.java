package com.iscas.autoCanary.service;

import com.huaweicloud.sdk.swr.v2.model.ShowReposResp;
import com.iscas.autoCanary.common.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


public interface ImageRepoService { List<ShowReposResp> getCCEImgList(String namespace);
}
