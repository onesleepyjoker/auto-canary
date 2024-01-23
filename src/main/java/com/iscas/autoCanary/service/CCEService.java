package com.iscas.autoCanary.service;

/**
 * @author windpo
 * @description CCE相关接口实现
 */
public interface CCEService {
    public void cutStableFlow();
    public void resumeStableFlow();
    public void cutCanaryFlow();
    public void resumeCanaryFlow();
}
