@UserE2ETest
Feature: User Management
  As a system user
  I want to manage user profiles
  So that I can maintain user information effectively

  Background:
    Given the user service is running

  Scenario: Get all users
    When I request all users
    Then the response status should be 200
    And the response should contain users
    And the response should match the user schema

  Scenario: Create a new user successfully
    When I create a user with the following details:
      | first_name | John                 |
      | last_name  | Doe                  |
      | email      | john.doe@example.com |
      | age        | 35                   |
    Then the response status should be 201
    And the response should match the user schema
    And the response should contain the created user details
    And the user should have a valid ID

  Scenario: Update an existing user successfully
    Given I create a user with the following details:
      | first_name  | Jane                  |
      | last_name   | Doe                   |
      | email       | john.doe@example.com  |
      | age         | 35                    |
    When I update the user with the following details:
      | first_name | Jane           |
      | last_name  | Smith          |
      | email      | jane@smith.com |
      | age        | 28             |
    Then the response status should be 200
    And the response should match the user schema
    And the user's ID is unaltered
    And the user's last name should be "Smith"
    And the user's email should be "jane@smith.com"
    And the user's age should be 28

  Scenario: Delete an existing user successfully
    Given I create a user with the following details:
      | first_name | Jane             |
      | last_name  | Johnson          |
      | email      | jane@johnson.com |
      | age        | 28               |
    When I delete the user
    Then the response status should be 200
    And when I try to retrieve the deleted user
    Then the response should not contain users