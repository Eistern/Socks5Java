package net.fit.activities;

import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@RequiredArgsConstructor
public class AnnouncementActivity extends VaryingActivity implements Runnable {
    private final DatagramSocket socket;
    private final GameModel model;
    private final NetworkManager networkManager;

    @Override
    public void run() {
        try {
            SnakesProto.GameMessage.AnnouncementMsg.Builder builder = SnakesProto.GameMessage.AnnouncementMsg.newBuilder();
            DatagramPacket packet = new DatagramPacket(new byte[0], 0);
            packet.setAddress(InetAddress.getByName("239.192.0.4"));
            packet.setPort(9192);
            builder.setConfig(model.getConfig());
            while (true) {
                synchronized (activityLock) {
                    while (!activityLock.get()) {
                        activityLock.wait();
                    }
                }
                //IF NEED CAN_JOIN, ADD builder.setCanJoin(model.canJoin()); (not recommended)
                builder.setPlayers(model.getPlayers());
                byte[] data = SnakesProto.GameMessage.newBuilder()
                            .setAnnouncement(builder.build())
                            .setMsgSeq(networkManager.getSequenceNum())
                            .build().toByteArray();
                packet.setData(data);
                packet.setLength(data.length);
                socket.send(packet);
                Thread.sleep(1000);
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
