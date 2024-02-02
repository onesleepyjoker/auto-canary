package com.iscas.autoCanary.service;

import io.kubernetes.client.openapi.ApiException;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
    public int updateDeployment(String deploymentName, String imagesURL) throws ApiException;

//    根据yaml创建负载
    public int createDeployment(File file) throws IOException, ApiException;

//    删除负载
    public int deleteDeployment(String deploymentName) throws ApiException;

//    查询statefulSet信息
    public String getStatefulSet(String statefulSetName) throws ApiException;

//    更新statefulSet  通过镜像地址
    public int updateStatefulSet(String statefulSetName, String imagesURL) throws ApiException;

//    创建statefulSet 通过yaml
    public int createStatefulSet(File file) throws IOException, ApiException;

//    删除statefulSet
    public int deleteStatefulSet(String statefulSetName) throws ApiException;

//    获取deployment列表
    public List<String> getDeploymentList() throws ApiException;

//    获取statefulSet列表
    public List<String> getStatefulSetList() throws ApiException;

}
