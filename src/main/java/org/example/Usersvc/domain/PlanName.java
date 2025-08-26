package org.example.Usersvc.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 구독 플랜 이름 Enum
 * 
 * Free와 Pro 두 가지 플랜을 지원합니다.
 * - FREE: 무료 플랜 (월 100회 API 호출, 기본 지원)
 * - PRO: 유료 플랜 (월 10,000회 API 호출, 우선 지원, 고급 분석)
 */
public enum PlanName {
    FREE("Free", 0.00, 100, 10, 100, 1000, 0, 3, 3), // CustomAPI 기능 Custom API Service로 이관, 공유 기능 비활성화
    PRO("Pro", 22.00, 10000, 60, 3600, 86400, 0, 20, 20); // CustomAPI 기능 Custom API Service로 이관, 공유 기능 비활성화

    private final String planName;
    private final double price;
    private final int maxApiCount;
    private final int rateLimitPerMinute;
    private final int rateLimitPerHour;
    private final int rateLimitPerDay;
    private final int maxCustomApiCount;
    private final int maxSharedApiCount;
    private final int maxDataBundleCount;

    PlanName(String planName, double price, int maxApiCount, 
             int rateLimitPerMinute, int rateLimitPerHour, int rateLimitPerDay,
             int maxCustomApiCount, int maxSharedApiCount, int maxDataBundleCount) {
        this.planName = planName;
        this.price = price;
        this.maxApiCount = maxApiCount;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.rateLimitPerHour = rateLimitPerHour;
        this.rateLimitPerDay = rateLimitPerDay;
        this.maxCustomApiCount = maxCustomApiCount;
        this.maxSharedApiCount = maxSharedApiCount;
        this.maxDataBundleCount = maxDataBundleCount;
    }

    @JsonValue
    public String getPlanName() {
        return planName;
    }

    public double getPrice() {
        return price;
    }

    public int getMaxApiCount() {
        return maxApiCount;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public int getRateLimitPerHour() {
        return rateLimitPerHour;
    }

    public int getRateLimitPerDay() {
        return rateLimitPerDay;
    }

    public int getMaxCustomApiCount() {
        return maxCustomApiCount;
    }

    public int getMaxSharedApiCount() {
        return maxSharedApiCount;
    }

    public int getMaxDataBundleCount() {
        return maxDataBundleCount;
    }

    /**
     * 플랜명으로부터 PlanName을 찾습니다.
     * 
     * @param planName 플랜명 ("Free" 또는 "Pro")
     * @return 해당하는 PlanName
     * @throws IllegalArgumentException 지원하지 않는 플랜명인 경우
     */
    @JsonCreator
    public static PlanName fromString(String planName) {
        if (planName == null) {
            throw new IllegalArgumentException("플랜명은 null일 수 없습니다.");
        }
        
        for (PlanName type : PlanName.values()) {
            if (type.planName.equalsIgnoreCase(planName)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("지원하지 않는 플랜입니다: " + planName + 
                                         ". 지원 플랜: Free, Pro");
    }

    /**
     * 대소문자 구분 없이 플랜명 매칭
     */
    public boolean matchesName(String planName) {
        return this.planName.equalsIgnoreCase(planName);
    }

    @Override
    public String toString() {
        return planName;
    }
}