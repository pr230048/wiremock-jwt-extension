package com.github.masonm.wiremock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.tomakehurst.wiremock.common.Json;
import com.google.common.base.Splitter;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

public class Jwt {
    private final JsonNode header;
    private final JsonNode payload;

    public Jwt(String token) {
        List<String> parts = Splitter.on(".").splitToList(token);
        if (parts.size() != 3) {
            this.header  = MissingNode.getInstance();
            this.payload = MissingNode.getInstance();
        } else {
            this.header = parsePart(parts.get(0));
            this.payload = parsePart(parts.get(1));
        }
    }

    private JsonNode parsePart(String part) {
        byte[] decodedJwtBody = Base64.getDecoder().decode(part);
        if (decodedJwtBody == null) {
            return null;
        }

        try {
            ObjectMapper mapper = Json.getObjectMapper();
            return mapper.readValue(decodedJwtBody, JsonNode.class);
        } catch (IOException ioe) {
            return null;
        }
    }

    public JsonNode getPayload() {
        return payload;
    }

    public JsonNode getHeader() {
        return header;
    }
}
