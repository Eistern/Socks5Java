package net.fit.activities;

import java.util.concurrent.atomic.AtomicBoolean;

public class VaryingActivity {
    final AtomicBoolean activityLock = new AtomicBoolean(true);

    public void resumeActivity() {
        synchronized (activityLock) {
            activityLock.set(true);
            activityLock.notify();
        }
    }

    public void stopActivity() {
        synchronized (activityLock) {
            activityLock.set(false);
        }
    }
}
