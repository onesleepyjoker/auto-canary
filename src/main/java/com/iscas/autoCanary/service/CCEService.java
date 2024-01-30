package com.iscas.autoCanary.service;

import io.kubernetes.client.openapi.ApiException;

import java.io.File;
import java.io.IOException;

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

//    获取负载信息
    public String getDeployment(String deploymentName) throws ApiException;

//    替换镜像 更新
    public void updateDeployment(String deploymentName, String imagesURL);

//    根据yaml创建负载
    public void createDeployment(File file) throws IOException, ApiException;

//    删除负载
    public void deleteDeployment(String deploymentName) throws ApiException;

//    查询statefulSet信息
    public String getStatefulSet(String statefulSetName) throws ApiException;

//    更新statefulSet  通过镜像地址
    public void updateStatefulSet(String statefulSetName, String imagesURL) throws ApiException;

//    创建statefulSet 通过yaml
    public void createStatefulSet(File file) throws IOException, ApiException;

//    删除statefulSet
    public void deleteStatefulSet(String statefulSetName) throws ApiException;

}
