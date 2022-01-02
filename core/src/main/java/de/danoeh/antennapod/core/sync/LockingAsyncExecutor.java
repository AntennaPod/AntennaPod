package de.danoeh.antennapod.core.sync;

import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

public class LockingAsyncExecutor {

    static final ReentrantLock lock = new ReentrantLock();

    /**
     * Take the lock and execute runnable (to prevent changes to preferences being lost when enqueueing while sync is
     * in progress). If the lock is free, the runnable is directly executed in the calling thread to prevent overhead.
     */
    public static void executeLockedAsync(Runnable runnable) {
        if (lock.tryLock()) {
            try {
                runnable.run();
            } finally {
                lock.unlock();
            }
        } else {
            Completable.fromRunnable(() -> {
                lock.lock();
                try {
                    runnable.run();
                } finally {
                    lock.unlock();
                }
            }).subscribeOn(Schedulers.io())
                    .subscribe();
        }
    }
}
