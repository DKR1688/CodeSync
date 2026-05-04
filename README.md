# CodeSync -- Progress Log
> This log documents the progress of tasks completed for the CodeSync backend project, as recorded in the repository’s README. Tasks are grouped by week with thematic headings, detailing the work done.

> **📂 Current Structure of GitHub :-**
```text
CodeSync/
│
├── api-gateway/
│
├── auth-service/
│
├── collab-service/
│
├── comment-service/
│
├── eureka-server/
│
├── execution-service/
│
├── file-service/
│
├── notification-service/
│
├── project-service/
│
├── version-service/
│
+-- README.md
```

## 🚀 Week 01 - Backend Microservices Foundation
> **🗓️ 18-April-2026 :-** Started CodeSync backend development by adding auth-service for login and register flow to establish the user authentication foundation of the platform [Click Here](./auth-service);

> **🗓️ 19-April-2026 :-** Added project or repository service structure, improved auth-service test coverage and integrated OAuth2 login support for GitHub and Google with environment-variable based configuration [Click Here](./project-service) and [Click Here](./auth-service);

> **🗓️ 20-April-2026 :-** Expanded backend architecture by adding file or editor service, Eureka server, API gateway with CORS support, JWT-aligned project-service authentication, service clients and supporting JUnit work across key backend modules [Click Here](./file-service), [Click Here](./eureka-server) and [Click Here](./api-gateway);

> **🗓️ 21-April-2026 :-** Added live collaboration service implementation and version or snapshot service support for real-time coding sessions and file history management [Click Here](./collab-service) and [Click Here](./version-service);

> **🗓️ 22-April-2026 :-** Added comment or code review service and notification service; refactored client handling, properties, exception flow and snapshot entity behavior to improve backend service communication [Click Here](./comment-service) and [Click Here](./notification-service);

## 🚀 Week 02 - Backend Service Expansion and Refactoring
> **🗓️ 23-April-2026 :-** Added execution service and supporting Maven structure, expanded inter-service clients for project and collaboration flows, improved JUnit coverage and merged major backend implementation updates into the main CodeSync backend flow [Click Here](./execution-service);

> **🗓️ 24-April-2026 :-** Refactored security configuration, project permission clients and service test setup to improve backend authorization, permission handling and verification reliability [Click Here](./auth-service) and [Click Here](./project-service);

> **🗓️ 25-April-2026 :-** Refactored backend services for cleaner structure, improved service communication flow and better maintainability across the microservice repository [Click Here](./api-gateway);

> **🗓️ 26-April-2026 :-** Completed the initial backend delivery window with the full CodeSync service set prepared for frontend connectivity, end-to-end feature verification and later integration refinement;

## 🚀 Week 03
> **🗓️ 04-May-2026 :-** Started SonarQube Cloud onboarding planning for the backend repository using GitHub Actions based analysis and prepared the service-wise quality tracking direction for the CodeSync microservices architecture;
