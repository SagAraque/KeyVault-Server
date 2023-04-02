package com.keyvault;

import java.io.Serial;
import java.io.Serializable;

public class Response implements Serializable {
    @Serial
    private static final long serialVersionUID = 6529685098267757631L;
    private final int responseCode;
    private Object[] responseContent;

    public Response(int responseCode, Object responseContent) {
        this.responseCode = responseCode;
        this.responseContent = new Object[]{responseContent};

    }

    public Response(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public Object[] getResponseContent() {
        return responseContent;
    }
}
