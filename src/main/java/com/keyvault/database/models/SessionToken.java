package com.keyvault.database.models;

import java.io.Serial;
import java.io.Serializable;

public class SessionToken implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757692L;
    private String value;
    private Users usersByIdTu;

    public SessionToken(String value, Users usersByIdTu) {
        this.value = value;
        this.usersByIdTu = usersByIdTu;
    }

    public Users getUser() {
        return usersByIdTu;
    }

    public void setUser(Users usersByIdTu) {
        this.usersByIdTu = usersByIdTu;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}