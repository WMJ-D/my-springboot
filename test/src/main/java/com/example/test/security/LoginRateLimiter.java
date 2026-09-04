package com.example.test.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录限流器，与 Express 侧 express-rate-limit 行为一致：
 * 每 IP 15 分钟窗口内最多 10 次，仅生产环境启用
 */
@Component
public class LoginRateLimiter {

    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;
    private static final int LIMIT = 10;

    private final Map<String, Deque<Long>> attempts = new ConcurrentHashMap<>();
    private final boolean enabled;

    @Autowired
    public LoginRateLimiter(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        boolean production = false;
        for (String profile : profiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                production = true;
                break;
            }
        }
        this.enabled = production;
    }

    /**
     * @return true 表示超出限流
     */
    public boolean isLimited(String key) {
        if (!enabled) {
            return false;
        }
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            prune(timestamps, now);
            if (timestamps.size() >= LIMIT) {
                return true;
            }
            timestamps.addLast(now);
            return false;
        }
    }

    private void prune(Deque<Long> timestamps, long now) {
        Iterator<Long> iterator = timestamps.iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next() >= WINDOW_MILLIS) {
                iterator.remove();
            } else {
                break;
            }
        }
    }
}
