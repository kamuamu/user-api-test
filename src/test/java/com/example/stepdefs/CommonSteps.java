package com.example.stepdefs;

import com.example.context.TestContext;
import com.example.models.User;
import com.example.utils.UserUtils;
import com.example.wiremock.InMemoryUserTransformer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.RestAssured.given;

public class CommonSteps {

    private final TestContext context;
    private static final String DEFAULT_BASE_URL = "https://mrvndkxjhndplhhjmkbf.supabase.co/rest/v1";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im1ydm5ka3hqaG5kcGxoaGpta2JmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDkxNDMxMDcsImV4cCI6MjA2NDcxOTEwN30.1g03KgbnmXOwjPdeT72QRlUBQWwnald5aD4lSqkKAw0";
    private static final String USER_ENDPOINT = "/users";
    private WireMockServer wireMockServer;
    private InMemoryUserTransformer userTransformer;
    private boolean useWireMock;
    private Logger logger = LoggerFactory.getLogger(CommonSteps.class);

    public CommonSteps(TestContext context) {
        this.context = context;
    }

    @Before
    public void setUp() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        context.setObjectMapper(objectMapper);

        useWireMock = isWireMockEnabled();

        String baseUrl = DEFAULT_BASE_URL;
        User existingUser = User.builder()
                .email("kamu@belyf.com")
                .firstName("Kamatchi")
                .lastName("Manickam")
                .age("29")
                .build();

        if (useWireMock) {
            startWireMock();
            User seededUser = userTransformer.resetWithExisting(existingUser);
            if (seededUser != null) {
                existingUser = seededUser;
            }
            baseUrl = wireMockServer.baseUrl();
        }
        context.setBaseUrl(baseUrl);
        context.setExistingUser(existingUser);

        RequestSpecification requestSpec = given()
                .baseUri(baseUrl)
                .header(new Header("apikey", API_KEY))
                .header(new Header("Prefer", "return=representation"))
                .contentType("application/json")
                .accept("application/json");
        context.setRequestSpec(requestSpec);

        if (!useWireMock) {
            cleanupAllUsers();
            // Create an existing user
            String requestBody = objectMapper.writeValueAsString(existingUser);
            Response response = requestSpec
                    .body(requestBody)
                    .post(USER_ENDPOINT);
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                context.setExistingUser(UserUtils.extractFirstUser(objectMapper, response));
            }
        }
    }

    @After
    public void tearDown() throws JsonProcessingException {
        if (!useWireMock) {
            cleanupAllUsers();
        }

        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @Given("the user service is running")
    public void theUserServiceIsRunning() {
        context.getRequestSpec().get("/")
                .then()
                .assertThat()
                .statusCode(200);
    }

    private void cleanupAllUsers() throws JsonProcessingException {
        RequestSpecification requestSpec = context.getRequestSpec();
        ObjectMapper objectMapper = context.getObjectMapper();

        // Fetch all current users in the table
        Response allUsersResponse = requestSpec.get(USER_ENDPOINT);
        List<User> allUsers = UserUtils.getAllUsers(objectMapper, allUsersResponse);

        // Collect all user IDs into a List<String>
        List<String> idsToDelete = allUsers.stream()
                .map(User::getId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        if (!idsToDelete.isEmpty()) {
            String idFilter = "in.(" + String.join(",", idsToDelete) + ")";
            logger.info("Cleaning up users with IDs: {}", idsToDelete);

            RequestSpecification cleanupRequestSpec = given()
                    .baseUri(context.getBaseUrl())
                    .header(new Header("apikey", API_KEY))
                    .header(new Header("Prefer", "return=minimal"))
                    .contentType("application/json")
                    .accept("application/json");
            cleanupRequestSpec
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
        userTransformer = new InMemoryUserTransformer(context.getObjectMapper());
        wireMockServer = new WireMockServer(WireMockConfiguration.options()
                .dynamicPort()
                .extensions(userTransformer));
        wireMockServer.start();

        wireMockServer.stubFor(any(urlPathMatching("/users.*"))
                .willReturn(aResponse().withTransformers(InMemoryUserTransformer.NAME)));

        wireMockServer.stubFor(any(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(200).withBody("OK")));
    }
}
