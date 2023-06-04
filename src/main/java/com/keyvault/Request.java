package com.keyvault;

import com.keyvault.database.models.Devices;
import com.keyvault.database.models.SessionToken;
import com.keyvault.database.models.Users;

import java.io.Serial;
import java.io.Serializable;

public class Request implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757621L;
    private Users user = null;
    private Devices device = null;
    private String operationCode = null;
    private Object content = null;
    private SessionToken token = null;

    public Request(Object responseContent, String operationCode, SessionToken token){
        this.content = responseContent;
        this.operationCode = operationCode;
        this.token = token;
    }

    public Request(Users user, Devices device, String operationCode){
        this.user = user;
        this.device = device;
        this.operationCode = operationCode;
    }

    public Request(String operationCode, SessionToken token){
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

    public SessionToken getToken(){ return token; }

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
