Feature: Hello World

  Scenario: The happy path where I get hello world
    When I make a GET request to "/hello"
    Then the response status code should be 200
    And the response body should be "Hello World"