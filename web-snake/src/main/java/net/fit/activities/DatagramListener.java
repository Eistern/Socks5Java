package net.fit.activities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fit.AnnouncementHolder;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class DatagramListener implements Runnable {
    private final GameModel model;
    private final NetworkManager networkManager;
    private final MulticastSocket socket;
    private final AnnouncementHolder announcementHolder;
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
                message = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                switch (message.getTypeCase()) {
                    case ANNOUNCEMENT:
                        System.out.println("Got announce");
                        announcementHolder.addAnnouncement(message.getAnnouncement(), (InetSocketAddress) packet.getSocketAddress());
                        break;
                    case JOIN:
                        if (!model.canJoin(packet.getAddress().getHostAddress(), packet.getPort())) {
                            networkManager.commit(SnakesProto.GameMessage.newBuilder()
                                    .setError(SnakesProto.GameMessage.ErrorMsg.newBuilder()
                                            .setErrorMessage("Field is full OR you have already joined")
                                            .build())
                                    .setMsgSeq(networkManager.getSequenceNum())
                                    .build(), packet.getSocketAddress());
                        }
                        else {
                            model.addPlayer(message.getJoin().getName(), packet.getPort(), packet.getAddress().getHostAddress());
                        }
                        System.out.println("JOIN received");
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
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
