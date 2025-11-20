package com.example.stepdefs;

import com.example.context.TestContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

public class VerificationSteps {

    private final TestContext context;

    public VerificationSteps(TestContext context) {
        this.context = context;
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        context.getResponse().then()
                .assertThat()
                .statusCode(expectedStatus);
    }

    @And("the response should not be empty")
    public void theResponseShouldNotBeEmpty() {
        context.getResponse().then()
                .assertThat()
                .body("size()", greaterThan(0));
    }

    @And("the response should be empty")
    public void theResponseShouldBeEmpty() {
        context.getResponse().then()
                .assertThat()
                .body("size()", equalTo(0));
    }

    @And("the response should match the {string} schema")
    public void theResponseShouldMatchTheUserSchema(String schemaFile) {
        context.getResponse().then()
                .assertThat()
                .body(matchesJsonSchemaInClasspath(schemaFile));
    }

    @And("the user's first name should be {string}")
    public void theUsersFirstNameShouldBe(String expectedFirstName) {
        assertThat(context.getCurrentUser().getFirstName()).isEqualTo(expectedFirstName);
    }

    @And("the user's last name should be {string}")
    public void theUsersLastNameShouldBe(String expectedLastName) {
        assertThat(context.getCurrentUser().getLastName()).isEqualTo(expectedLastName);
    }

    @And("the user's email should be {string}")
    public void theUsersEmailShouldBe(String expectedEmail) {
        assertThat(context.getCurrentUser().getEmail()).isEqualTo(expectedEmail);
    }

    @And("the user's age should be {string}")
    public void theUsersAgeShouldBe(String expectedAge) {
        assertThat(context.getCurrentUser().getAge()).isEqualTo(expectedAge);
    }

    @And("the response should contain {string}")
    public void theResponseShouldContainError(String errorSubString) {
        context.getResponse().then()
                .assertThat()
                .body("message", containsString(errorSubString));
    }
}
