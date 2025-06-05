# User API Test

A comprehensive BDD (Behavior-Driven Development) test suite for user profile management API using REST Assured.

## Overview

This test suite provides automated testing for a user profile management system with full CRUD operations. It uses a live api server to test API responses and validates user management functionality through readable Gherkin scenarios.

## Technology Stack

- **Java** - Programming language
- **Cucumber** - BDD testing framework
- **JUnit 5** - Test runner platform
- **REST Assured** - API testing library
- **Jackson** - JSON serialization/deserialization
- **AssertJ** - Fluent assertion library
- **Hamcrest** - Matcher library for assertions
- **Lombok** - Java library for reducing boilerplate code

## Project Structure

```
src/
├── main/java
│   └── /com/example/models/
│       └── User.java                       # User model with builder pattern
└── test/java
│   ├── /com/example/runners/
│   │   └── CucumberTestRunner.java         # JUnit 5 test runner
│   └── /com/example/stepdefs/
│       └── UserStepDefinitions.java        # Cucumber step implementations
└── test/resources/
        └── features/
            └── user.feature                # BDD scenarios in Gherkin
```

## Prerequisites

- Java 17 or higher