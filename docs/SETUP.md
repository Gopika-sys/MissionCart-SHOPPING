# MissionCart Local Setup & Deployment Guide (Windows PowerShell)

## Option A: Zero-Dependency Local Mode (H2 MySQL-Compatibility + Fallback AI + Demo Checkout)
```powershell
# 1. Start Backend
cd backend
mvn spring-boot:run

# 2. Start Frontend (in a new terminal)
cd frontend
npm install
npm run dev
```

## Option B: Docker MySQL 8 + Redis
```powershell
copy .env.example .env
docker compose up -d
cd backend
$env:SPRING_PROFILES_ACTIVE="mysql"
mvn spring-boot:run
```

## Admin Bootstrap
Set `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD` environment variables before starting the backend; `AdminBootstrapRunner` creates the admin account once when no `ADMIN` exists.
