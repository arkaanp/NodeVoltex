# NodeVoltex

![image](https://i.imgur.com/xjLX8Hd.jpeg)

Welcome to **[NodeVoltex](https://node-voltex.vercel.app/)**, a premium, modern rhythm game ecosystem built to deliver an elegant, highly responsive competitive experience. 

The project is structured into three main component layers:

---

## Repository Structure

NodeVoltex is composed of the following core directories:

*   **[`Game`](file:///e:/College/Semester%204/Netlab/game/NodeVoltex/NodeVoltex/Game)**: A high-performance 2D rhythm game client written in Java using the LibGDX framework.
    *   *Features*: Custom key mapping engine, robust texture decoders supporting automated WebP URL formatting, error-handling overlays for Render host wake-ups, and precise local performance caching.
*   **[`Portal`](file:///e:/College/Semester%204/Netlab/game/NodeVoltex/NodeVoltex/Portal)**: A modern, visually stunning React web application built with TypeScript, Vite, and custom CSS variables.
    *   *Design*: Sleek, competitive flat-aesthetics in a dark plum and violet palette matching the game's official backgrounds. Includes a scalable vertically-scrolling Song Packs grid download section, overall user rankings leaderboard, and comprehensive profile dashboards showcasing player Volforce, best plays, and recent attempts history.
*   **[`Backend`](file:///e:/College/Semester%204/Netlab/game/NodeVoltex/NodeVoltex/Backend)**: A high-throughput Spring Boot REST API backed by a PostgreSQL database.
    *   *Features*: Secure JWT player session managers, dynamic Volforce rating calculation services, and automated database triggers to maintain score deletes and overall rating synchronization.

---

##  Key Features

### Fully Customizable Gameplay
*   **Tailored Performance Layouts**: Complete control over your playfield environment. Customize gameplay settings directly from the client, including playfield width, scroll speed, background dim level, and so on.
*   **Mods**: Experiment and practice charts using mods like Autoplay (the map will play itself perfectly) and No Laser (automatic laser).
*   **Key Mapping**: Freedom to rebind input keys (A-Z, 0-9, Space) with primary and alternate configuration slots, featuring automated input collision prevention rules.

### Online Ecosystem & User Profiles
*   **Integrated Accounts & Personalization**: Register a personal profile and upload custom profile pictures (avatars), securely managed through JWT authentication sessions.
*   **Automated Volforce Rating System**: Performance rating is calculated dynamically using a competitive mathematical formula (taking into account map difficulty, score value, and chart clearance tier e.g., UC/PUC) based on your top 10 plays.
*   **Online Leaderboards**: Every map features an online leaderboard displaying real-time high-score ranks, grades, judgments (Criticals, Nears, Misses, Laser ticks), play timestamps, and secure replay recordings.
*   **Replay Recordings**: The ability to watch how a player plays in-game on any map leaderboard.

### Companion Web Portal
The official **[NodeVoltex Web Portal](https://node-voltex.vercel.app/)** acts as the central companion hub, offering:
*   **Global Rankings**: A real-time leaderboard ranking the community by overall best 10 charts performance (Volforce rating).
*   **Comprehensive Profile Stats**: Interactive dashboards displaying players' overall status, Top Plays, and Recent Scores attempts history.
*   **Song Packs Distribution**: A scalable distribution center containing official release maps and expansion packs for immediate download and gameplay installation.

---

## System Diagrams & Workflows

To better understand the ecosystem, here is how the frontend, game client, REST API, and data layer coordinate.

### 1. Overall System Architecture
Shows the relationship between the client layer, REST services, media CDN, and database storage.

```mermaid
graph TD
    classDef client fill:#221729,stroke:#8b73af,stroke-width:2px,color:#fff;
    classDef backend fill:#110b14,stroke:#e2a3cd,stroke-width:2px,color:#fff;
    classDef db fill:#16101c,stroke:#00e5ff,stroke-width:2px,color:#fff;

    subgraph Client Layer ["Client Layer"]
        A["Desktop Game Client (Java / LibGDX)"]:::client
        B["Web Portal (React / TypeScript)"]:::client
    end

    subgraph API Layer ["API Layer"]
        C["Backend REST API (Spring Boot)"]:::backend
    end

    subgraph Storage Layer ["Storage Layer"]
        D["Database (PostgreSQL / Neon)"]:::db
    end

    A -->|"Submit Play Scores (HTTPS)"| C
    A -->|"Fetch Profile Pictures (HTTPS CDN)"| E["Cloudinary CDN"]:::db
    B -->|"Fetch Player Stats / Leaderboards"| C
    C -->|"CRUD Entities (JPA / Hibernate)"| D
    D -->|"Trigger: Recalculate Volforce on Delete"| D
```

### 2. Database Entity Relationship Diagram (ERD)
Details the relational database schema, key mappings, and table fields that power the NodeVoltex competitive profiles.

```mermaid
erDiagram
    users {
        uuid id PK
        varchar username
        varchar password
        varchar profile_picture_url
        double volforce
    }
    beatmaps {
        varchar id PK
        varchar title
        varchar artist
        varchar difficulty
        integer level
    }
    scores {
        uuid id PK
        uuid user_id FK
        varchar beatmap_id FK
        integer score
        varchar grade
        integer max_combo
        double volforce
        integer s_criticals
        integer criticals
        integer nears
        integer mids
        integer fars
        integer misses
        integer laser_ticks
        integer laser_misses
        integer early
        integer late
        text replay_data_json
        timestamp created_at
    }

    users ||--o{ scores : "submits"
    beatmaps ||--o{ scores : "played on"
```

### 3. High Score Submission Sequence
Illustrates the exact workflow when a score is registered, saved, rating recalculated, and visualized in the portal.

```mermaid
sequenceDiagram
    autonumber
    actor Player as Desktop Game Client
    participant API as Spring Boot API
    participant DB as PostgreSQL Database
    participant Portal as React Web Portal

    Player->>API: POST /api/scores/submit (JWT Auth, ScoreRequest)
    Note over API: 1. Calculate Play Volforce<br/>2. Retrieve player's best 10 plays
    API->>DB: Query existing score for user + beatmap
    alt New High Score achieved
        API->>DB: Update score details & set createdAt = LocalDateTime.now()
        API->>DB: Recalculate & save user's overall Volforce rating
    else First Play on Chart
        API->>DB: Insert new Score record with current timestamp
        API->>DB: Recalculate & save user's overall Volforce rating
    end
    DB-->>API: Persist successful changes
    API-->>Player: ScoreSubmitResponse (Play Volforce, New Rating)
    Note over Player: Transition to Score Screen
    Portal->>API: GET /api/users/profile/{username} (Fetch Stats)
    API-->>Portal: Deliver updated profile with new best/recent attempts
    Note over Portal: Render score with Max Combo and fresh Play Date
```

---

## Getting Started

### Chart Setup & Playing Beatmaps
> [!IMPORTANT]
> **Beatmap Installation Directory**:
> To be able to load and play charts in NodeVoltex, you must place your extracted beatmap folders inside a folder named **`songs/`** located in the same directory as the executable file (`NodeVoltex.exe` or compiled `.jar` runner).
> 
> ```text
> 📂 NodeVoltex/
>  ├── 📄 Node.Voltex-1.x.x.exe (or Node.Voltex-1.x.x.jar)
>  └── 📂 songs/
>       ├── 📂 songtitle1/
>       │    ├── 📄 mxm.json
>       │    ├── 📄 jak.png
>       │    └── 📄 audio.ogg
>       └── 📂 songtitle2/
>            ├── 📄 nov.json
>            └── ..
> ```

---

## Tech Stack

*   **Frontend**: React, TypeScript, Vite, Vanilla CSS, Lucide icons.
*   **Backend**: Spring Boot 3.x, Spring Data JPA, PostgreSQL (Neon DB).
*   **Game Client**: LibGDX Framework, Java.
*   **Database Sync**: PostgreSQL PL/pgSQL database triggers.