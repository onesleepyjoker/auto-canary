package com.iscas.autoCanary.model.domain.request;


import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author <a href=#">一只小小丑</a>
 * @from 中科院软件所
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

}
