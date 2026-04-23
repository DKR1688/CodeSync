from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import ListFlowable, ListItem, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle


OUT_PATH = Path(r"D:\Code Collaboration Platform\CodeSync\CodeSync_Backend_Service_Architecture_Explanation.pdf")


styles = getSampleStyleSheet()
styles.add(
    ParagraphStyle(
        name="TitleCenter",
        parent=styles["Title"],
        alignment=TA_CENTER,
        fontName="Helvetica-Bold",
        fontSize=21,
        leading=26,
        spaceAfter=10,
        textColor=colors.HexColor("#12355B"),
    )
)
styles.add(
    ParagraphStyle(
        name="SubTitleCenter",
        parent=styles["Heading2"],
        alignment=TA_CENTER,
        fontName="Helvetica",
        fontSize=10.5,
        leading=14,
        spaceAfter=14,
        textColor=colors.HexColor("#4A5568"),
    )
)
styles.add(
    ParagraphStyle(
        name="Section",
        parent=styles["Heading1"],
        fontName="Helvetica-Bold",
        fontSize=16,
        leading=20,
        spaceBefore=8,
        spaceAfter=8,
        textColor=colors.HexColor("#12355B"),
    )
)
styles.add(
    ParagraphStyle(
        name="SubSection",
        parent=styles["Heading2"],
        fontName="Helvetica-Bold",
        fontSize=12,
        leading=15,
        spaceBefore=6,
        spaceAfter=5,
        textColor=colors.HexColor("#1D4E89"),
    )
)
styles.add(
    ParagraphStyle(
        name="BodyX",
        parent=styles["BodyText"],
        fontName="Helvetica",
        fontSize=9.6,
        leading=13,
        spaceAfter=5,
    )
)
styles.add(
    ParagraphStyle(
        name="TinyCell",
        parent=styles["BodyText"],
        fontName="Helvetica",
        fontSize=8.2,
        leading=10.4,
    )
)


