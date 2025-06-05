package com.example.stepdefs;

import com.example.models.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserStepDefinitions {

    private RequestSpecification requestSpec;
    private String baseUrl;
    private String apiKey;
    private Response response;
    private ObjectMapper objectMapper;
    private User createdUser;

    @Before
    public void setUp() {
        baseUrl = "https://x8ki-letl-twmt.n7.xano.io/api:damFVaMA/";
        // apiKey is given as plain text for demonstration purposes. In a real application, it should be stored securely.
        apiKey = "lUv1CdPhkt5HwmxMHg0ZMJ66PXD2bGmoeQvvz5YVoOH7i9LWHQmOy3FTi3tzrufnsqdDTDltPCzuptHlehtwHYD5MQx7RrRIkFTC91MyqZ3msjEglSEsos63XlwLwQiL";
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        requestSpec = given()
                .baseUri(baseUrl)
                .header(new Header("Apikey", apiKey))
                .contentType("application/json")
                .accept("application/json");
    }

    @After
    public void tearDown() {
        if(createdUser != null) {
            requestSpec.delete(createdUser.getId());
        }
    }

    @Given("the user service is running")
    public void theUserServiceIsRunning() {
        requestSpec.get()
                .then()
                .assertThat()
                .statusCode(200);
    }

    @When("I request all users")
    public void iRequestAllUsers() {
        response = requestSpec.get("user");
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        response.then()
                .assertThat()
                .statusCode(expectedStatus);
    }

    @And("the response should contain users")
    public void theResponseShouldContainUsers() {
        response.then()
                .assertThat()
                .body("size()", greaterThan(0));
    }

    @When("I create a user with the following details:")
    public void iCreateAUserWithTheFollowingDetails(DataTable dataTable) throws JsonProcessingException {
        Map<String, String> userData = dataTable.asMap(String.class, String.class);
        // Create User object using builder pattern
        User user = objectMapper.convertValue(userData, User.class);
        // Convert to JSON using ObjectMapper
        String requestBody = objectMapper.writeValueAsString(user);

        response = requestSpec
                .body(requestBody)
                .post("user");
    }

    @And("the response should contain the created user details")
    public void theResponseShouldContainTheCreatedUserDetails() throws Exception {
        // Parse response as User object
        User user = objectMapper.readValue(response.getBody().asString(), User.class);

        // Validate using the parsed object
        assertNotNull(user, "User should not be null");
        assertNotNull(user.getId(), "User ID should not be null");
        assertNotNull(user.getFirstName(), "First name should not be null");
        assertNotNull(user.getEmail(), "Email should not be null");
        assertNotNull(user.getAge(), "Age should not be null");
        // Store created user for potential cleanup or further validation
        this.createdUser = user;
    }

    @And("the user should have a valid ID")
    public void theUserShouldHaveAValidID() throws Exception {
        assertNotNull(createdUser.getId(), "User ID should match expected value");
    }
}