package net.fit.activities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class DatagramListener implements Runnable {
    private final GameModel model;
    private final NetworkManager networkManager;
    private final MulticastSocket socket;
    @Getter private Map<Integer, SnakesProto.Direction> recentDirections = new HashMap<>();

    @Override
    public void run() {
        try {
            socket.joinGroup(InetAddress.getByName("239.192.0.4"));
        } catch (IOException e) {
            System.err.println("Unknown host name for group address, can't receive Announcement messages");
        }
        byte[] data = new byte[2048];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        SnakesProto.GameMessage message;
        while (true) {
            try {
                socket.receive(packet);
                message = SnakesProto.GameMessage.parseFrom(packet.getData());
                switch (message.getTypeCase()) {
                    case ACK:
                        networkManager.confirm(message.getMsgSeq());
                        break;
                    case STATE:
                        model.updateState(message.getState().getState());
                        break;
                    case STEER:
                        int id = model.idByIpAndPort(packet.getAddress().getHostName(), packet.getPort());
                        recentDirections.put(id, message.getSteer().getDirection());
                    case TYPE_NOT_SET:
                    default:
                        System.err.println("Received unknown type :" + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