SERVICE_DATA = [
    {
        "name": "Eureka Server",
        "module": "eureka-server",
        "port": "8761",
        "purpose": "Central service registry for discovery.",
        "what": [
            "Spring Boot 4.0.5 application",
            "Spring Cloud Netflix Eureka Server",
            "Spring Boot Actuator",
            "Java 21",
        ],
        "where": [
            "eureka-server/pom.xml",
            "eureka-server/src/main/java/com/codesync/eureka/EurekaServerApplication.java",
            "eureka-server/src/main/resources/application.properties",
        ],
        "how": "The application starts on port 8761, does not register itself as a client, and exposes the Eureka registry so every other microservice can publish and discover live instances.",
        "why": "Without a registry, the gateway and other services would depend on fixed host and port assumptions. Eureka makes routing and internal calls more resilient and matches the case-study microservice architecture.",
        "features": [
            "Registers and displays live backend instances",
            "Supports dynamic service lookup for gateway and discovery-aware RestClient calls",
            "Exposes actuator health and info endpoints",
        ],
        "tests": ["EurekaServerApplicationTests.java: context loading for the registry service"],
        "communication": "All backend services register with this module. The gateway and load-balanced RestClients resolve service names through it.",
    },
    {
        "name": "API Gateway",
        "module": "api-gateway",
        "port": "8080",
        "purpose": "Single backend entry point for all client-facing API traffic.",
        "what": [
            "Spring Cloud Gateway Server WebFlux",
            "Spring Cloud LoadBalancer",
            "Spring Cloud Eureka Client",
            "Spring Boot Actuator",
            "Custom CORS filter and gateway info controller",
        ],
        "where": [
            "api-gateway/pom.xml",
            "api-gateway/src/main/resources/application.properties",
            "api-gateway/src/main/java/com/codesync/api/config/GatewayCorsConfig.java",
            "api-gateway/src/main/java/com/codesync/api/web/GatewayInfoController.java",
        ],
        "how": "Routes are defined in application.properties using discovery-backed lb:// URIs for auth, project, file, collab, version, comment, notification, and execution services. A reactive CORS filter allows frontend access with configurable origin patterns. GatewayInfoController exposes a quick route summary and health pointers.",
        "why": "The future Angular frontend should talk to one stable backend endpoint instead of every service separately. This keeps client integration cleaner and makes route, CORS, and discovery behavior centralized.",
        "features": [
            "Routes /auth/**, /api/v1/projects/**, /api/v1/files/**, /api/v1/sessions/**, /api/v1/versions/**, /api/v1/comments/**, /api/v1/notifications/**, and /api/v1/executions/**",
            "Handles WebSocket route prefixes for notifications and execution streams",
            "Exposes gateway actuator route inspection",
            "Publishes service route summary at / and /gateway/info",
        ],
        "tests": ["ApiGatewayApplicationTests.java: verifies gateway context and route metadata exposure"],
        "communication": "Receives client requests and forwards them to downstream services using Eureka-discovered service names.",
    },
    {
        "name": "Auth Service",
        "module": "auth-service",
        "port": "8081",
        "purpose": "Identity, authentication, user profile, JWT, and admin user management.",
        "what": [
            "Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator",
            "OAuth2 client support for GitHub and Google sign-in",
            "JJWT for token creation and validation",
            "MySQL runtime with H2 for tests",
            "Eureka client registration",
        ],
        "where": [
            "auth-service/pom.xml",
            "auth-service/src/main/resources/application.properties",
            "auth-service/src/main/java/com/codesync/auth/resource/AuthResource.java",
            "auth-service/src/main/java/com/codesync/auth/config/SecurityConfig.java",
            "auth-service/src/main/java/com/codesync/auth/security/JwtAuthenticationFilter.java",
            "auth-service/src/main/java/com/codesync/auth/service/AuthServiceImpl.java",
        ],
        "how": "AuthResource exposes registration, admin bootstrap, login, logout, refresh, profile, search, password change, deactivate, reactivate, list users, and delete-user operations. Spring Security plus JwtAuthenticationFilter authenticates protected requests. OAuth2 client settings support future social login through the gateway callback URLs.",
        "why": "Every other business service depends on a trusted identity source. This service centralizes login and user state so project, file, comment, notification, and execution flows can enforce authorization consistently.",
        "features": [
            "Register normal user and bootstrap first admin",
            "Login, logout, and refresh JWT token",
            "Get and update current profile or profile by id",
            "Change password and deactivate account",
            "Search users for mentions or member management",
            "Admin-only list, reactivate, and delete users",
        ],
        "tests": ["AuthServiceApplicationTests.java", "AuthServiceImplTest.java"],
        "communication": "Supplies user lookup and authorization context to other services. Project service validates members through it, and comment service can search mentioned users through it.",
    },
    {
        "name": "Project Service",
        "module": "project-service",
        "port": "8082",
        "purpose": "Project metadata, ownership, visibility, membership, permissions, stars, forks, and archive state.",
        "what": [
            "Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator",
            "RestClient for downstream auth and file calls",
            "JWT parsing for authenticated user context",
            "MySQL or PostgreSQL capable JPA persistence",
            "Eureka client registration",
        ],
        "where": [
            "project-service/pom.xml",
            "project-service/src/main/resources/application.properties",
            "project-service/src/main/java/com/codesync/project/resource/ProjectResource.java",
            "project-service/src/main/java/com/codesync/project/client/AuthServiceClient.java",
            "project-service/src/main/java/com/codesync/project/client/FileServiceClient.java",
            "project-service/src/main/java/com/codesync/project/service/ProjectServiceImpl.java",
        ],
        "how": "ProjectResource creates projects for the authenticated owner, computes permission DTOs, filters private/public visibility, supports update, archive, star, fork, member add/remove, owner/member/admin listings, language search, and deletion. AuthServiceClient validates that invited members exist. FileServiceClient is used during project fork to copy project files, with rollback behavior if copy fails.",
        "why": "The project domain is the authorization anchor of the platform. File, comment, version, collaboration, and execution services all need a consistent decision about who can read, write, or manage a project.",
        "features": [
            "Create, read, update, archive, delete project",
            "Visibility-aware project access for public and private projects",
            "Permission endpoint consumed by multiple downstream services",
            "Owner and member project listings",
            "Add and remove members",
            "Star and fork support, including file copy integration",
            "Admin endpoint to list all projects",
        ],
        "tests": ["ProjectServiceApplicationTests.java", "ProjectServiceImplTest.java"],
        "communication": "Calls auth-service for user existence checks and file-service during project fork. Serves permission decisions to file, collab, comment, version, and execution services.",
    },
    {
        "name": "File Service",
        "module": "file-service",
        "port": "8083",
        "purpose": "Project file and folder storage, hierarchy, content update, search, move, rename, delete, restore, and tree building.",
        "what": [
            "Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator",
            "RestClient permission client to project-service",
            "JWT-based authenticated user extraction",
            "MySQL runtime with H2 support",
            "Structured DTOs for folder creation, content updates, moves, renames, tree nodes, and project copy",
        ],
        "where": [
            "file-service/pom.xml",
            "file-service/src/main/resources/application.properties",
            "file-service/src/main/java/com/codesync/file/resource/FileResource.java",
            "file-service/src/main/java/com/codesync/file/client/ProjectPermissionClient.java",
            "file-service/src/main/java/com/codesync/file/service/FileServiceImpl.java",
        ],
        "how": "FileResource checks project read or write permission before exposing file content or mutating operations. It supports file creation, folder creation, cross-project file copy, get by id, list by project, raw content fetch, content update, rename, move, delete, restore, tree generation, and search inside a project.",
        "why": "The collaborative editor experience depends on a dedicated service that understands project-scoped file structure, not just generic binary storage. Permission checks are delegated to the project domain so authorization stays consistent.",
        "features": [
            "Create code file with creator and last-editor tracking",
            "Create logical folder nodes",
            "Copy files when a project is forked",
            "Read file metadata and raw content",
            "Update content, rename, move, delete, restore",
            "Build hierarchical project tree",
            "Search within project files",
        ],
        "tests": ["FileServiceApplicationTests.java", "FileServiceImplTest.java"],
        "communication": "Calls project-service permission endpoint for read/write validation. Provides file validation data to collab-service and version-service workflows.",
    },
    {
        "name": "Collaboration Service",
        "module": "collab-service",
        "port": "8084",
        "purpose": "Live collaboration sessions tied to files, participants, cursors, and content broadcast state.",
        "what": [
            "Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator",
            "Spring WebSocket with STOMP and SockJS",
            "RestClient clients for project and file services",
            "JWT-based security",
            "Scheduled idle-session cleanup settings",
        ],
        "where": [
            "collab-service/pom.xml",
            "collab-service/src/main/resources/application.properties",
            "collab-service/src/main/java/com/codesync/collab/resource/CollabResource.java",
            "collab-service/src/main/java/com/codesync/collab/config/WebSocketConfig.java",
            "collab-service/src/main/java/com/codesync/collab/client/ProjectPermissionClient.java",
            "collab-service/src/main/java/com/codesync/collab/client/FileServiceClient.java",
            "collab-service/src/main/java/com/codesync/collab/service/CollabServiceImpl.java",
        ],
        "how": "CollabResource handles session creation, active-session listing, fetch by session id, list by project, active session by file, join, leave, end, participant listing, cursor update, content broadcast, and kick participant operations. WebSocketConfig exposes /ws/collab with a simple /topic broker and /app application destination prefix. Configuration properties control idle timeout, cleanup interval, and allowed origins.",
        "why": "Real-time collaboration is a separate concern from file persistence. Splitting it out keeps transient session state and WebSocket behavior isolated from long-term file storage while still enforcing project and file validation.",
        "features": [
            "Create collaboration session for a file",
            "Join and leave session",
            "Track participants and roles",
            "Update cursor positions",
            "Broadcast live content changes",
            "Find active session for a file",
            "End or administratively kick participants",
            "Expose live WebSocket endpoint /ws/collab",
        ],
        "tests": ["CollabServiceApplicationTests.java", "CollabServiceImplTest.java"],
        "communication": "Calls project-service to verify access and file-service to validate target files. Can be consumed by a frontend editor over REST plus WebSocket.",
    },
    {
        "name": "Version Service",
        "module": "version-service",
        "port": "8085",
        "purpose": "Snapshot history, branches, tags, diffs, and restore support for project files.",
        "what": [
            "Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator",
            "RestClient permission and file integrations",
            "DTOs for snapshot create, branch create, diff, restore, and tagging",
            "JWT-based authorization",
            "Hash and branch-oriented snapshot model",
        ],
        "where": [
            "version-service/pom.xml",
            "version-service/src/main/resources/application.properties",
            "version-service/src/main/java/com/codesync/version/resource/VersionResource.java",
            "version-service/src/main/java/com/codesync/version/client/ProjectPermissionClient.java",
            "version-service/src/main/java/com/codesync/version/client/FileServiceClient.java",
            "version-service/src/main/java/com/codesync/version/service/VersionServiceImpl.java",
        ],
        "how": "VersionResource protects all reads and writes with project permission checks. It supports snapshot creation, get by snapshot id, list by file or project, branch-specific history, latest snapshot lookup, file history, diff between two snapshots, restore from a snapshot, create branch from a snapshot, and tag a snapshot.",
        "why": "Collaborative coding needs a durable record of meaningful states beyond the current file content. Snapshot storage lets teams inspect history, restore stable states, and compare changes safely.",
        "features": [
            "Create snapshot with project and file context",
            "Get snapshot by id",
            "Get snapshots by file, project, or branch",
            "Find latest snapshot and full file history",
            "Generate line-level diff response",
            "Restore snapshot back into file flow",
            "Create branch head from source snapshot",
            "Tag snapshots",
        ],
        "tests": ["VersionServiceApplicationTests.java", "VersionServiceImplTest.java"],
        "communication": "Uses project-service permissions before exposing history and works with file-service during restore-related workflows.",
    },
    {
        "name": "Comment Service",
        "module": "comment-service",
        "port": "8086",
        "purpose": "Inline code review comments, threaded replies, resolve state, and mention-triggered notifications.",
        "what": [
            "Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator",
            "RestClient clients for auth, project, and notification services",
            "JWT-based identity extraction",
            "Comment entity linked to project, file, line, column, parent comment, and snapshot",
            "Feature flag for mention notifications",
        ],
        "where": [
            "comment-service/pom.xml",
            "comment-service/src/main/resources/application.properties",
            "comment-service/src/main/java/com/codesync/comment/resource/CommentResource.java",
            "comment-service/src/main/java/com/codesync/comment/client/AuthUserClient.java",
            "comment-service/src/main/java/com/codesync/comment/client/ProjectPermissionClient.java",
            "comment-service/src/main/java/com/codesync/comment/client/NotificationClient.java",
            "comment-service/src/main/java/com/codesync/comment/service/CommentServiceImpl.java",
        ],
        "how": "CommentResource allows add, get by id, list by file, list by project, reply listing, line-based filtering, count-by-file, resolved/unresolved filtering, update, resolve, unresolve, and delete. Access is controlled through project permissions, and comment edits or deletes are restricted to the author or a manager-level user. The service can look up mentioned users and create notifications through downstream clients.",
        "why": "Code collaboration is not complete without discussion anchored to exact locations in code. This service isolates review conversation state and ties it into notifications without bloating the file or project domains.",
        "features": [
            "Add top-level or reply comment",
            "Bind comment to line, column, file, project, and snapshot",
            "Get comments by file, project, line, or reply thread",
            "Get file comment count",
            "Filter resolved and unresolved comments",
            "Update, resolve, unresolve, and delete comment",
            "Trigger notification workflows for mentions and comment events",
        ],
        "tests": ["CommentServiceApplicationTests.java", "CommentServiceImplTest.java"],
        "communication": "Checks project permissions through project-service, looks up users through auth-service, and pushes notifications through notification-service.",
    },
    {
        "name": "Notification Service",
        "module": "notification-service",
        "port": "8087",
        "purpose": "In-app notifications, read state management, bulk delivery, and real-time notification publishing.",
        "what": [
            "Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator",
            "Spring WebSocket with STOMP and SockJS",
            "Notification publisher abstraction",
            "JWT-based recipient and admin authorization",
            "Optional email notification flag",
        ],
        "where": [
            "notification-service/pom.xml",
            "notification-service/src/main/resources/application.properties",
            "notification-service/src/main/java/com/codesync/notification/resource/NotificationResource.java",
            "notification-service/src/main/java/com/codesync/notification/config/WebSocketConfig.java",
            "notification-service/src/main/java/com/codesync/notification/service/NotificationServiceImpl.java",
            "notification-service/src/main/java/com/codesync/notification/service/NotificationPublisher.java",
        ],
        "how": "NotificationResource supports single send, bulk send, send email, get by id, get by recipient, unread count, mark one read, mark all read, delete read, delete one notification, admin list all, filter by type, and filter by related object id. WebSocketConfig exposes /ws/notifications for live consumption with both plain WebSocket and SockJS endpoints.",
        "why": "Collaboration events should not require polling every domain. A dedicated notification service collects user-facing events into one consistent inbox and gives the frontend a real-time channel to listen to.",
        "features": [
            "Create single notification",
            "Send bulk notification as admin",
            "Trigger email pathway for admin use cases",
            "Get recipient-specific notifications",
            "Get unread count",
            "Mark single or all notifications as read",
            "Delete read notifications",
            "Filter by type or related object",
            "Expose WebSocket endpoint /ws/notifications",
        ],
        "tests": ["NotificationServiceApplicationTests.java", "NotificationServiceImplTest.java"],
        "communication": "Receives notification creation requests from comment-service and can stream updates to connected clients over WebSocket.",
    },
    {
        "name": "Execution Service",
        "module": "execution-service",
        "port": "8088",
        "purpose": "Code execution jobs, sandboxed runners, language registry, queueing, streaming, and execution statistics.",
        "what": [
            "Spring MVC, Spring Security, Spring Data JPA, Validation, Actuator",
            "Spring AMQP for optional RabbitMQ-backed queue mode",
            "Spring WebSocket for result streaming",
            "RestClient to project-service permission endpoint",
            "ThreadPoolTaskExecutor worker pool",
            "Docker-based sandbox runner and process manager",
            "Supported language seeding on startup",
        ],
        "where": [
            "execution-service/pom.xml",
            "execution-service/src/main/resources/application.properties",
            "execution-service/src/main/java/com/codesync/execution/resource/ExecutionResource.java",
            "execution-service/src/main/java/com/codesync/execution/config/RabbitQueueConfig.java",
            "execution-service/src/main/java/com/codesync/execution/config/ExecutionWorkerConfig.java",
            "execution-service/src/main/java/com/codesync/execution/service/SupportedLanguageSeeder.java",
            "execution-service/src/main/java/com/codesync/execution/sandbox/DockerSandboxRunner.java",
            "execution-service/src/main/java/com/codesync/execution/service/ExecutionWorker.java",
        ],
        "how": "ExecutionResource accepts execution submissions, exposes job lookup and result lookup, cancellation, per-user history, per-project history, supported languages, runtime version lookup, admin job search, admin execution stats, and admin language management. Queue mode can be local or RabbitMQ-based. Worker threads consume jobs. DockerSandboxRunner writes source into a temporary directory, runs it inside a constrained Docker container with no network and capped CPU and memory, captures stdout and stderr, and cleans up afterward. SupportedLanguageSeeder inserts a default language catalog when the database is empty.",
        "why": "Remote code execution is operationally risky and domain-specific, so it needs its own service, queueing model, sandboxing, and controls. Keeping it separate protects the rest of the platform and makes language or runtime expansion easier.",
        "features": [
            "Submit execution job for a project",
            "Get execution job metadata and result",
            "Cancel execution by owner or admin",
            "List current user or project execution history",
            "List supported languages and runtime versions",
            "Admin job filtering and stats",
            "Admin create, update, enable, and disable language definitions",
            "Docker sandbox with CPU, memory, process, and network restrictions",
            "Rabbit queue mode or local queue mode",
            "Seeded languages: Python, Java, JavaScript, C, C++, Go, Rust, Ruby, TypeScript, PHP, Kotlin, Swift, and R",
        ],
        "tests": [
            "ExecutionServiceApplicationTests.java",
            "ExecutionServiceImplTest.java",
            "ExecutionWorkerTest.java",
            "SupportedLanguageSeederTest.java",
        ],
        "communication": "Calls project-service for permission checks before running or exposing project execution history. Can stream execution progress through WebSocket routes handled by the gateway.",
    },
    {
        "name": "CodeSync Web",
        "module": "codesync-web",
        "port": "8090",
        "purpose": "Headless helper service kept minimal after frontend removal.",
        "what": [
            "Spring Boot MVC",
            "Spring Boot Actuator",
            "Single service-status controller",
            "No server-rendered frontend layer",
        ],
        "where": [
            "codesync-web/pom.xml",
            "codesync-web/src/main/resources/application.properties",
            "codesync-web/src/main/java/com/codesync/web/ServiceStatusController.java",
        ],
        "how": "The service runs on port 8090 and exposes a root JSON response plus actuator health and info. The root response explicitly states that frontend is managed in a separate Angular repository.",
        "why": "You asked to keep frontend implementation out of this repository. This module remains as a lightweight service placeholder instead of a server-rendered UI application.",
        "features": [
            "GET / returns service identity and frontend boundary information",
            "Actuator health endpoint",
            "Actuator info endpoint",
        ],
        "tests": ["CodesyncWebApplicationTests.java: contextLoads smoke test"],
        "communication": "This module is intentionally minimal and not part of core inter-service backend orchestration.",
    },
]


