package com.example.ebookrecommendsystem.ai;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class AiRequestGuardTest {
    static class MutableClock extends Clock {
        long millis;
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
    }
    @Test void perUserWindowExpiresAndOtherUsersHaveOwnQuota() {
        var clock = new MutableClock(); var guard = new AiRequestGuard(clock);
        for (int i = 0; i < 5; i++) guard.acquire(10).close();
        assertEquals(429, assertThrows(AiServiceException.class, () -> guard.acquire(10)).status());
        guard.acquire(20).close();
        clock.millis = 60000;
        guard.acquire(10).close();
    }
    @Test void concurrentLimitAndIdempotentRelease() {
        var guard = new AiRequestGuard();
        var a = guard.acquire(1); var b = guard.acquire(2);
        assertThrows(AiServiceException.class, () -> guard.acquire(3));
        a.close(); a.close();
        var c = guard.acquire(3);
        assertThrows(AiServiceException.class, () -> guard.acquire(4));
        b.close(); c.close();
    }
}
