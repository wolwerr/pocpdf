package com.poc.pocpdf.application.port.out;

import java.time.Duration;
import java.util.concurrent.Callable;

public interface LockPort {
    <T> T withLock(String lockKey, Duration ttl, Callable<T> action);
}
