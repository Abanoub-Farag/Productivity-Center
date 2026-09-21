# Virtual Workspace (Productivity Center)

A full-stack monorepo application for virtual study and work spaces featuring heartbeat-based presence tracking, customizable rooms, task management, and session timers.

## Application URLs

- Frontend Application: `http://localhost:4200`
- Backend API Base: `http://localhost:8080/api/v1`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`
- OpenAPI Specification: `http://localhost:8080/v3/api-docs`
- Remote Dev API Tunnel: `https://vocalist-virtual-luckiness.ngrok-free.dev`

## Architecture and Core Features

- Backend: Java 21, Spring Boot 4.0.6, Spring Security with stateless JWT authentication, PostgreSQL, Redis distributed cache, Bucket4j rate limiting, Flyway database migrations, MapStruct compile-time DTO mappers.
- Frontend: Angular 22 with standalone components, SSR (Server-Side Rendering) and client hydration, reactive state facades, Lucide icons, custom SCSS/CSS design tokens (no external CSS frameworks).
- Authentication and Security: Stateless JWT token issuance and validation, refresh token rotation with database revocation, BCrypt password hashing, and per-IP rate limiting on authentication routes.
- User Profiles: Automatic profile initialization via Spring domain events upon registration, profile fetching, and profile metadata updates.
- Virtual Rooms: Public and private room creation, slice-based pagination, room metadata updates, room deletion, and personal favorite room bookmarking.
- Heartbeat Presence System: Client HTTP heartbeat signaling with a background scheduler disconnecting inactive participants after 30 seconds of inactivity, backed by Redis caching.
- Focus and Task Tools: Integrated room timer/stopwatch and user-scoped task management CRUD.

## Monorepo File Structure

```
Productivity-Center/
├── backend/                   # Spring Boot modular monolith backend
│   ├── src/main/java/app/virtual_workspace/
│   │   ├── accounts/          # Authentication, user entities, and profile domain
│   │   ├── config/            # Redis cache and Jackson JSON configurations
│   │   ├── exceptions/        # Centralized REST exception handler and error responses
│   │   ├── rooms/             # Room lifecycle, favorites, and room membership domain
│   │   ├── scheduling/        # Background scheduled presence eviction
│   │   ├── security/          # JWT filter, rate limiting filter, and security rules
│   │   ├── shared/            # Common API response structures
│   │   └── tasks/             # Task management domain
│   ├── src/main/resources/    # Application properties and Flyway SQL migrations
│   ├── Dockerfile             # Multi-stage container build for backend
│   ├── pom.xml                # Maven dependencies and build configuration
│   └── README.md              # Backend-specific architecture and API documentation
├── frontend/                  # Angular SSR web application
│   ├── src/app/
│   │   ├── core/              # Global interceptors, guards, toast/auth services, models
│   │   └── features/          # Domain feature components: auth, profile, rooms, not-found
│   ├── src/environments/      # Environment-specific API configuration
│   ├── dev-proxy.conf.json    # Proxy config for remote ngrok tunnel
│   ├── local-proxy.conf.json  # Proxy config for localhost backend
│   ├── proxy.conf.json        # Proxy config for Docker Compose backend service
│   ├── Dockerfile             # Container build for frontend client
│   ├── package.json           # Frontend dependencies and npm scripts
│   └── README.md              # Frontend-specific architecture and route documentation
├── docker-compose-dev.yml     # Multi-container local environment (db, redis, backend, frontend)
└── README.md                  # Project overview documentation
```

## Subproject Documentation

- Backend details, configurations, and API endpoints: [backend/README.md](backend/README.md)
- Frontend details, routing, and component architecture: [frontend/README.md](frontend/README.md)

## Multi-Container Setup

Launch the stack using Docker Compose:

```bash
docker compose -f docker-compose-dev.yml up --build
```

