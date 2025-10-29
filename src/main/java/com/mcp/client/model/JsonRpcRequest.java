package com.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcRequest {
    private String jsonrpc = "2.0";
    private Object id;
    private String method;
    private Object params;

    public JsonRpcRequest(Object id, String method, Object params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }
}