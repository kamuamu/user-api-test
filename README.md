
![Tests](https://github.com/kamuamu/user-api-test/actions/workflows/tests.yml/badge.svg)

# User API Test

A comprehensive BDD (Behavior-Driven Development) test suite for user management API using REST Assured.

---

## Overview

This test suite provides automated testing for a user management system with full CRUD operations. It uses a live api server to test API responses and validates user management functionality through readable Gherkin scenarios.

---

## Technology Stack

- **Java** - Programming language
- **Cucumber** - BDD testing framework
- **JUnit 5** - Test runner platform
- **REST Assured** - API testing library
- **json-schema-validator**: Ensures API responses conform to predefined JSON schema contracts
- **Jackson** - JSON serialization/deserialization
- **Hamcrest** - Matcher library for assertions
- **Lombok** - Java library for reducing boilerplate code
- **SLF4J/Logback**: Provides robust and configurable logging for test execution.

---

## API Definition

The tests interact with the following live API endpoint:

`https://preview--friendly-user-profiles-api.lovable.app/`

---

## Project Structure

```
src/
├── main/java
│   └── com/example/models/
│       └── User.java                           # User model with builder pattern
│   └── com/example/utils/
│       └── UserUtils.java                      # Utility methods for user extraction
└── test/java
│   ├── com/example/runners/
│   │   └── CucumberTestRunner.java             # JUnit 5 test runner
│   └── com/example/stepdefs/
│       └── UserStepDefinitions.java            # Cucumber step implementations
└── test/resources/
├── features/
│   └── user.feature                            # E2E test scenarios for user management
│   └── negative_user_scenarios.feature         # Example for negative test cases
│   └── create_multiple_users.feature           # Example for creating multiple users
└── schemas/
├── user-schema.json                            # JSON schema for a single user object
├── users-list-schema.json                      # JSON schema for a list of user objects
└── error-response-schema.json                  # JSON schema for API error responses
```

---

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

---

## Setup Instructions

1.  **Clone the repository:**
    ```bash
    git clone git@github.com:kamuamu/user-api-test.git
    ```
2.  **Build the project:**
    Navigate to the project's root directory and build it using Maven. This command downloads all necessary dependencies and compiles the code.
    ```bash
    cd user-api-test
    mvn clean install
    ```
3.  **Running the Tests:**
    To execute the entire test suite, simply run the following Maven command from the project's root directory:
    ```bash
    mvn clean test   
    ```
