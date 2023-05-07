package com.keyvault;

import com.keyvault.database.models.Devices;
import com.keyvault.database.models.Tokens;
import com.keyvault.database.models.Users;

import java.io.Serial;
import java.io.Serializable;

public class Request implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757621L;
    public static String GET = "GET";
    public static String GET_DEVICES = "GET-DEVICES";

    public static String DELETE = "DELETE";
    public static String DELETE_USER = "DELETE-USER";
    public static String INSERT = "INSERT";
    public static String VERIFY = "VERIFY";
    public static String MOD = "MOD";
    public static String LOGIN = "LOGIN";
    public static String REGISTER = "REGISTER";
    public static String CLEAR_DEVICE = "CLEAR-DEVICE";
    public static String TOTP = "TOTP";
    public static String VERIFY_TOTP = "VERIFY-TOTP";
    private Users user = null;
    private Devices device = null;
    private String operationCode = null;
    private Object content = null;
    private Tokens token = null;

    public Request(Object responseContent, String operationCode, Tokens token){
        this.content = responseContent;
        this.operationCode = operationCode;
        this.token = token;
    }

    public Request(Users user, Devices device, String operationCode){
        this.user = user;
        this.device = device;
        this.operationCode = operationCode;
    }

    public Request(String operationCode, Tokens token){
        this.operationCode = operationCode;
        this.token = token;
    }

    public Request(){}

    public Object getContent() {
        return content;
    }

    public String getOperationCode() {
        return operationCode;
    }

    public Tokens getToken(){ return token; }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public Devices getDevice() {
        return device;
    }

    public void setDevice(Devices device) {
        this.device = device;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
