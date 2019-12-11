package net.fit.activities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.gui.error.ErrorBox;
import net.fit.proto.SnakesProto;
import net.fit.thread.ThreadManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class DatagramListener implements Runnable {
    private final GameModel model;
    private final NetworkManager networkManager;
    private final DatagramSocket socket;
    private final ThreadManager threadManager;
    @Getter private Map<Integer, SnakesProto.Direction> recentDirections = new HashMap<>();

    @Override
    public void run() {
        byte[] data = new byte[2048];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        SnakesProto.GameMessage.AckMsg.Builder ackBuilder = SnakesProto.GameMessage.AckMsg.newBuilder();
        SnakesProto.GameMessage.Builder ackMessageBuilder = SnakesProto.GameMessage.newBuilder().setAck(ackBuilder);
        SnakesProto.GameMessage message;
        while (true) {
            try {
                socket.receive(packet);
                message = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                switch (message.getTypeCase()) {
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
                            SnakesProto.GameMessage.RoleChangeMsg changeMsg = model.addPlayer(message.getJoin().getName(), packet.getPort(), packet.getAddress().getHostAddress());
                            if (changeMsg != null) {
                                networkManager.commit(SnakesProto.GameMessage.newBuilder()
                                        .setRoleChange(changeMsg)
                                        .setMsgSeq(networkManager.getSequenceNum())
                                        .build(), packet.getSocketAddress());
                            }
                        }
                    case ACK:
                        networkManager.confirm(message.getMsgSeq());
                        break;
                    case STATE:
                        model.updateState(message.getState().getState(), (InetSocketAddress) packet.getSocketAddress());
                        if (model.getOwnId() == -1) {
                            model.resolveOwnId();
                        }
                        break;
                    case STEER:
                        int id = model.idByIpAndPort(packet.getAddress().getHostAddress(), packet.getPort());
                        recentDirections.put(id, message.getSteer().getDirection());
                        break;
                    case ERROR:
                        ErrorBox.showError(message.getError().getErrorMessage());
                        break;
                    case PING:
                        System.out.println("GOT PING FROM " + packet.getSocketAddress());
                        break;
                    case ROLE_CHANGE:
                        switch (message.getRoleChange().getReceiverRole()) {
                            case MASTER:
                                boolean masterModel = model.becomeMaster(message.getSenderId(), packet.getSocketAddress());
                                if (masterModel) {
                                    SnakesProto.GamePlayer deputy = model.reelectDeputy();
                                    if (deputy != null) {
                                        networkManager.commit(
                                                SnakesProto.GameMessage.newBuilder()
                                                        .setSenderId(model.getOwnId())
                                                        .setReceiverId(deputy.getId())
                                                        .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                                                .setReceiverRole(SnakesProto.NodeRole.DEPUTY)
                                                                .setSenderRole(SnakesProto.NodeRole.MASTER)
                                                                .build())
                                                        .build(), new InetSocketAddress(deputy.getIpAddress(), deputy.getPort()));
                                    }
                                    threadManager.activateMaster();
                                }
                                break;
                            case NORMAL:
                                if (message.getRoleChange().getSenderRole() == SnakesProto.NodeRole.MASTER)
                                    model.setHost((InetSocketAddress) packet.getSocketAddress());
                            case DEPUTY:
                            case VIEWER:
                                threadManager.activateClient();
                                break;
                            default:
                                System.out.println("Got :" + message);
                        }
                        model.setRole(message.getRoleChange().getReceiverRole());
                        break;
                    case TYPE_NOT_SET:
                    default:
                        System.err.println("Received unknown type :" + message);
                }
                if (message.getTypeCase() != SnakesProto.GameMessage.TypeCase.ACK && message.getTypeCase() != SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT) {
                    System.out.println("Sending ack for " + message);
                    networkManager.commit(ackMessageBuilder
                            .setSenderId(model.getOwnId())
                            .setReceiverId(model.idByIpAndPort(packet.getAddress().getHostAddress(), packet.getPort()))
                            .setMsgSeq(message.getMsgSeq())
                            .build(), packet.getSocketAddress());
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
