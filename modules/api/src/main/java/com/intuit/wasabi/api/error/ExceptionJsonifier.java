package com.intuit.wasabi.api.error;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.core.Response;

public class ExceptionJsonifier {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // FIXME: test
    public String serialize(final Response.Status status, final String message) {
        return objectMapper.createObjectNode()
                .set("error", objectMapper.createObjectNode()
                        .put("code", status.getStatusCode())
                        .put("message", message))
//                        .set("errors", objectMapper.createArrayNode()
//                                .addObject().put("message", message)))
                .toString();
    }
}
