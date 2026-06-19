package com.attendance.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    @Test
    void isExpiredTrueWhenExpirationInPast() {
        Session session = new Session();
        session.setExpirationTime(LocalDateTime.now().minusMinutes(1));
        assertThat(session.isExpired()).isTrue();
    }

    @Test
    void isExpiredFalseWhenExpirationInFuture() {
        Session session = new Session();
        session.setExpirationTime(LocalDateTime.now().plusMinutes(10));
        assertThat(session.isExpired()).isFalse();
    }
}
