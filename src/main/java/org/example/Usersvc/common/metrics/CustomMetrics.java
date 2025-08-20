package org.example.Usersvc.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 커스텀 비즈니스 메트릭
 * 
 * UserService의 핵심 비즈니스 지표들을 Prometheus로 수집합니다.
 */
@Component
public class CustomMetrics {

    private final MeterRegistry meterRegistry;
    
    // 카운터 메트릭
    private final Counter userCreatedCounter;
    private final Counter userDeletedCounter;
    private final Counter subscriptionCreatedCounter;
    private final Counter subscriptionCancelledCounter;
    private final Counter apiKeyRegisteredCounter;
    private final Counter apiKeyDeletedCounter;
    private final Counter customApiCreatedCounter;
    private final Counter apiSharedCounter;
    private final Counter planLimitExceededCounter;
    private final Counter paymentSuccessCounter;
    private final Counter paymentFailureCounter;
    private final Counter authenticationFailureCounter;
    private final Counter unauthorizedAccessCounter;

    // 타이머 메트릭
    private final Timer userCreationTimer;
    private final Timer subscriptionCreationTimer;
    private final Timer apiKeyRegistrationTimer;
    private final Timer paymentProcessingTimer;
    private final Timer databaseQueryTimer;

    // 게이지 메트릭을 위한 원자적 값들
    private final AtomicInteger activeUsersCount = new AtomicInteger(0);
    private final AtomicInteger activeSubscriptionsCount = new AtomicInteger(0);
    private final AtomicInteger totalApiKeysCount = new AtomicInteger(0);
    private final AtomicInteger totalCustomApisCount = new AtomicInteger(0);
    private final AtomicLong totalRevenueAmount = new AtomicLong(0);

    // 플랜별 사용자 수 추적
    private final ConcurrentHashMap<String, AtomicInteger> planUserCounts = new ConcurrentHashMap<>();

    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // 카운터 메트릭 초기화
        this.userCreatedCounter = Counter.builder("userservice.users.created.total")
                .description("Total number of users created")
                .register(meterRegistry);

        this.userDeletedCounter = Counter.builder("userservice.users.deleted.total")
                .description("Total number of users deleted")
                .register(meterRegistry);

        this.subscriptionCreatedCounter = Counter.builder("userservice.subscriptions.created.total")
                .description("Total number of subscriptions created")
                .tag("plan_type", "all")
                .register(meterRegistry);

        this.subscriptionCancelledCounter = Counter.builder("userservice.subscriptions.cancelled.total")
                .description("Total number of subscriptions cancelled")
                .register(meterRegistry);

        this.apiKeyRegisteredCounter = Counter.builder("userservice.apikeys.registered.total")
                .description("Total number of API keys registered")
                .register(meterRegistry);

        this.apiKeyDeletedCounter = Counter.builder("userservice.apikeys.deleted.total")
                .description("Total number of API keys deleted")
                .register(meterRegistry);

        this.customApiCreatedCounter = Counter.builder("userservice.customapis.created.total")
                .description("Total number of custom APIs created")
                .register(meterRegistry);

        this.apiSharedCounter = Counter.builder("userservice.apis.shared.total")
                .description("Total number of APIs shared")
                .register(meterRegistry);

        this.planLimitExceededCounter = Counter.builder("userservice.planlimits.exceeded.total")
                .description("Total number of plan limit exceeded attempts")
                .register(meterRegistry);

        this.paymentSuccessCounter = Counter.builder("userservice.payments.success.total")
                .description("Total number of successful payments")
                .register(meterRegistry);

        this.paymentFailureCounter = Counter.builder("userservice.payments.failure.total")
                .description("Total number of failed payments")
                .register(meterRegistry);

        this.authenticationFailureCounter = Counter.builder("userservice.auth.failure.total")
                .description("Total number of authentication failures")
                .register(meterRegistry);

        this.unauthorizedAccessCounter = Counter.builder("userservice.access.unauthorized.total")
                .description("Total number of unauthorized access attempts")
                .register(meterRegistry);

        // 타이머 메트릭 초기화
        this.userCreationTimer = Timer.builder("userservice.users.creation.duration")
                .description("Time taken to create a user")
                .register(meterRegistry);

        this.subscriptionCreationTimer = Timer.builder("userservice.subscriptions.creation.duration")
                .description("Time taken to create a subscription")
                .register(meterRegistry);

        this.apiKeyRegistrationTimer = Timer.builder("userservice.apikeys.registration.duration")
                .description("Time taken to register an API key")
                .register(meterRegistry);

        this.paymentProcessingTimer = Timer.builder("userservice.payments.processing.duration")
                .description("Time taken to process a payment")
                .register(meterRegistry);

        this.databaseQueryTimer = Timer.builder("userservice.database.query.duration")
                .description("Time taken for database queries")
                .register(meterRegistry);

        // 게이지 메트릭 초기화
        Gauge.builder("userservice.users.active.count", activeUsersCount, AtomicInteger::doubleValue)
                .description("Current number of active users")
                .register(meterRegistry);

        Gauge.builder("userservice.subscriptions.active.count", activeSubscriptionsCount, AtomicInteger::doubleValue)
                .description("Current number of active subscriptions")
                .register(meterRegistry);

        Gauge.builder("userservice.apikeys.total.count", totalApiKeysCount, AtomicInteger::doubleValue)
                .description("Current total number of API keys")
                .register(meterRegistry);

        Gauge.builder("userservice.customapis.total.count", totalCustomApisCount, AtomicInteger::doubleValue)
                .description("Current total number of custom APIs")
                .register(meterRegistry);

        Gauge.builder("userservice.revenue.total.amount", totalRevenueAmount, AtomicLong::doubleValue)
                .description("Total revenue amount in cents")
                .register(meterRegistry);

