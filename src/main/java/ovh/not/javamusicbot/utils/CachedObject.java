package ovh.not.javamusicbot.utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.List;
import java.util.ArrayList;

public class CachedObject<T> {
    
    private final Consumer<BiConsumer<T, Throwable>> updater;
    
    private AtomicBoolean busy = new AtomicBoolean();
    
    private final long expiry;
    
    private volatile T value;
    
    private volatile long expiresAt;
    
    private final List<BiConsumer<T, Throwable>> waiting = new ArrayList<>();

    public CachedObject(Consumer<BiConsumer<T, Throwable>> updater, long expiry) {
        this.updater = updater;
        this.expiry = expiry;
    }

    public boolean hasValue() {
        return this.value != null;
    }

    public T getUncached() {
        return this.value;
    }
    
    public void getAsync(BiConsumer<T, Throwable> consumer) {
        if (this.value != null || this.expiresAt > System.currentTimeMillis()) {
            consumer.accept(this.value, null);
            return;
        }
        
        synchronized (this) {
            if (this.value != null || this.expiresAt > System.currentTimeMillis()) {
                consumer.accept(this.value, null);
                return;
            }
            waiting.add(consumer);
            
            if (busy.compareAndSet(false, true)) {
                updater.accept((value, error) -> {
                    synchronized (this) {
                        this.value = value;
                        if (error == null) {
                            this.expiresAt = System.currentTimeMillis() + this.expiry;
                        }
                        busy.set(false);
                        waiting.forEach(waiter -> waiter.accept(this.value, error));
                        waiting.clear();
                    }
                });
            }
        }
        
    }
}