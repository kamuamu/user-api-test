@NegativeUserScenarios
Feature: User Management
  As a system user
  I want to make sure errors are handled correctly when managing users
  So that I can ensure the robustness of the user service

  Background:
    Given the user service is running

  Scenario Outline: Attempt to create user with invalid data
    When I create a user with the following details:
      | first_name | <first_name> |
      | last_name  | <last_name>  |
      | email      | <email>      |
      | age        | <age>        |
    Then the response status should be 400
    And the response should contain "<error>"
    And the response should match the "schemas/error-response-schema.json" schema
    Examples:
      | first_name | last_name | email              | age | error                |
      | Sid        | Selvan    | invalid-email      | 28  | check_email_format   |
      | Sid        | Selvan    | sid.ss@example.com | 200 | check_age_positive   |
      | Sid        | Selvan    | sid.ss@example.com | -5  | check_age_positive   |

    Scenario: Attempt to create user with email that exists already in the system
      When I create a user with the following details:
        | first_name | Sid              |
        | last_name  | Selvan           |
        | email      | sid.ss@gmail.com |
        | age        | 28               |
      Then the response status should be 201
      When I create a user with the following details:
        | first_name | Sara             |
        | last_name  | Sid              |
        | email      | sid.ss@gmail.com |
        | age        | 23               |
      Then the response status should be 409
      And the response should contain "users_email_key"
      And the response should match the "schemas/error-response-schema.json" schema