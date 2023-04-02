package com.keyvault;

import com.keyvault.entities.Tokens;

import java.io.Serial;
import java.io.Serializable;

public class Request implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757621L;
    public static String GET = "GET";
    public static String DELETE = "DELETE";
    public static String DELETE_USER = "DELETE-USER";
    public static String INSERT = "INSERT";
    public static String VERIFY = "VERIFY";
    public static String MOD = "MOD";
    public static String LOGIN = "LOGIN";
    public static String REGISTER = "REGISTER";
    public static String CLEAR_DEVICE = "CLEAR-DEVICE";
    public static String TOTP = "TOTP";
    private final Object[] object;
    private final String operationCode;
    private final Tokens token;

    public Request(Object[] object, String operationCode, Tokens token){
        this.object = object;
        this.operationCode = operationCode;
        this.token = token;
    }

    public Request(String operationCode, Tokens token){
        this.object = null;
        this.operationCode = operationCode;
        this.token = token;
    }

    public Request(Object[] object, String operationCode){
        this.object = object;
        this.operationCode = operationCode;
        this.token = null;
    }

    public Object[] getContent() {
        return object;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public Tokens getToken(){ return token; }
}
