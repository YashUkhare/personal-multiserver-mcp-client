package com.mcp.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    private String jsonrpc = "2.0";
    private Object id;
    private Object result;
    private JsonRpcError error;

    @Data
    @NoArgsConstructor
    public static class JsonRpcError {
        private int code;
        private String message;
        private Object data;
    }
}
