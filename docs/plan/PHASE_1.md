# Phase 1: Project Setup and Backend Development (Spring Boot)

This phase focuses on creating the core of your application: the web dashboard and the backend API.

## 1.1. Initialize Your Spring Boot Application

Use the [Spring Initializr](https://start.spring.io/) to generate your project.¹  
Dependencies to include:

- **Spring Web** – for building RESTful APIs and the web dashboard  
- **Spring Data JPA** – to interact with the H2 database  
- **H2 Database** – an in-memory database ideal for development and local deployment²³  
- **Spring Boot DevTools** – for automatic application restarts during development  
- **Spring Boot Actuator** – to monitor and manage your application⁴  

## 1.2. Configure the H2 Database

In `application.properties`, enable the H2 console so you can easily view and manage your database during development:⁵⁶

```properties
spring.h2.console.enabled=true
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

## 1.3. Define Your Data Model

Create JPA entity classes to represent your data.  
At a minimum you’ll likely need an `Article` entity with fields such as:

- `title`  
- `originalUrl`  
- `content`  
- `summary`  
- `publicationDate`  

## 1.4. Build the RESTful API

### Controllers
Implement `@RestController` classes to expose the following endpoints:⁷

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET    | `/api/articles` | Fetch all summarized articles |
| GET    | `/api/articles/{id}` | Fetch a specific article |
| POST   | `/api/articles` | Private endpoint for your Python worker to submit new articles & summaries |

### Services
Create service classes to encapsulate business logic (saving, retrieving, etc.).

## 1.5. Develop the Web Dashboard

- Build a simple, clean UI with static HTML, CSS, and JavaScript.  
- Use **AJAX** or the **Fetch API** to call your Spring Boot endpoints asynchronously and display summarized articles without page reloads.⁷  
- For a more dynamic experience you can adopt a lightweight frontend framework (Vue.js, React), but plain JavaScript is sufficient for a simple dashboard.
```

¹ [Spring Initializr](https://start.spring.io/)  
² [H2 Database Engine](https://www.h2database.com/html/main.html)  
³ [Spring Boot H2 Guide](https://spring.io/guides/gs/accessing-data-jpa/)  
⁴ [Spring Boot Actuator Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)  
⁵ [H2 Console Configuration](https://www.baeldung.com/spring-boot-h2-database)  
⁶ [Spring Data JPA Reference](https://spring.io/projects/spring-data-jpa)  
⁷ [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)