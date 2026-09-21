# Virtual Workspace - Frontend Application

Angular 22 web application featuring Server-Side Rendering (SSR), standalone components, reactive signals, custom SCSS/CSS design tokens, and HTTP-based heartbeat presence tracking.

## Application and Proxy URLs

- Development Server: `http://localhost:4200`
- Production SSR Server: `http://localhost:4000` (served via `node dist/frontend/server/server.mjs`)
- Local Development Proxy Target: `http://localhost:8080` (`local-proxy.conf.json`)
- Container Environment Proxy Target: `http://backend:8080` (`proxy.conf.json`)
- Remote Development Tunnel Target: `https://vocalist-virtual-luckiness.ngrok-free.dev` (`dev-proxy.conf.json`)

## Complete Route Table

| Path | Guard | Page Title | Description |
| --- | --- | --- | --- |
| `/register` | `guestGuard` | Sign Up – Pcenter | Registration form for new user accounts |
| `/login` | `guestGuard` | Log In – Pcenter | Login form with JWT token storage |
| `/rooms` | `authGuard` | Rooms – Pcenter | Room gallery with search and "All" / "Favorites" filter tabs |
| `/rooms/create` | `authGuard` | Create Room – Pcenter | Form for creating public or private rooms |
| `/rooms/:id` | `authGuard` | Room Details – Pcenter | Workspace with focus timer, member roster, and task panel |
| `/profile` | `authGuard` | Profile – Pcenter | User profile dashboard and metadata editor |
| `/404` | None | 404 - Page Not Found – Pcenter | Route fallback page |
| `/` | None | None | Redirects to `/register` |
| `/**` | None | None | Wildcard route; redirects to `/404` |

## Frontend Features and Technical Architecture

- Standalone Architecture: Entire application is built using Angular standalone components with modern `@if` and `@for` control flow blocks.
- Server-Side Rendering (SSR) and Hydration: Pre-rendered using `@angular/ssr` and Node/Express server (`server.ts`), hydrated on the client without full page reloads.
- State Facades: State and business logic are segregated into dedicated facade services (`RoomsFacade`, `RoomDetailFacade`, `ProfileFacade`, `AuthFacade`) combining Signals and RxJS observables.
- Route Guards: Functional `authGuard` prevents unauthorized access to protected paths, while `guestGuard` redirects logged-in users away from registration and login screens.
- HTTP Interceptor Chain:
  - `authInterceptor`: Intercepts outbound requests and injects the `Authorization: Bearer <token>` header for `/api/` calls.
  - `authErrorInterceptor`: Catches `401 Unauthorized` responses and cleans authentication state.
  - `ngrokInterceptor`: Injects the `ngrok-skip-browser-warning` header when querying through development tunnels.
- Heartbeat Presence System: While inside `/rooms/:id`, the component initiates an HTTP interval loop sending a heartbeat to `/api/v1/rooms/{roomId}/heartbeat` every 25 seconds and polling `/api/v1/rooms/{roomId}/members` to keep the active member roster up to date. No WebSocket connections are used.
- Productivity Utilities: Interactive session timer/stopwatch component with play, pause, and reset controls, along with an in-room task management panel for task CRUD.
- Styling: Native CSS variables and custom SCSS components without third-party CSS utility frameworks, supporting responsive navigation, toast notifications (`ToastService`), and theme switching (`ThemeService`).

## Frontend File Structure

```
frontend/
├── angular.json                                 # Angular CLI workspace build configuration
├── package.json                                 # Node dependencies and build scripts
├── Dockerfile                                   # Node 22 container configuration
├── dev-proxy.conf.json                          # Proxy settings for ngrok remote endpoint
├── local-proxy.conf.json                        # Proxy settings for local Spring Boot backend
├── proxy.conf.json                              # Proxy settings for Docker network backend
└── src/
    ├── main.ts                                  # Client application bootstrap
    ├── main.server.ts                           # Server application bootstrap
    ├── server.ts                                # Express server engine for SSR
    ├── index.html                               # Root HTML template
    ├── styles.css                               # Global styling and CSS custom properties
    ├── environments/                            # Environment configuration files
    │   ├── environment.ts                       # Production environment configuration
    │   └── environment.development.ts           # Local development configuration
    └── app/
        ├── app.ts                               # Root component
        ├── app.html                             # Root layout markup with router-outlet and toasts
        ├── app.config.ts                        # Application providers, router, interceptors
        ├── app.config.server.ts                 # Server-side rendering providers
        ├── app.routes.ts                        # Application route definitions
        ├── app.routes.server.ts                 # Server render mode route configuration
        ├── core/                                # Singleton logic and shared application primitives
        │   ├── components/toast/                # Toast notification UI component
        │   ├── guards/                          # Functional route guards (auth.guard, guest.guard)
        │   ├── interceptors/                    # HTTP interceptors (auth, auth-error, ngrok)
        │   ├── models/                          # Shared data interfaces (auth, room-member)
        │   └── services/                        # AuthService, ToastService, ThemeService
        └── features/                            # Feature-driven domain modules
            ├── auth/                            # Authentication module
            │   ├── login/                       # Login component and reactive forms
            │   ├── register/                    # Registration component and reactive forms
            │   └── services/auth.facade.ts      # Authentication state facade
            ├── profile/                         # User profile module
            │   ├── dashboard/                   # Profile viewing and editing view
            │   ├── models/profile.models.ts     # Profile TypeScript contracts
            │   └── services/                    # ProfileService and ProfileFacade
            ├── rooms/                           # Room browsing, detail, and member interaction
            │   ├── rooms-view.component.*       # Rooms listing and search overview
            │   ├── create-room/                 # Room creation form component
            │   ├── room-detail/                 # In-room workspace container
            │   ├── components/                  # Room UI sub-components
            │   │   ├── room-action-button/      # Action triggers
            │   │   ├── room-card/               # Room summary card
            │   │   ├── room-header/             # Room title, favorite toggle, and controls
            │   │   ├── room-join/               # Modal dialog for joining rooms
            │   │   ├── room-members-list/       # Active members roster
            │   │   ├── room-task-panel/         # In-room task management interface
            │   │   ├── room-timer/              # Focus timer and stopwatch widget
            │   │   ├── sidebar/                 # Left navigation sidebar
            │   │   └── top-nav/                 # Top application header
            │   ├── models/rooms.models.ts       # Room and task data contracts
            │   └── services/                    # RoomService, TaskService, RoomMemberService, Facades
            └── not-found/                       # 404 error page component
```

## Running the Application

1. Local development server:
   ```bash
   npm start
   ```

2. Build for production:
   ```bash
   npm run build
   ```

3. Run the SSR server bundle:
   ```bash
   npm run serve:ssr:frontend
   ```
