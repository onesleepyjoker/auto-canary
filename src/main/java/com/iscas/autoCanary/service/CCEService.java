package com.iscas.autoCanary.service;

import io.kubernetes.client.openapi.ApiException;

/**
 * @author windpo
 * @description CCE相关接口实现
 */
public interface CCEService {
    public void cutStableFlow();
    public void resumeStableFlow();
    public void cutCanaryFlow() throws ApiException;
    public void resumeCanaryFlow() throws ApiException;
    public String getIngressStatus() throws ApiException;
}
