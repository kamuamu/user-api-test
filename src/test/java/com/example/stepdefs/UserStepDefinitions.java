package com.example.stepdefs;

import com.example.models.User;
import com.example.utils.UserUtils;
import com.example.wiremock.InMemoryUserTransformer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.assertj.core.api.Assertions.assertThat;

public class UserStepDefinitions {

    private RequestSpecification requestSpec;
    private static final String DEFAULT_BASE_URL = "https://mrvndkxjhndplhhjmkbf.supabase.co/rest/v1";
    // apiKey is given as plain text for demonstration purposes. In a real
    // application, it should be stored securely.
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
    private User currentUser;
    private boolean useWireMock;
    private String baseUrl = DEFAULT_BASE_URL;
    private WireMockServer wireMockServer;
    private InMemoryUserTransformer userTransformer;
    private Logger logger = LoggerFactory.getLogger(UserStepDefinitions.class);

    @Before
    public void setUp() throws JsonProcessingException {
        objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        useWireMock = isWireMockEnabled();

        if (useWireMock) {
            startWireMock();
            User seededUser = userTransformer.resetWithExisting(existingUser);
            if (seededUser != null) {
                existingUser = seededUser;
            }
            baseUrl = wireMockServer.baseUrl();
        }

        requestSpec = given()
                .baseUri(baseUrl)
                .header(new Header("apikey", API_KEY))
                .header(new Header("Prefer", "return=representation"))
                .contentType("application/json")
                .accept("application/json");

        if (!useWireMock) {
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
    }

    @After
    public void tearDown() throws JsonProcessingException {
        if (!useWireMock) {
            // Perform cleanup using the batch delete method
            cleanupAllUsers();
        }

        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
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
                    .baseUri(baseUrl)
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

    private boolean isWireMockEnabled() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("USE_WIREMOCK", "false"))
                || Boolean.parseBoolean(System.getProperty("USE_WIREMOCK", "false"));
    }

    private void startWireMock() {
        userTransformer = new InMemoryUserTransformer(objectMapper);
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .dynamicPort()
                .extensions(userTransformer));
        wireMockServer.start();

        wireMockServer.stubFor(any(urlPathMatching("/users.*"))
                .willReturn(aResponse().withTransformers(InMemoryUserTransformer.NAME)));

        wireMockServer.stubFor(any(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(200).withBody("OK")));
    }

    @Given("the user service is running")
    public void theUserServiceIsRunning() {
        requestSpec.get("/")
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

    @And("the response should not be empty")
    public void theResponseShouldNotBeEmpty() {
        response.then()
                .assertThat()
                .body("size()", greaterThan(0));
    }

    @And("the response should be empty")
    public void theResponseShouldBeEmpty() {
        response.then()
                .assertThat()
                .body("size()", equalTo(0));
    }

    @When("I create a user with the following details:")
    public void iCreateAUserWithTheFollowingDetails(DataTable dataTable) throws JsonProcessingException {
        Map<String, String> userData = dataTable.asMap(String.class, String.class);
        // Create User object using Jackson's ObjectMapper
        User user = objectMapper.convertValue(userData, User.class);
        // Convert to JSON using ObjectMapper
        String requestBody = objectMapper.writeValueAsString(user);

        response = requestSpec
                .body(requestBody)
                .post(USER_ENDPOINT);
        // Store created user for potential cleanup or further validation
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            currentUser = UserUtils.extractFirstUser(objectMapper, response);
        }
    }

    @When("I create a user with the following details with an invalid endpoint:")
    public void iCreateAUserWithTheFollowingDetailsWithAnInvalidEndpoint(DataTable dataTable)
            throws JsonProcessingException {
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
            currentUser = UserUtils.extractFirstUser(objectMapper, response);
        }
    }

    @And("the response should match the {string} schema")
    public void theResponseShouldMatchTheUserSchema(String schemaFile) {
        response.then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath(schemaFile));
    }

    @When("I update the user with the following details:")
    public void iUpdateTheUserWithTheFollowingDetails(DataTable dataTable) throws JsonProcessingException {
        Map<String, String> userData = dataTable.asMap(String.class, String.class);
        User updatedUser = objectMapper.convertValue(userData, User.class);
        String requestBody = objectMapper.writeValueAsString(updatedUser);

        response = requestSpec
                .body(requestBody)
                .param("id", "eq." + currentUser.getId())
                .patch(USER_ENDPOINT);
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            this.currentUser = UserUtils.extractFirstUser(objectMapper, response);
        }
    }

    @And("the user's first name should be {string}")
    public void theUsersFirstNameShouldBe(String expectedFirstName) throws Exception {
        assertThat(currentUser.getFirstName()).isEqualTo(expectedFirstName);
    }

    @And("the user's last name should be {string}")
    public void theUsersLastNameShouldBe(String expectedLastName) throws Exception {
        assertThat(currentUser.getLastName()).isEqualTo(expectedLastName);
    }

    @And("the user's email should be {string}")
    public void theUsersEmailShouldBe(String expectedEmail) throws Exception {
        assertThat(currentUser.getEmail()).isEqualTo(expectedEmail);
    }

    @And("the user's age should be {string}")
    public void theUsersAgeShouldBe(String expectedAge) throws Exception {
        assertThat(currentUser.getAge()).isEqualTo(expectedAge);
    }

    @When("I delete the user")
    public void iDeleteTheUser() {
        response = requestSpec.param("id", "eq." + currentUser.getId()).delete(USER_ENDPOINT);
    }

    @And("when I try to retrieve the deleted user")
    public void whenITryToRetrieveTheDeletedUser() {
        response = requestSpec.param("id", "eq." + currentUser.getId()).get(USER_ENDPOINT);
    }

    @And("the response should contain {string}")
    public void theResponseShouldContainError(String errorSubString) {
        response.then()
                .assertThat()
                .body("message", containsString(errorSubString));
    }
}
