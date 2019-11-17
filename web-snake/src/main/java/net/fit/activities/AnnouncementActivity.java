package net.fit.activities;

import net.fit.GameModel;
import net.fit.proto.SnakesProto;

import java.net.DatagramSocket;

public class AnnouncementActivity implements Runnable {
    private final DatagramSocket socket;
    private final GameModel model;

    public AnnouncementActivity(DatagramSocket socket, GameModel model) {
        this.socket = socket;
        this.model = model;
    }

    @Override
    public void run() {
        SnakesProto.GameMessage.AnnouncementMsg.Builder builder = SnakesProto.GameMessage.AnnouncementMsg.newBuilder();
        builder.setConfig(model.getConfig());
        while (true) {
            try {
                builder.setCanJoin(model.canJoin());
                builder.setPlayers(model.getPlayers());
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
