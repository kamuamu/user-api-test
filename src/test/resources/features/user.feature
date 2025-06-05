@UserProfile
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

  Scenario: Create a new user successfully
    When I create a user with the following details:
      | first_name  | John          |
      | last_name   | Doe           |
      | email       | john@doe.com  |
      | age         | 35            |
    Then the response status should be 200
    And the response should contain the created user details
    And the user should have a valid ID