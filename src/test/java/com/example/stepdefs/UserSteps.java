package com.example.stepdefs;

import com.example.context.TestContext;
import com.example.models.User;
import com.example.utils.UserUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.Map;

public class UserSteps {

    private final TestContext context;
    private static final String USER_ENDPOINT = "/users";

    public UserSteps(TestContext context) {
        this.context = context;
    }

    @When("I request all users")
    public void iRequestAllUsers() {
        Response response = context.getRequestSpec().get(USER_ENDPOINT);
        context.setResponse(response);
    }

    @When("I create a user with the following details:")
    public void iCreateAUserWithTheFollowingDetails(DataTable dataTable) throws JsonProcessingException {
        Map<String, String> userData = dataTable.asMap(String.class, String.class);
        ObjectMapper objectMapper = context.getObjectMapper();

        User user = objectMapper.convertValue(userData, User.class);
        String requestBody = objectMapper.writeValueAsString(user);

        Response response = context.getRequestSpec()
                .body(requestBody)
                .post(USER_ENDPOINT);
        context.setResponse(response);

        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            context.setCurrentUser(UserUtils.extractFirstUser(objectMapper, response));
        }
    }

    @When("I create a user with the following details with an invalid endpoint:")
    public void iCreateAUserWithTheFollowingDetailsWithAnInvalidEndpoint(DataTable dataTable)
            throws JsonProcessingException {
        Map<String, String> userData = dataTable.asMap(String.class, String.class);
        ObjectMapper objectMapper = context.getObjectMapper();

        User user = objectMapper.convertValue(userData, User.class);
        String requestBody = objectMapper.writeValueAsString(user);

        Response response = context.getRequestSpec()
                .body(requestBody)
                .post(USER_ENDPOINT + "/invalid");
        context.setResponse(response);

        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            context.setCurrentUser(UserUtils.extractFirstUser(objectMapper, response));
        }
    }

    @When("I update the user with the following details:")
    public void iUpdateTheUserWithTheFollowingDetails(DataTable dataTable) throws JsonProcessingException {
        Map<String, String> userData = dataTable.asMap(String.class, String.class);
        ObjectMapper objectMapper = context.getObjectMapper();

        User updatedUser = objectMapper.convertValue(userData, User.class);
        String requestBody = objectMapper.writeValueAsString(updatedUser);

        Response response = context.getRequestSpec()
                .body(requestBody)
                .param("id", "eq." + context.getCurrentUser().getId())
                .patch(USER_ENDPOINT);
        context.setResponse(response);

        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            context.setCurrentUser(UserUtils.extractFirstUser(objectMapper, response));
        }
    }

    @When("I delete the user")
    public void iDeleteTheUser() {
        Response response = context.getRequestSpec()
                .param("id", "eq." + context.getCurrentUser().getId())
                .delete(USER_ENDPOINT);
        context.setResponse(response);
    }

    @When("when I try to retrieve the deleted user")
    public void whenITryToRetrieveTheDeletedUser() {
        Response response = context.getRequestSpec()
                .param("id", "eq." + context.getCurrentUser().getId())
                .get(USER_ENDPOINT);
        context.setResponse(response);
    }
}
