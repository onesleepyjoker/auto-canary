package com.iscas.autoCanary.common;


/**
 * 错误码
 *
 * @author <a href=#">一只小小丑</a>
 * @from 中科院软件所
 */
public enum ErrorCode {

    SUCCESS(0, "ok", ""),
    NO_INGRESS(30000,"没有相关Ingress资源",""),
    INGRESS_CONFIG_ERROR(30001,"Ingress配置文件错误",""),
    PARAMS_ERROR(40000, "请求参数错误", ""),
    NULL_ERROR(40001, "请求数据为空", ""),
    NOT_LOGIN(40100, "未登录", ""),
    NO_AUTH(40101, "无权限", ""),
    CREATE_ERROR(40002, "重复创建", ""),
    SYSTEM_ERROR(50000, "系统内部异常", "");


    private final int code;

    /**
     * 状态码信息
     */
    private final String message;

    /**
     * 状态码描述（详情）
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


    public String getDescription() {
        return description;
    }
}
