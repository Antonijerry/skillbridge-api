# Declaration (Week 3)

I, Anthony Jerry Kalu (repository: ANTHONY JERRY K.), declare the following about the SkillBridge API project submitted for BuildLab Stage 1 (Backend Development).

## Own work
I designed the idea, chose the scope and the stack, made the design decisions recorded in `docs/design-decisions.md`, ran and debugged every part of the project on my own machine, and wrote the technical reflection myself. The idea is my own and not a tutorial project with the names changed.

## Libraries, frameworks and boilerplate
- **Starter:** the project skeleton (the `pom.xml` layout, the main application class and the Maven wrapper) was generated with Spring Initializr and then changed by me.
- **Runtime:** Java 21, Spring Boot 4.0.8 (web MVC, Data JPA with Hibernate, Validation, Security with OAuth2 resource server and Nimbus JOSE for JWT), Flyway with its PostgreSQL module, the PostgreSQL JDBC driver.
- **Testing:** JUnit 5, Mockito, AssertJ, JsonPath and MockMvc (through the Spring Boot test starters), Spring Security test support, Testcontainers (JUnit and PostgreSQL modules), the JaCoCo Maven plugin.
- **Infrastructure:** Docker and Docker Compose; the `postgres:16` image.
- **Tools used while working:** Postman, PowerShell, IntelliJ IDEA, Git and GitHub.
- No code was copied from a tutorial or another project's repository. Algorithms and patterns (BCrypt password storage, JWT bearer tokens, fixed-window rate limiting, offset pagination) are standard techniques that I applied myself and that are explained in my documents.

## Material AI use
I used AI tools throughout this project, and I consider that use material.
- **Claude (Anthropic)** was my main tool, used as a mentor and pair programmer. It proposed the milestones and design, and wrote the first drafts of much of the Java code, the SQL migrations, the tests, the check scripts and the documents. I created and ran everything, pasted the real output for every milestone, debugged failures, decided what to keep or change, and checked each statement in the documents against my own results. Its corrections and mistakes are recorded in `docs/ai-usage-log.md`.
- **ChatGPT and Gemini** were used for: *(write this in your own words, as in your Week 2 declaration)*.
- I did not paste secrets, real passwords or real personal data into any tool. All data in the project is dummy data.
- The technical reflection (`docs/technical-reflection.md`) is in my own words.

Running record: `docs/ai-usage-log.md`.

Signed: Anthony Jerry Kalu, ______ October/November 2026