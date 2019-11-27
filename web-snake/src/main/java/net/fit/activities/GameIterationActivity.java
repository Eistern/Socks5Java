package net.fit.activities;

import lombok.AllArgsConstructor;
import net.fit.GameModel;

@AllArgsConstructor
public class GameIterationActivity extends VaryingActivity implements Runnable {
    private GameModel model;
    private DatagramListener listener;

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (activityLock) {
                    while (!activityLock.get()) {
                        activityLock.wait();
                    }
                }
                Thread.sleep(model.getConfig().getStateDelayMs());
                model.iterateState(listener.getRecentDirections());
            } catch (InterruptedException e) {
                System.err.println("Game iteration activity interrupted");
            }
        }
    }
}
