package com.yoanesber.order_payment_rabbitmq.util;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
 * HelperUtil.java
 * This utility class provides methods to convert various objects to a Map<String, Object> representation.
 */

public class HelperUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static Map<String, Object> convertToMap(Object entity) throws IllegalArgumentException {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        if (entity instanceof String) {
            throw new IllegalArgumentException("Entity cannot be a String");
        }
        if (entity instanceof Number) {
            throw new IllegalArgumentException("Entity cannot be a Number");
        }
        if (entity instanceof Boolean) {
            throw new IllegalArgumentException("Entity cannot be a Boolean");
        }
        
        try {
            return objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to convert entity to Map", e);
        }
    }

    public static Map<String, Object> convertToMap(byte[] entity) throws IllegalArgumentException, IOException {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        try {
            return objectMapper.readValue(entity, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to convert entity to Map", e);
        } catch (IOException e) {
            throw new IOException("Failed to convert entity to Map", e);
        }
    }

    public static String convertToString(byte[] entity) throws IllegalArgumentException, IOException {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failed to convert entity to String", e);
        } catch (IOException e) {
            throw new IOException("Failed to convert entity to String", e);
        }
    }
}