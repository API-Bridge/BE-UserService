package org.example.Usersvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.Usersvc.domain.CustomApi;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiSharingValidationService {

    public int getMaxDataCountForPlan(String planType) {
        switch (planType) {
            case "FREE":
                return 3;
            case "PRO":
                return 20;
            case "ENTERPRISE":
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }

    public boolean validateApiForSharing(CustomApi api, String planType) {
        if (api == null) {
            return false;
        }
        // 새 스키마에서는 모든 유효한 API를 공유할 수 있음
        return api.isValid();
    }
}