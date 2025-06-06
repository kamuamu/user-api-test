@userE2ETest
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

  Scenario Outline: Create a new user successfully
    When I create a user with the following details:
      | first_name  | <first_name>  |
      | last_name   | <last_name>   |
      | email       | <email>       |
      | age         | <age>         |
    Then the response status should be 201
    And the response should contain the created user details
    And the user should have a valid ID
    Examples:
      | first_name  | last_name | email                 | age |
      | John        | Doe       | john.doe@example.com  | 35  |

  Scenario Outline: Update an existing user successfully
    Given I create a user with the following details:
      | first_name  | <first_name>  |
      | last_name   | <last_name>   |
      | email       | <email>       |
      | age         | <age>         |
    When I update the user with the following details:
      | first_name  | Jane            |
      | last_name   | Smith           |
      | email       | jane@smith.com  |
      | age         | 29              |
    Then the response status should be 200
    And the user's ID is unaltered
    And the user's last name should be "Smith"
    And the user's email should be "jane@smith.com"
    And the user's age should be 29
    Examples:
      | first_name  | last_name | email                 | age |
      | Jane        | Johnson   | jane@johnson.com      | 28  |

  Scenario Outline: Delete an existing user successfully
    Given I create a user with the following details:
      | first_name  | <first_name>  |
      | last_name   | <last_name>   |
      | email       | <email>       |
      | age         | <age>         |
    When I delete the user
    Then the response status should be 200
    And when I try to retrieve the deleted user
    Then the response should not contain users
    Examples:
      | first_name  | last_name | email                 | age |
      | Jane        | Johnson   | jane@johnson.com      | 28  |