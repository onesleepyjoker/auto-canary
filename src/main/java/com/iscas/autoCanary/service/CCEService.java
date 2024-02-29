package com.iscas.autoCanary.service;

import com.iscas.autoCanary.pojo.output.ImageOutput;
import io.kubernetes.client.openapi.ApiException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    public int createDeployment(String stringYaml) throws IOException, ApiException;

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

    //    获取所有namespace当中的new pod列表里面的镜像列表
    public List<ImageOutput> getNewImageList() throws ApiException;

    //    获取所有namespace当中的old pod列表里面的镜像列表
    public List<ImageOutput> getOldImageList() throws ApiException;

    //  对标签为new 的镜像进行替换
    public int updateNewImageList(Map<String, String> imageNameAndUrl) throws ApiException;

    //    模拟点火测试的接口方法
    public boolean fireTest() throws InterruptedException;

    //    根据所有标签为new的无状态复杂查找对应的镜像id和镜像url
    public Map<Long,String> NewImageList() throws ApiException;

//    找出所有标签为new的无状态五载的镜像id和服务名称
    public Map<Long,String> newImageListAndDeploymentName() throws ApiException;

//        找出所有标签为old的负载当中的镜像id和镜像url
    public  Map<Long,String> oldImageList() throws ApiException;

    //    根据所有标签为old的无状态复杂查找对应的镜像id和服务名称
    public Map<Long,String> oldImageListAndDeploymentName() throws ApiException;
}
