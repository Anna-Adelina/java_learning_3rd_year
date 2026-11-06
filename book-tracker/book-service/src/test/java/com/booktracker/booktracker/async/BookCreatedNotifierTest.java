package com.booktracker.booktracker.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookCreatedNotifierTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookCreatedNotifier notifier;

    @Test
    void notifyBookCreated_executorAccepts_delegatesToNotificationService() {
        // Act
        notifier.notifyBookCreated("anna", "1984");

        // Assert
        verify(notificationService).sendBookCreatedEmail("anna", "1984");
    }

    @Test
    void notifyBookCreated_executorSaturated_dropsNotificationWithoutFailing() {
        // Arrange
        doThrow(new TaskRejectedException("pool is full"))
                .when(notificationService).sendBookCreatedEmail("anna", "1984");

        // Act + Assert: виняток не виходить назовні
        assertDoesNotThrow(() -> notifier.notifyBookCreated("anna", "1984"));
        verify(notificationService).sendBookCreatedEmail("anna", "1984");
    }
}