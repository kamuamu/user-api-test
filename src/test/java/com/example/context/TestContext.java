package com.example.context;

import com.example.models.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Data;

@Data
public class TestContext {
    private RequestSpecification requestSpec;
    private Response response;
    private User currentUser;
    private User existingUser;
    private ObjectMapper objectMapper;
    private String baseUrl;
}