def bullet_list(items):
    return ListFlowable(
        [ListItem(Paragraph(item, styles["BodyX"])) for item in items],
        bulletType="bullet",
        leftIndent=15,
    )


story = []
story.append(Spacer(1, 18 * mm))
story.append(Paragraph("CodeSync Backend Architecture and Service-by-Service Explanation", styles["TitleCenter"]))
story.append(
    Paragraph(
        "What was used, where it was used, how it was used, and why it was used across the full backend",
        styles["SubTitleCenter"],
    )
)
story.append(
    Paragraph(
        "This document is backend-only. Frontend implementation is intentionally excluded because the Angular frontend will live in a separate repository.",
        styles["BodyX"],
    )
)
story.append(
    Paragraph(
        "Verified against the current CodeSync workspace structure, service configuration, resource classes, and test layout.",
        styles["BodyX"],
    )
)
story.append(Spacer(1, 6 * mm))

summary_rows = [["Service", "Port", "Main Responsibility"]]
for service in SERVICE_DATA:
    summary_rows.append([service["name"], service["port"], service["purpose"]])

summary_table = Table(summary_rows, colWidths=[52 * mm, 16 * mm, 102 * mm])
summary_table.setStyle(
    TableStyle(
        [
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#12355B")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
            ("FONTSIZE", (0, 0), (-1, -1), 8.5),
            ("LEADING", (0, 0), (-1, -1), 10.5),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.whitesmoke, colors.HexColor("#EEF4FB")]),
            ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#C7D2E1")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 4),
            ("RIGHTPADDING", (0, 0), (-1, -1), 4),
            ("TOPPADDING", (0, 0), (-1, -1), 4),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ]
    )
)
story.append(summary_table)
story.append(PageBreak())

