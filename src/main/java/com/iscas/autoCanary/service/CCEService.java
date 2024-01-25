package com.iscas.autoCanary.service;

import io.kubernetes.client.openapi.ApiException;

/**
 * @author windpo
 * @description CCE相关接口实现
 */
public interface CCEService {
    public void cutStableFlow() throws ApiException;
    public void resumeStableFlow() throws ApiException;
    public void cutCanaryFlow() throws ApiException;
    public void resumeCanaryFlow() throws ApiException;
    public String getIngressStatus() throws ApiException;
}
