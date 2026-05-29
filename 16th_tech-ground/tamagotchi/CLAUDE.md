# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Claude API 토큰 사용량을 통해 다마고치를 키우는 토이 프로젝트. 사용자가 Claude를 사용할수록 토큰이 쌓이고, 그 토큰량에 따라 다마고치가 성장하는 구조를 목표로 한다.

Spring Boot 4.0.6, Java 21, Gradle, MySQL 8 (prod) / H2 (test), Thymeleaf 서버사이드 렌더링. Mashup Spring 16 Team A 제작.

## Commands

Run from the `tamagotchi/` directory.

```bash
# Build
./gradlew build

# Run locally (http://localhost:8080/login)
./gradlew bootRun

# Run all tests (uses H2 in-memory DB)
./gradlew test

# Run a single test class
./gradlew test --tests "mashup.spring16.tamagotchi.service.MemberServiceTest"

# Start MySQL for local development
cd docker && docker-compose up -d
```

Local dev connects to MySQL at `localhost:3306` with database `app_db`, user `app_user`, password `app_pass` (see `docker/docker-compose.yml`).

Production requires env vars: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (activate with `-Dspring.profiles.active=prod`).

## Architecture

Standard Spring MVC layered architecture: Controller → Service → Repository → JPA Entity.

**User flow:**
1. `/login` or `/signup` — auth handled by `AuthController`, password stored via BCrypt (`spring-security-crypto`, no full Spring Security filter chain)
2. `/signup/select` — new users pick a pet character and name; stored as a `Tamagotchi` entity linked 1:1 to `Member`
3. `/tamagotchi` — main room; redirects to `/login` if session missing, `/signup/select` if no pet exists

**Session:** `memberId` (Long) stored in `HttpSession` — no JWT or cookie-based auth.

**Key package:** `mashup.spring16.tamagotchi`
- `domain/` — JPA entities (`Member`, `Tamagotchi`)
- `dto/` — Java records for request binding (`LoginRequest`, `SignupRequest`, `TamagotchiCreateRequest`)
- `service/` — business logic; unit-tested with H2
- `controller/` — thin Thymeleaf controllers; redirect/forward model attributes
- `config/AppConfig.java` — beans (e.g., `BCryptPasswordEncoder`)

**Test profile:** `src/test/resources/application.properties` enables H2 with `ddl-auto=create-drop`. Tests are `@SpringBootTest` + `@Transactional`.