story.append(Paragraph("1. Common Backend Stack", styles["Section"]))
story.append(
    bullet_list(
        [
            "Spring Boot 4.0.5 is the foundation used by every module for application bootstrapping and auto-configuration.",
            "Java 21 is the common runtime across the backend.",
            "Spring Cloud 2025.1.1 is used where service discovery, gateway routing, and Eureka integration are needed.",
            "Actuator is present across the platform so health and operational information can be exposed consistently.",
            "Most business services use Spring MVC plus Spring Security, Spring Data JPA, Validation, and JWT-based request authentication.",
            "MySQL is the default runtime persistence choice in the business services, with H2 available for tests or lightweight runs in several modules.",
            "Service-to-service calls use Spring RestClient. Several clients are implemented with discovery-first behavior and direct URL fallback.",
            "WebSocket and STOMP are used where the backend needs real-time push behavior, especially in collaboration, notifications, and execution streaming scenarios.",
            "The gateway is discovery-backed with lb:// service URIs, which means routing prefers Eureka-based resolution rather than only fixed localhost assumptions.",
        ]
    )
)

story.append(Paragraph("2. Overall Request Flow", styles["Section"]))
story.append(
    ListFlowable(
        [
            ListItem(Paragraph(item, styles["BodyX"]))
            for item in [
                "A client or the future Angular frontend sends requests to the API Gateway on port 8080.",
                "The gateway routes the request to the correct downstream microservice using Eureka discovery.",
                "Auth-service establishes identity and issues JWT tokens.",
                "Project-service becomes the permission authority for project access.",
                "File, collaboration, version, comment, and execution services ask project-service whether the current user can read or write the project.",
                "Comment-service can ask auth-service about mentioned users and ask notification-service to create alerts.",
                "Execution-service isolates code running in sandboxed containers and exposes results separately from file persistence.",
                "Notification and collaboration services expose WebSocket endpoints for live updates.",
            ]
        ],
        bulletType="1",
        leftIndent=18,
    )
)
story.append(PageBreak())

