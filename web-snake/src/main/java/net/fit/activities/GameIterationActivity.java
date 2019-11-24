package net.fit.activities;

import lombok.AllArgsConstructor;
import net.fit.GameModel;

@AllArgsConstructor
public class GameIterationActivity implements Runnable {
    private GameModel model;
    private DatagramListener listener;

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(model.getConfig().getStateDelayMs());
                model.iterateState(listener.getRecentDirections());
            } catch (InterruptedException e) {
                System.err.println("Game iteration activity interrupted");
            }
        }
    }
}
