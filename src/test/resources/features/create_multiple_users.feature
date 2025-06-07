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
    And the response should match the "schemas/user-list-schema.json" schema
    And the user's first name should be "<first_name>"
    And the user's last name should be "<last_name>"
    And the user's email should be "<email>"
    And the user's age should be "<age>"
    Examples:
      | first_name | last_name | email                | age |
      | Karthik    | Rajan     | karthikr@example.com | 35  |
      | Priya      | Selvi     | priyas@example.com   | 28  |
      | Anand      | Kumar     | anandk@example.com   | 42  |
      | Meena      | Devi      | meenad@example.com   | 23  |
      | Suresh     | Pandian   | sureshp@example.com  | 50  |