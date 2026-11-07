# SkillSwap Campus

Peer-to-peer skill-swap desktop app for students (Java Swing + PostgreSQL).
Students list what they can teach and want to learn, get matched, book sessions, chat, share reels, rate each other and earn badges.

## Architecture

```
 Desktop app (Java Swing, runs on every student's computer)
        |  JDBC (PostgreSQL protocol)
        v
 PostgreSQL database (shared: accounts, posts, reels, bookings, chat)
```

There is no separate application server yet: each desktop app talks to PostgreSQL directly
(see **Security notes** before deploying to real users).

## Project layout

```
skillswap/
├── pom.xml                         Maven build (PostgreSQL driver, optional JavaFX video, runnable jar)
├── database/schema.sql             Tables (the app also creates them automatically on first connect)
├── lib/postgresql-42.7.4.jar       JDBC driver for building without Maven
├── scripts/                        run-dev, package (installer), build-no-maven  (.sh and .bat)
├── .env.example  .gitignore
└── src/main/java/campus/skillswap/
    ├── Main.java                   Entry point
    ├── config/AppConfig.java       ~/.skillswap/config.properties (DB connection, theme)
    ├── core/AppState.java          Loaded data snapshot + signed-in user
    ├── model/                      User, Post, Sess, Msg, Report, Store, Match
    ├── data/                       Database (SQL, polling, saving) + Seed (demo data)
    ├── service/                    Stats (ratings, badges, matching), Skills (categories), Security (hashing)
    └── ui/
        ├── Theme, AppShell, Notifier       Look & feel, main window, toasts/tray/reminders
        ├── screens/                Login, Connection, Feed, Search, Certs, Bookings, Nearby, Messages, Profile, Settings
        └── media/                  Videos, Reels, VideoPlayerFX (optional JavaFX player)
```

## Run it (development)

Requirements: JDK 17+ and Maven, and a PostgreSQL server you can reach.

```
mvn exec:java            # or scripts/run-dev.sh  /  scripts\run-dev.bat
```

No Maven? `scripts/build-no-maven.sh` (or `.bat`) compiles with plain `javac` and the jar in `lib/`
(reels then open in the default video player instead of inside the app).

On first start a **Connect to PostgreSQL** dialog appears. Enter host, port, database, user, password.
The database and all tables are created automatically, and demo data is added if it is empty.
Settings are remembered per computer in `~/.skillswap/config.properties`.
Demo accounts (password `demo`): ramos, alamag, henry, jeb, chinu, dino, jamesa, kylet.

## Deploy

1. **Host the database** somewhere every student can reach (Supabase, Neon, Railway, a school server...).
   Create a database user for the app and give students the host/port/db/user/password.
2. **Build the installer** on the OS you are targeting:
   - Windows: `scripts\package.bat`  (needs the WiX Toolset for .exe/.msi, or switch to `--type app-image`)
   - macOS: `scripts/package.sh`  -> .dmg
   - Linux: `scripts/package.sh`  -> .deb
   The installer includes Java, so students install nothing else. Output is in `dist/`.
3. Students install the app, enter the database details once, and sign in.

`mvn package` alone produces `target/skillswap-campus.jar` (run with `java -jar`).

## Security notes (read before real deployment)

- Every client connects to the database directly, so every client holds the database login.
  Use a **dedicated limited database user** (not `postgres`/admin), never share your admin password, and treat
  the credentials as visible to anyone who has the app.
- Passwords are stored as SHA-256 hashes (salted only with the username). For a real launch, move to bcrypt/argon2
  and put a small backend API between the app and the database, so the DB password never leaves the server.
- Reels are stored inside PostgreSQL (BYTEA, max 60 MB each). Large volumes need object storage instead.
- Never commit `.env` or `config.properties` (both are in `.gitignore`).
