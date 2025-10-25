package com.paklog.inventory.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainExceptionTest {

    @Test
    @DisplayName("Should create domain exception with message")
    void constructor_WithMessage_CreatesDomainException() {
        // Arrange
        String message = "Test error message";

        // Act
        DomainException exception = new TestDomainException(message);

        // Assert
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("Should create domain exception with message and cause")
    void constructor_WithMessageAndCause_CreatesDomainException() {
        // Arrange
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");

        // Act
        DomainException exception = new TestDomainException(message, cause);

        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should be a RuntimeException")
    void domainException_IsRuntimeException() {
        // Arrange & Act
        DomainException exception = new TestDomainException("Test message");

        // Assert
        assertInstanceOf(RuntimeException.class, exception);
    }

    // Test implementation of DomainException
    private static class TestDomainException extends DomainException {
        public TestDomainException(String message) {
            super(message);
        }

        public TestDomainException(String message, Throwable cause) {
            super(message, cause);
        

}
}
}
