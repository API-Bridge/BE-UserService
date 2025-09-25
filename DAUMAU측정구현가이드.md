# 활성 사용자(DAU/MAU) 측정 기능 구현 가이드 (Redis HyperLogLog)

이 문서는 Redis의 HyperLogLog 자료구조를 사용하여 서비스의 활성 사용자 수를 효율적으로 측정하는 기능의 구현 방법을 단계별로 안내합니다.

## 목표

- **DAU (Daily Active Users):** 일간 순수 활성 사용자 수 측정
- **WAU (Weekly Active Users):** 주간 순수 활성 사용자 수 측정
- **MAU (Monthly Active Users):** 월간 순수 활성 사용자 수 측정

## 기술 스택

- Java, Spring Boot
- Redis (HyperLogLog)

## 구현 단계

### 1단계: 의존성 추가

`pom.xml` 파일에 `spring-boot-starter-data-redis` 의존성을 추가하여 Spring Boot에서 Redis를 쉽게 사용할 수 있도록 설정합니다.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 2단계: Redis 연결 설정

`src/main/resources/application.properties` 파일에 Redis 서버 접속 정보를 추가합니다.

```properties
# Redis-related properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 3단계: 활성 사용자 추적 서비스 생성

사용자 활동을 기록하고 DAU/MAU를 계산하는 핵심 로직을 담은 `ActiveUserTracker.java` 서비스를 생성합니다. 이 서비스는 Redis HyperLogLog의 모든 연산을 캡슐화합니다.

**생성 파일:** `src/main/java/com/example/demo/metrics/ActiveUserTracker.java`

### 4단계: 핵심 기능에 추적 코드 연동

사용자의 '활성' 상태를 정의하는 핵심 기능(예: 게시물 작성, 로그인 등)이 실행될 때, `ActiveUserTracker`의 `track` 메소드를 호출하여 사용자 활동을 기록합니다.

**수정 파일:** `src/main/java/com/example/demo/controller/PostController.java`

### 5단계: 통계 조회 API 생성

측정된 DAU, WAU, MAU 데이터를 외부에서 쉽게 조회할 수 있도록 `StatisticsController.java`를 생성합니다. 이 컨트롤러는 통계 대시보드나 모니터링 시스템과 연동하는 데 사용될 수 있습니다.

**생성 파일:** `src/main/java/com/example/demo/controller/StatisticsController.java`

## 사용 방법

1.  애플리케이션을 실행합니다.
2.  사용자가 `POST /api/v1/posts` 와 같이 활성 사용자로 간주되는 API를 호출하면, 해당 사용자의 ID가 오늘의 활성 사용자 집합에 기록됩니다.
3.  아래의 API를 통해 통계를 조회할 수 있습니다.
    -   **DAU 조회:** `GET http://localhost:8080/api/stats/dau?date=2023-10-27`
    -   **WAU 조회:** `GET http://localhost:8080/api/stats/wau`
    -   **MAU 조회:** `GET http://localhost:8080/api/stats/mau`

---

*이 문서는 AI 코딩 어시스턴트가 프로젝트 구조와 코드를 이해하고 추가 작업을 수행하는 데 도움을 주기 위해 작성되었습니다.*
