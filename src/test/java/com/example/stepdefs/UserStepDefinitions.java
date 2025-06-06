package com.example.stepdefs;

import com.example.models.User;
import com.example.utils.UserUtils;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UserStepDefinitions {

    private RequestSpecification requestSpec;
    private static final String BASE_URL = "https://mrvndkxjhndplhhjmkbf.supabase.co/rest/v1";
    // apiKey is given as plain text for demonstration purposes. In a real application, it should be stored securely.
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1ydm5ka3hqaG5kcGxoaGpta2JmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDkxNDMxMDcsImV4cCI6MjA2NDcxOTEwN30.1g03KgbnmXOwjPdeT72QRlUBQWwnald5aD4lSqkKAw0";
    private static final String USER_ENDPOINT = "/users";
    private Response response;
    private ObjectMapper objectMapper;
    private User existingUser = User.builder()
            .email("kamu@belyf.com")
            .firstName("Kamatchi")
            .lastName("Manickam")
            .age("29")
            .build();
    private User createdUser;
    private User updatedUser;
    private Logger logger = LoggerFactory.getLogger(UserStepDefinitions.class);

    @Before
    public void setUp() throws JsonProcessingException {
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        requestSpec = given()
                .baseUri(BASE_URL)
                .header(new Header("apikey", API_KEY))
                .header(new Header("Prefer", "return=representation"))
                .contentType("application/json")
                .accept("application/json");
        cleanupAllUsers();
        // Create an existing user
        String requestBody = objectMapper.writeValueAsString(existingUser);
        response = requestSpec
                .body(requestBody)
                .post(USER_ENDPOINT);
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            existingUser = UserUtils.extractFirstUser(objectMapper, response);
        }
    }

    @After
    public void tearDown() throws JsonProcessingException {
        // Perform cleanup using the batch delete method
        cleanupAllUsers();
    }

    // Helper method to clean up all users
    private void cleanupAllUsers() throws JsonProcessingException {
        // Fetch all current users in the table
        Response allUsersResponse = requestSpec.get(USER_ENDPOINT);
        List<User> allUsers = UserUtils.getAllUsers(objectMapper, allUsersResponse);

        // Collect all user IDs into a List<String>
        List<String> idsToDelete = allUsers.stream()
                .map(User::getId)
                .filter(StringUtils::isNotBlank) // Ensure ID is not null or empty
                .collect(Collectors.toList());

        if (!idsToDelete.isEmpty()) {
            // Construct the IN filter for the DELETE request
            // Example: id=in.(id1,id2,id3)
            String idFilter = "in.(" + String.join(",", idsToDelete) + ")";
            logger.info("Cleaning up users with IDs: {}", idsToDelete);

            // Send a single DELETE request with the 'in' filter
            // IMPORTANT: Create a NEW RequestSpecification instance for cleanup
            // This ensures that any parameters or headers added for cleanup
            // do NOT affect the 'requestSpec' used by actual test steps.
            RequestSpecification cleanupRequestSpec = given()
                    .baseUri(BASE_URL)
                    .header(new Header("apikey", API_KEY))
                    .header(new Header("Prefer", "return=minimal")) // Only for cleanup, don't return data
                    .contentType("application/json")
                    .accept("application/json");
            cleanupRequestSpec
                    .header(new Header("Prefer", "return=minimal")) // Don't need representation for cleanup
                    .param("id", idFilter)
                    .delete(USER_ENDPOINT)
                    .then()
                    .statusCode(204);
        } else {
            logger.info("No users found to delete during cleanup.");
        }
    }


    @Given("the user service is running")
    public void theUserServiceIsRunning() {
        requestSpec.get(USER_ENDPOINT)
                .then()
                .assertThat()
                .statusCode(200);
    }

    @When("I request all users")
    public void iRequestAllUsers() {
        response = requestSpec.get(USER_ENDPOINT);
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

    @And("the response should not contain users")
    public void theResponseShouldNotContainUsers() {
        response.then()
                .assertThat()
                .body("size()", equalTo(0));
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
                .post(USER_ENDPOINT);
        // Store created user for potential cleanup or further validation
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            createdUser = UserUtils.extractFirstUser(objectMapper, response);
        }
    }

    @When("I create a user with the following details with an invalid endpoint:")
    public void iCreateAUserWithTheFollowingDetailsWithAnInvalidEndpoint(DataTable dataTable) throws JsonProcessingException {
        Map<String, String> userData = dataTable.asMap(String.class, String.class);
        // Create User object using builder pattern
        User user = objectMapper.convertValue(userData, User.class);
        // Convert to JSON using ObjectMapper
        String requestBody = objectMapper.writeValueAsString(user);

        response = requestSpec
                .body(requestBody)
                .post(USER_ENDPOINT + "/invalid"); // Intentionally using an invalid endpoint
        // Store created user for potential cleanup or further validation
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            createdUser = UserUtils.extractFirstUser(objectMapper, response);
        }
    }

    @And("the response should match the {string} schema")
    public void theResponseShouldMatchTheUserSchema(String schemaFile) {
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath(schemaFile));
    }

    @And("the response should contain the created user details")
    public void theResponseShouldContainTheCreatedUserDetails() throws Exception {
        // Validate using the parsed object
        assertNotNull(createdUser, "User should not be null");
        assertNotNull(createdUser.getId(), "User ID should not be null");
        assertNotNull(createdUser.getFirstName(), "First name should not be null");
        assertNotNull(createdUser.getEmail(), "Email should not be null");
        assertNotNull(createdUser.getAge(), "Age should not be null");

    }

    @And("the user should have a valid ID")
    public void theUserShouldHaveAValidID() throws Exception {
        assertNotNull(createdUser.getId(), "User ID should match expected value");
        assertFalse(createdUser.getId().isEmpty(), "User ID should not be empty");
    }

    @When("I update the user with the following details:")
    public void iUpdateTheUserWithTheFollowingDetails(DataTable dataTable) throws JsonProcessingException {
        Map<String, String> userData = dataTable.asMap(String.class, String.class);
        User updatedUser = objectMapper.convertValue(userData, User.class);
        String requestBody = objectMapper.writeValueAsString(updatedUser);

        response = requestSpec
                .body(requestBody)
                .param("id", "eq." + createdUser.getId())
                .patch(USER_ENDPOINT);
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            this.updatedUser = UserUtils.extractFirstUser(objectMapper, response);
        }
    }


    @And("the user's ID is unaltered")
    public void theUsersIDisUnaltered() throws Exception {
        assertEquals(createdUser.getId(), updatedUser.getId());
    }

    @And("the user's first name should be {string}")
    public void theUsersFirstNameShouldBe(String expectedFirstName) throws Exception {
        assertEquals(expectedFirstName, updatedUser.getFirstName());
    }

    @And("the user's last name should be {string}")
    public void theUsersLastNameShouldBe(String expectedLastName) throws Exception {
        assertEquals(expectedLastName, updatedUser.getLastName());
    }

    @And("the user's email should be {string}")
    public void theUsersEmailShouldBe(String expectedEmail) throws Exception {
        assertEquals(expectedEmail, updatedUser.getEmail());
    }

    @And("the user's age should be {string}")
    public void theUsersAgeShouldBe(String expectedAge) throws Exception {
        assertEquals(expectedAge, updatedUser.getAge());
    }

    @When("I delete the user")
    public void iDeleteTheUser() {
        response = requestSpec.param("id", "eq." + createdUser.getId()).delete(USER_ENDPOINT);
    }

    @And("when I try to retrieve the deleted user")
    public void whenITryToRetrieveTheDeletedUser() {
        response = requestSpec.param("id", "eq." + createdUser.getId()).get(USER_ENDPOINT);
    }

    @And("the response should contain {string}")
    public void theResponseShouldContainError(String errorSubString) {
        response.then()
                .assertThat()
                .body("message", containsString(errorSubString));
    }
}