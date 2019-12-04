package net.fit.activities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fit.AnnouncementHolder;
import net.fit.GameModel;
import net.fit.gui.error.ErrorBox;
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
        SnakesProto.GameMessage.AckMsg.Builder ackBuilder = SnakesProto.GameMessage.AckMsg.newBuilder();
        SnakesProto.GameMessage.Builder messageBuilder = SnakesProto.GameMessage.newBuilder().setAck(ackBuilder);
        SnakesProto.GameMessage message;
        while (true) {
            try {
                socket.receive(packet);
                message = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                switch (message.getTypeCase()) {
                    case ANNOUNCEMENT:
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
                    case ACK:
                        networkManager.confirm(message.getMsgSeq());
                        break;
                    case STATE:
                        model.updateState(message.getState().getState());
                        break;
                    case STEER:
                        int id = model.idByIpAndPort(packet.getAddress().getHostAddress(), packet.getPort());
                        recentDirections.put(id, message.getSteer().getDirection());
                        break;
                    case ERROR:
                        ErrorBox.showError(message.getError().getErrorMessage());
                        break;
                    case TYPE_NOT_SET:
                    default:
                        System.err.println("Received unknown type :" + message);
                }
                if (message.getTypeCase() != SnakesProto.GameMessage.TypeCase.ACK && message.getTypeCase() != SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT) {
                    networkManager.commit(messageBuilder.setMsgSeq(message.getMsgSeq()).build(), packet.getSocketAddress());
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
