

# Booking Service

## README

### Описание

**Booking Service** — это микросервис, отвечающий за управление бронированиями в системе отелей.
Он взаимодействует с **Hotel Management Service** для проверки и подтверждения доступности номеров, а также поддерживает аутентификацию и авторизацию пользователей через JWT.

Основные функции:

* Регистрация и аутентификация пользователей
* Создание и просмотр бронирований
* Интеграция с Hotel Management Service через REST
* Circuit Breaker и идемпотентность операций
* Безопасность с помощью Spring Security и JWT

---

### Технологический стек

| Категория       | Технологии                    |
| --------------- | ----------------------------- |
| Язык            | Java 17                       |
| Фреймворк       | Spring Boot 3                 |
| Безопасность    | Spring Security, JWT          |
| Устойчивость    | Resilience4j (CircuitBreaker) |
| REST            | RestTemplate                  |
| БД              | PostgreSQL                    |
| Сборка          | Maven                         |
| Контейнеризация | Docker                        |

---

### Архитектура

**Взаимодействие сервисов:**

```
        ┌────────────────────┐
        │  API Gateway       │
        │  (JWT forwarding)  │
        └────────┬───────────┘
                 │
        ┌────────▼─────────┐
        │  Booking Service │
        │  - Auth          │
        │  - Booking logic │
        └────────┬─────────┘
                 │ REST
        ┌────────▼─────────┐
        │ Hotel Service    │
        │ - Room allocate  │
        │ - Confirm/release│
        └──────────────────┘
```

**Схема данных**

**Booking**

```
id: Long
userId: Long
roomId: Long
startDate: LocalDate
endDate: LocalDate
status: BookingStatus (PENDING / CONFIRMED / CANCELLED)
createdAt: LocalDateTime
correlationId: String
```

**User**

```
id: Long
username: String
password: String (bcrypt)
role: String (USER / ADMIN)
```

---

### Инструкция по запуску

#### 1. Клонирование репозитория

```bash
git clone https://github.com/Daniil1380/Booking-Service.git
cd Booking-Service
```

#### 2. Настройка окружения

Создайте файл `.env` (или используйте `application.yml`) и добавьте параметры:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/booking_db
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
JWT_SECRET=superSecretKeyForJwtThatShouldBeStoredInEnv
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
```

#### 3. Сборка проекта

```bash
mvn clean install
```

#### 4. Запуск

```bash
mvn spring-boot:run
```

Или с Docker Compose (если есть `docker-compose.yml`):

```bash
docker-compose up --build
```

#### 5. Проверка API

**Регистрация пользователя**

```bash
POST /api/user/register
{
  "username": "user1",
  "password": "12345"
}
```

**Авторизация**

```bash
POST /api/user/auth
{
  "username": "user1",
  "password": "12345"
}
```

**Создание бронирования**

```bash
POST /api/bookings
Authorization: Bearer <jwt-token>
{
  "startDate": "2025-10-23",
  "endDate": "2025-10-25"
}
```

**Получить бронирование**

```bash
GET /api/bookings/1
Authorization: Bearer <jwt-token>
```

---

### Основные модули

| Пакет        | Назначение                                            |
| ------------ | ----------------------------------------------------- |
| `controller` | REST API для пользователей и бронирований             |
| `service`    | Бизнес-логика (идемпотентность, вызовы Hotel Service) |
| `entity`     | JPA-сущности (User, Booking)                          |
| `repository` | Spring Data JPA репозитории                           |
| `security`   | Конфигурация JWT, ролей и фильтров                    |
| `dto`        | Модели запросов/ответов                               |

---

## ADR — Architectural Decision Records

### ADR-1: Идемпотентность бронирований

**Решение:** использовать `correlationId` (UUID) для предотвращения дублирующих бронирований.
**Причина:** при сетевых сбоях возможна повторная отправка запроса.
**Альтернатива:** хранить хэш запроса, но UUID проще и эффективнее.
**Статус:** принято

---

### ADR-2: Circuit Breaker для взаимодействия с Hotel Service

**Решение:** использовать `@CircuitBreaker` из Resilience4j с `fallbackMethod`.
**Причина:** обеспечить устойчивость при недоступности Hotel Service.
**Статус:** принято
**Fallback:** создается запись со статусом `CANCELLED`.

---

### ADR-3: Безопасность и аутентификация

**Решение:** Spring Security + JWT.
**Причина:** централизованная авторизация через Gateway, масштабируемость.
**Статус:** принято
**Альтернатива:** Keycloak или OAuth2 — избыточны для учебного проекта.

---

### ADR-4: Хранение пользователей в локальной БД

**Решение:** использовать локальную таблицу `users` вместо отдельного Auth Service.
**Причина:** упрощает архитектуру на этапе демонстрации.
**Статус:** принято

---

### ADR-5: RestTemplate вместо WebClient

**Решение:** использовать `RestTemplate` для простоты.
**Причина:** минимальная конфигурация и синхронное взаимодействие.
**Альтернатива:** WebClient — предпочтителен для реактивных сервисов.
**Статус:** принято

---

## Roadmap

* Добавить Kafka для событий "BookingCreated"
* Перейти на WebClient
* Вынести аутентификацию в отдельный Auth Service
* Добавить интеграционные тесты
* Подключить Prometheus и Grafana
