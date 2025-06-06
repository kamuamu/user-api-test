package com.example.utils;

import com.example.models.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;

import java.util.List;

public class UserUtils {
    public static User extractFirstUser(ObjectMapper objectMapper, Response response) throws JsonProcessingException {
        List<User> createdUsers = getAllUsers(objectMapper, response);
        return createdUsers.get(0);
    }
    
    public static List<User> getAllUsers(ObjectMapper objectMapper, Response response) throws JsonProcessingException {
        String responseBody = response.getBody().asString();
        List<User> createdUsers = objectMapper.readValue(responseBody, new TypeReference<List<User>>() {});
        return createdUsers;
    }
}