for index, service in enumerate(SERVICE_DATA, start=1):
    story.append(Paragraph(f"3.{index} {service['name']}", styles["Section"]))
    story.append(
        Paragraph(
            f"<b>Module:</b> {service['module']} &nbsp;&nbsp;&nbsp; <b>Port:</b> {service['port']}",
            styles["BodyX"],
        )
    )
    story.append(Paragraph(f"<b>Main responsibility:</b> {service['purpose']}", styles["BodyX"]))

    detail_rows = [
        [
            Paragraph("<b>What used</b>", styles["TinyCell"]),
            Paragraph("<b>Where used</b>", styles["TinyCell"]),
            Paragraph("<b>How used</b>", styles["TinyCell"]),
            Paragraph("<b>Why used</b>", styles["TinyCell"]),
        ],
        [
            Paragraph("<br/>".join(service["what"]), styles["TinyCell"]),
            Paragraph("<br/>".join(service["where"]), styles["TinyCell"]),
            Paragraph(service["how"], styles["TinyCell"]),
            Paragraph(service["why"], styles["TinyCell"]),
        ],
    ]
    detail_table = Table(detail_rows, colWidths=[39 * mm, 46 * mm, 50 * mm, 47 * mm])
    detail_table.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#1D4E89")),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
                ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                ("FONTSIZE", (0, 0), (-1, -1), 8.1),
                ("LEADING", (0, 0), (-1, -1), 10),
                ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#C7D2E1")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.HexColor("#F8FBFE")]),
                ("LEFTPADDING", (0, 0), (-1, -1), 4),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4),
                ("TOPPADDING", (0, 0), (-1, -1), 4),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
            ]
        )
    )
    story.append(detail_table)
    story.append(Spacer(1, 3 * mm))
    story.append(Paragraph("Key Features", styles["SubSection"]))
    story.append(bullet_list(service["features"]))
    story.append(Paragraph("Communication Role", styles["SubSection"]))
    story.append(Paragraph(service["communication"], styles["BodyX"]))
    story.append(Paragraph("Test Coverage", styles["SubSection"]))
    story.append(bullet_list(service["tests"]))
    if index != len(SERVICE_DATA):
        story.append(PageBreak())

