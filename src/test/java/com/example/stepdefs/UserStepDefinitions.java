package com.example.stepdefs;

import com.example.models.User;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;

public class UserStepDefinitions {

    private RequestSpecification requestSpec;
    private String baseUrl;
    private String apiKey;
    private Response response;

    @Before
    public void setUp() {
        baseUrl = "https://x8ki-letl-twmt.n7.xano.io/api:damFVaMA/";
        // apiKey is given as plain text for demonstration purposes. In a real application, it should be stored securely.
        apiKey = "lUv1CdPhkt5HwmxMHg0ZMJ66PXD2bGmoeQvvz5YVoOH7i9LWHQmOy3FTi3tzrufnsqdDTDltPCzuptHlehtwHYD5MQx7RrRIkFTC91MyqZ3msjEglSEsos63XlwLwQiL";
        requestSpec = given()
                .baseUri(baseUrl)
                .header(new Header("Apikey", apiKey))
                .contentType("application/json")
                .accept("application/json");
    }

    @After
    public void tearDown() {
        // Nothing for now
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
}