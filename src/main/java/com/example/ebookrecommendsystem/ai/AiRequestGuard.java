package com.example.ebookrecommendsystem.ai;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/** Small single-instance course deployment: five model requests/minute/user, two globally in flight. */
@Component
public class AiRequestGuard {
    private record Window(long started, int count) {}
    private final Clock clock;
    private final Map<Integer, Window> windows = new HashMap<>();
    private final Semaphore concurrent = new Semaphore(2);
    public AiRequestGuard() { this(Clock.systemUTC()); }
    AiRequestGuard(Clock clock) { this.clock = clock; }

    public synchronized Lease acquire(int userId) {
        long now = clock.millis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().started() >= 60000);
        Window window = windows.getOrDefault(userId, new Window(now, 0));
        if (window.count() >= 5 || (!windows.containsKey(userId) && windows.size() >= 1000)) {
            throw new AiServiceException(429, "提问较频繁，请一分钟后再试");
        }
        if (!concurrent.tryAcquire()) throw new AiServiceException(429, "学习助手正在回答其他问题，请稍后再试");
        windows.put(userId, new Window(window.started(), window.count() + 1));
        return new Lease();
    }
    public final class Lease implements AutoCloseable {
        private final AtomicBoolean closed = new AtomicBoolean();
        @Override public void close() { if (closed.compareAndSet(false, true)) concurrent.release(); }
    }
}