story.append(PageBreak())
story.append(Paragraph("4. Inter-Service Communication Used in the Backend", styles["Section"]))
comm_rows = [
    ["Caller", "Target", "Reason"],
    ["API Gateway", "All backend services", "Routes external traffic to the correct service path"],
    ["Project Service", "Auth Service", "Validates invited users and member identities"],
    ["Project Service", "File Service", "Copies files when a project is forked"],
    ["File Service", "Project Service", "Checks read or write permissions before exposing project files"],
    ["Collab Service", "Project Service", "Checks that the caller can collaborate on the project"],
    ["Collab Service", "File Service", "Checks that the file exists and session target is valid"],
    ["Version Service", "Project Service", "Checks read or write access for snapshot and restore operations"],
    ["Version Service", "File Service", "Reads or restores file content in version workflows"],
    ["Comment Service", "Project Service", "Checks comment visibility and write permission"],
    ["Comment Service", "Auth Service", "Finds users for mention and user-summary workflows"],
    ["Comment Service", "Notification Service", "Creates notifications for comment and mention events"],
    ["Execution Service", "Project Service", "Checks whether the user can run or read project executions"],
]
comm_table = Table(comm_rows, colWidths=[40 * mm, 42 * mm, 88 * mm])
comm_table.setStyle(
    TableStyle(
        [
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#12355B")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
            ("FONTSIZE", (0, 0), (-1, -1), 8.5),
            ("LEADING", (0, 0), (-1, -1), 10.5),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.whitesmoke, colors.HexColor("#EEF4FB")]),
            ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#C7D2E1")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 4),
            ("RIGHTPADDING", (0, 0), (-1, -1), 4),
            ("TOPPADDING", (0, 0), (-1, -1), 4),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ]
    )
)
story.append(comm_table)

