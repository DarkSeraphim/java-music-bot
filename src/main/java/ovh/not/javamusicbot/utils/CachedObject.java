package ovh.not.javamusicbot.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;

public class CachedObject<T> {
    
    private final Consumer<Consumer<T>> updater;
    
    private AtomicBoolean busy = new AtomicBoolean();
    
    private final long expiry;
    
    private volatile T value;
    
    private volatile long expiresAt;
    
    private final List<Consumer<T>> waiting = new ArrayList<>();
    
    
    public CachedObject(Consumer<Consumer<T>> updater, long expiry) {
        this.updater = updater;
        this.expiry = expiry;
    }
    
    public void getAsync(Consumer<T> consumer) {
        if (this.value != null || this.expiresAt > System.currentTimeMillis()) {
            consumer.accept(this.value);
            return;
        }
        
        synchronized (this) {
            if (this.value != null || this.expiresAt > System.currentTimeMillis()) {
                consumer.accept(this.value);
                return;
            }
            waiting.add(consumer);
            
            if (busy.compareAndSet(false, true)) {
                updater.accept(value -> {
                    synchronized (this) {
                        this.value = value;
                        this.expiresAt = System.currentTimeMillis() + this.expiry;
                        busy.set(false);
                        waiting.forEach(waiter -> waiter.accept(this.value));
                        waiting.clear();
                    }
                });
            }
        }
        
    }
}