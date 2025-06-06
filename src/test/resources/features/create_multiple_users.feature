@CreateMultipleUsers
Feature: Create Users
  As a system user
  I want create multiple users
  So that I can test the user creation functionality

  Background:
    Given the user service is running

  Scenario Outline: Create a new user successfully
    When I create a user with the following details:
      | first_name  | <first_name>  |
      | last_name   | <last_name>   |
      | email       | <email>       |
      | age         | <age>         |
    Then the response status should be 201
    And the response should match the user schema
    And the response should contain the created user details
    And the user should have a valid ID
    Examples:
      | first_name | last_name | email                  | age |
      | John       | Doe       | john.doe@example.com   | 35  |
      | Jane       | Smith     | jane.smith@example.com | 28  |
      | Michael    | Johnson   | michaelj@example.com   | 42  |
      | Emily      | Davis     | emily.d@example.com    | 23  |
      | David      | Brown     | davidb@example.com     | 50  |