        // 플랜별 사용자 수 게이지
        initializePlanGauges();
    }

    // === 카운터 메트릭 증가 메서드들 ===

    public void incrementUserCreated() {
        userCreatedCounter.increment();
        activeUsersCount.incrementAndGet();
    }

    public void incrementUserDeleted() {
        userDeletedCounter.increment();
        activeUsersCount.decrementAndGet();
    }

    public void incrementSubscriptionCreated(String planType) {
        subscriptionCreatedCounter.increment();
        activeSubscriptionsCount.incrementAndGet();
        incrementPlanUserCount(planType);
    }

    public void incrementSubscriptionCancelled(String planType) {
        subscriptionCancelledCounter.increment();
        activeSubscriptionsCount.decrementAndGet();
        decrementPlanUserCount(planType);
    }

    public void incrementApiKeyRegistered() {
        apiKeyRegisteredCounter.increment();
        totalApiKeysCount.incrementAndGet();
    }

    public void incrementApiKeyDeleted() {
        apiKeyDeletedCounter.increment();
        totalApiKeysCount.decrementAndGet();
    }

    public void incrementCustomApiCreated() {
        customApiCreatedCounter.increment();
        totalCustomApisCount.incrementAndGet();
    }

    public void incrementApiShared() {
        apiSharedCounter.increment();
    }

    public void incrementPlanLimitExceeded(String limitType) {
        Counter.builder("userservice.planlimits.exceeded.total")
                .description("Total number of plan limit exceeded attempts")
                .tag("limit_type", limitType)
                .register(meterRegistry)
                .increment();
    }

    public void incrementPaymentSuccess(double amount) {
        paymentSuccessCounter.increment();
        totalRevenueAmount.addAndGet((long) (amount * 100)); // 센트 단위로 저장
    }

    public void incrementPaymentFailure(String reason) {
        Counter.builder("userservice.payments.failure.total")
                .description("Total number of failed payments")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void incrementAuthenticationFailure(String reason) {
        Counter.builder("userservice.auth.failure.total")
                .description("Total number of authentication failures")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void incrementUnauthorizedAccess(String resource) {
        Counter.builder("userservice.access.unauthorized.total")
                .description("Total number of unauthorized access attempts")
                .tag("resource", resource)
                .register(meterRegistry)
                .increment();
    }

    // === 타이머 메트릭 메서드들 ===

    public Timer.Sample startUserCreationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordUserCreationTime(Timer.Sample sample) {
        sample.stop(userCreationTimer);
    }

    public Timer.Sample startSubscriptionCreationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordSubscriptionCreationTime(Timer.Sample sample) {
        sample.stop(subscriptionCreationTimer);
    }

    public Timer.Sample startApiKeyRegistrationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordApiKeyRegistrationTime(Timer.Sample sample) {
        sample.stop(apiKeyRegistrationTimer);
    }

    public Timer.Sample startPaymentProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPaymentProcessingTime(Timer.Sample sample) {
        sample.stop(paymentProcessingTimer);
    }

    public Timer.Sample startDatabaseQueryTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDatabaseQueryTime(Timer.Sample sample) {
        sample.stop(databaseQueryTimer);
    }

    // === 게이지 메트릭 업데이트 메서드들 ===

    public void updateActiveUsersCount(int count) {
        activeUsersCount.set(count);
    }

    public void updateActiveSubscriptionsCount(int count) {
        activeSubscriptionsCount.set(count);
    }

    public void updateTotalApiKeysCount(int count) {
        totalApiKeysCount.set(count);
    }

    public void updateTotalCustomApisCount(int count) {
        totalCustomApisCount.set(count);
    }

    public void updateTotalRevenue(long amountInCents) {
        totalRevenueAmount.set(amountInCents);
    }

    // === 플랜별 사용자 수 관리 ===

    private void initializePlanGauges() {
        for (String planType : new String[]{"FREE", "PRO"}) {
            AtomicInteger planCount = new AtomicInteger(0);
            planUserCounts.put(planType, planCount);
            
            Gauge.builder("userservice.users.by.plan.count", planCount, AtomicInteger::doubleValue)
                    .description("Number of users by plan type")
                    .tag("plan_type", planType)
                    .register(meterRegistry);
        }
    }

    private void incrementPlanUserCount(String planType) {
        planUserCounts.computeIfAbsent(planType, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    private void decrementPlanUserCount(String planType) {
        AtomicInteger count = planUserCounts.get(planType);
        if (count != null && count.get() > 0) {
            count.decrementAndGet();
        }
    }

    public void updatePlanUserCount(String planType, int count) {
        planUserCounts.computeIfAbsent(planType, k -> new AtomicInteger(0))
                .set(count);
    }

    // === 비즈니스 메트릭 계산 메서드들 ===

    /**
     * 구독 전환율 계산을 위한 메트릭 기록
     */
    public void recordConversionRate(String fromPlan, String toPlan) {
        Counter.builder("userservice.conversions.total")
                .description("Plan conversion events")
                .tag("from_plan", fromPlan)
                .tag("to_plan", toPlan)
                .register(meterRegistry)
                .increment();
    }

    /**
     * API 사용량 분포 기록
     */
    public void recordApiUsageDistribution(String planType, int apiCount, int usageLevel) {
        AtomicInteger apiCountGauge = new AtomicInteger(apiCount);
        Gauge.builder("userservice.api.usage.distribution", apiCountGauge, AtomicInteger::doubleValue)
                .description("API usage distribution by plan")
                .tag("plan_type", planType)
                .tag("usage_level", String.valueOf(usageLevel))
                .register(meterRegistry);
    }
}