story.append(Paragraph("5. Why This Backend Design Works for CodeSync", styles["Section"]))
story.append(
    bullet_list(
        [
            "The backend is split by business domain, so each service owns a clear slice of responsibility instead of one large mixed codebase.",
            "Project permissions are reused across the platform, which avoids inconsistent access decisions in file, comment, version, collaboration, and execution features.",
            "Discovery-backed routing through Eureka and the gateway keeps local and future deployment topology cleaner.",
            "Execution is isolated behind a dedicated sandboxed service instead of running inside the same service that stores files or comments.",
            "Real-time concerns are isolated in WebSocket-capable services instead of being forced into purely request-response modules.",
            "Auth is centralized, which is important for a collaborative platform with members, admins, notifications, and project-scoped permissions.",
            "The backend already exposes a stable API surface that a separate Angular frontend can consume through the API Gateway.",
        ]
    )
)

story.append(Paragraph("6. Testing Summary", styles["Section"]))
story.append(
    bullet_list(
        [
            "Every module includes Spring Boot test support through spring-boot-starter-test.",
            "Security-aware services also include spring-security-test for authorization-aware test scenarios.",
            "Application context smoke tests exist across Eureka, gateway, auth, project, file, collab, version, comment, notification, execution, and codesync-web.",
            "Service implementation tests exist for the core business services.",
            "Execution-service includes extra tests for worker behavior and supported language seeding.",
            "The backend has also been previously verified through end-to-end gateway flows and service health checks.",
        ]
    )
)

