# Virtual Workspace - Backend API

Spring Boot 4 modular monolith providing REST APIs for authentication, user profiles, rooms, heartbeat presence tracking, and task management.

## Service URLs

- Base API URL: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI v3 Specification: `http://localhost:8080/v3/api-docs`

## Complete API Endpoints

### Authentication (`/api/v1/auth`)

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | Public | Registers user with username, email, and password; emits `UserRegisteredEvent` |
| POST | `/api/v1/auth/login` | Public | Authenticates credentials; returns JWT access token and refresh token |
| POST | `/api/v1/auth/refresh` | Public | Validates refresh token from body `{ "refreshToken": "..." }` and returns new token pair |
| POST | `/api/v1/auth/logout` | Protected | Revokes provided refresh token in database |
| GET | `/api/v1/auth/user/{userId}/data` | Protected | Retrieves user account metadata |

### User Profile (`/api/v1/profile`)

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| GET | `/api/v1/profile/{userId}` | Protected | Retrieves profile data (bio, gender, full name, profile picture URL) |
| PUT | `/api/v1/profile` | Protected | Updates profile fields for the authenticated user |

### Rooms (`/api/v1/rooms`)

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| GET | `/api/v1/rooms` | Protected | Paginated Slice listing of available rooms (`page`, `size`, `sort`) |
| POST | `/api/v1/rooms` | Protected | Creates a new room (name, description, visibility) |
| GET | `/api/v1/rooms/{id}` | Protected | Retrieves metadata for a specific room |
| PUT | `/api/v1/rooms/{roomId}` | Protected | Updates room details (restricted to room creator) |
| DELETE | `/api/v1/rooms/{roomId}` | Protected | Deletes room; evicts `room_members` cache |
| POST | `/api/v1/rooms/{roomId}/join` | Protected | Adds authenticated user to room membership; evicts `room_members` cache |
| POST | `/api/v1/rooms/{roomId}/heartbeat` | Protected | Updates user presence heartbeat timestamp for active room membership |
| GET | `/api/v1/rooms/{roomId}/members` | Protected | Lists active room members; cached in Redis under `room_members` |

### Favorite Rooms (`/api/v1/rooms/favorites`)

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| GET | `/api/v1/rooms/favorites` | Protected | Paginated Slice listing of user bookmarked rooms (`page`, `size`, `sort`) |
| POST | `/api/v1/rooms/favorites/{roomId}` | Protected | Adds room to user favorites |
| DELETE | `/api/v1/rooms/favorites/{roomId}` | Protected | Removes room from user favorites |

### Tasks (`/api/v1/tasks`)

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| GET | `/api/v1/tasks` | Protected | Paginated Slice listing of tasks belonging to authenticated user |
| POST | `/api/v1/tasks` | Protected | Creates user task (title, description, status) |
| PUT | `/api/v1/tasks/{taskId}` | Protected | Updates user task fields |
| DELETE | `/api/v1/tasks/{taskId}` | Protected | Deletes user task |

## Backend Features and Technical Details

- Stateless Security: Spring Security filter chain with HMAC-SHA256 JWT validation, BCrypt password hashing, and database-backed refresh token rotation with explicit revocation.
- Rate Limiting: Bucket4j token bucket filter backed by Caffeine cache enforcing a 5 requests per minute per IP limit on `/api/v1/auth` routes.
- Distributed Caching: Spring Data Redis cache manager configured with Jackson polymorphic serialization and a 10-minute TTL. Used for caching room member lists with selective eviction on membership changes.
- Heartbeat Presence Management: Room member active status is maintained via client HTTP POST requests to `/api/v1/rooms/{roomId}/heartbeat`. A background task (`RoomMembersStatusScheduler`) runs every 30 seconds via `@Scheduled(fixedRate = 30000)` executing a bulk JPQL query to mark members inactive if their heartbeat is older than 30 seconds. No WebSocket connections are used (But i am working on it).
- Event-Driven Initialization: User registration publishes a `UserRegisteredEvent`, which triggers `ProfileSetupListener` to asynchronously create an associated empty profile entity.
- Paginated Slices: List endpoints return Spring Data `Slice<T>` instead of `Page<T>` to eliminate `COUNT(*)` query overhead.
- Database Migrations: Flyway tracks schema changes across modular migration scripts located in `src/main/resources/db/migration`.
- Global Exception Handling: `@RestControllerAdvice` translates domain exceptions into standardized `ApiResponse<T>` envelopes with HTTP status codes and structured timestamps.

## Backend File Structure

```
backend/
├── pom.xml                                      # Maven dependencies and build configuration
├── Dockerfile                                   # Multi-stage Eclipse Temurin 21 build and JRE runtime
└── src/
    └── main/
        ├── java/app/virtual_workspace/
        │   ├── Application.java                 # Spring Boot application entrypoint
        │   ├── accounts/                        # Accounts, auth, and profile domain
        │   │   ├── controllers/                 # AuthenticationController, ProfileController
        │   │   ├── dtos/                        # Auth, profile, user data DTOs, and UserPrincipal
        │   │   ├── events/                      # UserRegisteredEvent and ProfileSetupListener
        │   │   ├── mappers/                     # MapStruct interfaces: AuthMapper, ProfileMapper, UserMapper
        │   │   ├── models/                      # User, Profile, RefreshToken entities and enums (Role, Gender)
        │   │   ├── repositories/                # UserRepository, ProfileRepository, RefreshTokenRepository
        │   │   └── services/                    # UserAuthService, UserService, ProfileService, RefreshTokenService
        │   ├── config/                          # RedisCacheConfig, JacksonConfig
        │   ├── exceptions/                      # GlobalExceptionHandler, ErrorResponse, custom exceptions
        │   ├── rooms/                           # Room, room membership, and favorite rooms domain
        │   │   ├── controllers/                 # RoomController, RoomMembersController, FavoriteRoomController
        │   │   ├── dtos/                        # Room, room member, and favorite room DTOs
        │   │   ├── mappers/                     # RoomMapper, RoomMembersMapper, FavoriteRoomMapper
        │   │   ├── models/                      # Room, RoomMembers, FavoriteRoom entities and enums (Status, Visibility)
        │   │   ├── repositories/                # RoomRepository, RoomMembersRepository, FavoriteRoomRepository
        │   │   └── services/                    # RoomService, RoomMembersService, FavoriteRoomService, RoomAuthService
        │   ├── scheduling/                      # RoomMembersStatusScheduler (periodic presence check)
        │   ├── security/                        # SecurityConfig, JwtFilter, RateLimitingFilter, JwtService
        │   ├── shared/                          # ApiResponse generic payload wrapper
        │   └── tasks/                           # Task management domain
        │       ├── controllers/                 # TaskController
        │       ├── dtos/                        # CreateTaskDto, UpdateTaskDto, TaskResponseDto
        │       ├── mappers/                     # TaskMapper
        │       ├── models/                      # Task entity
        │       ├── repositories/                # TaskRepository
        │       └── services/                    # TaskService
        └── resources/
            ├── application.properties           # Database, Redis, JWT, and Flyway configuration
            └── db/migration/                    # Flyway migration SQL scripts
                ├── V1__initial_schema.sql
                ├── auth/V4__create_refresh_token_table.sql
                ├── rooms/V5__add_visibility_column-to-rooms.sql
                └── tasks/
                    ├── V2__add_createdat.sql
                    └── V3__add_updatedat.sql
```

## Running the Application

Run locally with the Maven wrapper:

```bash
./mvnw spring-boot:run
```

