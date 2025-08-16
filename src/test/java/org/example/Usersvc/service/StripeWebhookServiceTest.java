package org.example.Usersvc.service;

import com.stripe.model.*;
import org.example.Usersvc.domain.User;
import org.example.Usersvc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Stripe 웹훅 처리 서비스 테스트")
class StripeWebhookServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionManagementService subscriptionManagementService;

    @InjectMocks
    private StripeWebhookService stripeWebhookService;

    @Test
    @DisplayName("지원하지 않는 이벤트 타입은 무시해야 한다")
    void handleWebhook_UnsupportedEventType() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("customer.created");

        stripeWebhookService.handleWebhook(mockEvent);

        verify(subscriptionManagementService, never()).handleSubscriptionStatusChange(any(), any());
        verify(subscriptionManagementService, never()).handlePaymentSuccess(any(), any(), any());
        verify(subscriptionManagementService, never()).handlePaymentFailure(any(), any(), any());
    }

    @Test
    @DisplayName("구독 생성 이벤트 타입을 올바르게 식별해야 한다")
    void handleWebhook_SubscriptionCreated() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("customer.subscription.created");
        when(mockEvent.getId()).thenReturn("evt_test_webhook");

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        stripeWebhookService.handleWebhook(mockEvent);

        verify(mockEvent).getDataObjectDeserializer();
    }

    @Test
    @DisplayName("구독 업데이트 이벤트 타입을 올바르게 식별해야 한다")
    void handleWebhook_SubscriptionUpdated() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("customer.subscription.updated");
        when(mockEvent.getId()).thenReturn("evt_test_webhook");

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        stripeWebhookService.handleWebhook(mockEvent);

        verify(mockEvent).getDataObjectDeserializer();
    }

    @Test
    @DisplayName("구독 삭제 이벤트 타입을 올바르게 식별해야 한다")
    void handleWebhook_SubscriptionDeleted() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("customer.subscription.deleted");
        when(mockEvent.getId()).thenReturn("evt_test_webhook");

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        stripeWebhookService.handleWebhook(mockEvent);

        verify(mockEvent).getDataObjectDeserializer();
    }

    @Test
    @DisplayName("결제 성공 이벤트 타입을 올바르게 식별해야 한다")
    void handleWebhook_PaymentSucceeded() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("invoice.payment_succeeded");
        when(mockEvent.getId()).thenReturn("evt_test_webhook");

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        stripeWebhookService.handleWebhook(mockEvent);

        verify(mockEvent).getDataObjectDeserializer();
    }

    @Test
    @DisplayName("결제 실패 이벤트 타입을 올바르게 식별해야 한다")
    void handleWebhook_PaymentFailed() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("invoice.payment_failed");
        when(mockEvent.getId()).thenReturn("evt_test_webhook");

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        stripeWebhookService.handleWebhook(mockEvent);

        verify(mockEvent).getDataObjectDeserializer();
    }

    @Test
    @DisplayName("결제 수단 연결 이벤트 타입을 올바르게 식별해야 한다")
    void handleWebhook_PaymentMethodAttached() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("payment_method.attached");
        when(mockEvent.getId()).thenReturn("evt_test_webhook");

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        stripeWebhookService.handleWebhook(mockEvent);

        verify(mockEvent).getDataObjectDeserializer();
    }

    @Test
    @DisplayName("고객 업데이트 이벤트 타입을 올바르게 식별해야 한다")
    void handleWebhook_CustomerUpdated() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("customer.updated");
        when(mockEvent.getId()).thenReturn("evt_test_webhook");

        EventDataObjectDeserializer mockDeserializer = mock(EventDataObjectDeserializer.class);
        when(mockEvent.getDataObjectDeserializer()).thenReturn(mockDeserializer);
        when(mockDeserializer.getObject()).thenReturn(Optional.empty());

        stripeWebhookService.handleWebhook(mockEvent);

        verify(mockEvent).getDataObjectDeserializer();
    }

    @Test
    @DisplayName("웹훅 처리 시작과 완료 로그가 기록되어야 한다")
    void handleWebhook_LoggingTest() {
        Event mockEvent = mock(Event.class);
        when(mockEvent.getType()).thenReturn("customer.created");
        when(mockEvent.getId()).thenReturn("evt_test_webhook");

        stripeWebhookService.handleWebhook(mockEvent);

        verify(mockEvent, atLeast(2)).getId();
        verify(mockEvent, atLeast(2)).getType();
    }
}