story.append(Paragraph("7. Frontend Boundary", styles["Section"]))
story.append(
    Paragraph(
        "There is no frontend implementation in this backend document or in the current codesync-web module. That was intentionally removed so the Angular frontend can be developed in a separate repository and integrate through the API Gateway only.",
        styles["BodyX"],
    )
)
story.append(Spacer(1, 8 * mm))
story.append(Paragraph("End of document", styles["SubTitleCenter"]))


def draw_page(canvas, doc):
    width, height = A4
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#12355B"))
    canvas.setLineWidth(1)
    canvas.line(16 * mm, height - 12 * mm, width - 16 * mm, height - 12 * mm)
    canvas.setFont("Helvetica", 8.5)
    canvas.setFillColor(colors.HexColor("#4A5568"))
    canvas.drawString(16 * mm, 8 * mm, "CodeSync Backend Architecture and Service-by-Service Explanation")
    canvas.drawRightString(width - 16 * mm, 8 * mm, f"Page {doc.page}")
    canvas.restoreState()


doc = SimpleDocTemplate(
    str(OUT_PATH),
    pagesize=A4,
    rightMargin=16 * mm,
    leftMargin=16 * mm,
    topMargin=16 * mm,
    bottomMargin=16 * mm,
    title="CodeSync Backend Architecture and Service-by-Service Explanation",
    author="OpenAI Codex",
)
doc.build(story, onFirstPage=draw_page, onLaterPages=draw_page)
print(OUT_PATH)
print(OUT_PATH.stat().st_size)
