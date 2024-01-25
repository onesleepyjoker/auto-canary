package com.iscas.autoCanary.service.impl;

import com.iscas.autoCanary.common.ErrorCode;
import com.iscas.autoCanary.exception.BusinessException;
import com.iscas.autoCanary.service.CCEService;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author windpo
 * @description CCE相关接口实现类
 */
@Service
@Slf4j
public class CCEServiceImpl implements CCEService {
    //ingress名称常量
    final String NAMESPACE="default";
    final String STABLE_INGRESS_NAME="project";
    final String CANARY_INGRESS_NAME="new-project";
    //灰度发布相关常量
    final String HEADER="canary";
    final String CANARY_HEADER_TEST_PATTERN="^tester$";
    final String CANARY_HEADER_NORMAL_PATTERN="^(new|tester)$";
    final String STABLE_HEADER_VALUE="tester";

    /**
     * 获取ingress的注解
     * @param namespace
     * @param ingressName
     * @return nonNullable
     */
    protected Map<String,String> getAnnotations(String namespace,String ingressName) throws ApiException {
        // 创建 Networking API 实例
        NetworkingV1Api networkingV1Api = new NetworkingV1Api();
        V1Ingress stableIngress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null);
        // 读取 Annotations
        V1ObjectMeta stableMetadata = Optional.ofNullable(stableIngress)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_INGRESS))
                .getMetadata();
        Map<String, String> stableAnnotations = Optional.ofNullable(stableMetadata)
                .orElseThrow(() -> new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR))
                .getAnnotations();
        //返回annotations，annotations==null, throw Exception
        return Optional.ofNullable(stableAnnotations)
                .orElseThrow(()->new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR));
    }

    /**
     * 更新ingress的注解
     * @param namespace
     * @param ingressName
     * @param annotations
     * @throws ApiException
     */
    protected void updateAnnotations(String namespace,String ingressName,Map<String,String> annotations) throws ApiException{
        // 创建 Networking API 实例
        NetworkingV1Api networkingV1Api = new NetworkingV1Api();
        V1Ingress ingress = networkingV1Api.readNamespacedIngress(ingressName, namespace, null);
        V1ObjectMeta metadata = Optional.ofNullable(ingress)
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_INGRESS))
                .getMetadata();
        Optional.ofNullable(metadata)
                .orElseThrow(()->new BusinessException(ErrorCode.INGRESS_CONFIG_ERROR))
                .setAnnotations(annotations);
        networkingV1Api.replaceNamespacedIngress(ingressName,namespace,ingress,null,null,null,null);
    }

    /**
     * 用于稳定版开始测试按钮
     */
    @Override
    public void cutStableFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //切断内侧用户流量
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary","false");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary","true");
        // 保证状态为目标状态
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern",CANARY_HEADER_NORMAL_PATTERN);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value",STABLE_HEADER_VALUE);

        // 更新注解
        updateAnnotations(NAMESPACE,CANARY_INGRESS_NAME,canaryAnnotations);
        updateAnnotations(NAMESPACE,STABLE_INGRESS_NAME,stableAnnotations);
    }

    /**
     * 用于稳定版正式发布按钮
     */
    @Override
    public void resumeStableFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //切断内侧用户流量
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary","false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary","true");
        // 保证状态为目标状态
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value",STABLE_HEADER_VALUE);
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern",CANARY_HEADER_NORMAL_PATTERN);

        // 更新注解
        updateAnnotations(NAMESPACE,STABLE_INGRESS_NAME,stableAnnotations);
        updateAnnotations(NAMESPACE,CANARY_INGRESS_NAME,canaryAnnotations);
    }

    /**
     * 用于灰度版开始测试按钮
     * @throws ApiException
     */
    @Override
    public void cutCanaryFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //切断内侧用户流量
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern",CANARY_HEADER_TEST_PATTERN);
        // 保证状态为目标状态
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary","true");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary","false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value",STABLE_HEADER_VALUE);

        // 更新注解
        updateAnnotations(NAMESPACE,CANARY_INGRESS_NAME,canaryAnnotations);
        updateAnnotations(NAMESPACE,STABLE_INGRESS_NAME,stableAnnotations);
    }

    /**
     * 用于灰度版正式发布按钮
     */
    @Override
    public void resumeCanaryFlow() throws ApiException {
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);

        //恢复内侧用户流量
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-pattern", CANARY_HEADER_NORMAL_PATTERN);
        //保证状态为目标状态
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary","true");
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary","false");
        canaryAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header",HEADER);
        stableAnnotations.put("nginx.ingress.kubernetes.io/canary-by-header-value",STABLE_HEADER_VALUE);

        // 更新注解
        updateAnnotations(NAMESPACE,CANARY_INGRESS_NAME,canaryAnnotations);
        updateAnnotations(NAMESPACE,STABLE_INGRESS_NAME,stableAnnotations);
    }

    @Override
    public String getIngressStatus() throws ApiException {
        Map<String, String> stableAnnotations = getAnnotations(NAMESPACE, STABLE_INGRESS_NAME);
        Map<String, String> canaryAnnotations = getAnnotations(NAMESPACE, CANARY_INGRESS_NAME);
        //todo 对annotations进行处理并返回
        return null;
    }
}
