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