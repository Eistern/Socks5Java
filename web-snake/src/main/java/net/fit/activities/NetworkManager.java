package net.fit.activities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;
import net.fit.thread.ThreadManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public class NetworkManager implements Runnable {
    @AllArgsConstructor
    private static class Message {
        private SnakesProto.GameMessage message;
        private SocketAddress address;
        private long sequence;
        private Date timestamp;
    }

    private long sequenceNum = 0;
    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private final ThreadManager threadManager;
    @Getter private PingActivity pingActivity = new PingActivity();
    private final ResendActivity resendActivity = new ResendActivity();
    private final DisconnectActivity disconnectActivity = new DisconnectActivity();
    private final DatagramSocket socket;
    private final GameModel model;

    public synchronized long getSequenceNum() {
        return sequenceNum++;
    }

    public void commit(SnakesProto.GameMessage message, SocketAddress address) throws InterruptedException {
        messageQueue.put(new Message(message, address, 0, null));
    }

    public void removePendingUsers(List<SnakesProto.GamePlayer> users) {
        resendActivity.removePendingUsers(users);
    }

    public void updateMessage(InetSocketAddress address) {
        disconnectActivity.updateMessage(address);
    }

    public void confirm(long sequence, SocketAddress socketAddress) {
        resendActivity.confirm(sequence, socketAddress);
    }

    @Override
    public void run() {
        Thread resendThread = new Thread(resendActivity, "Resend");
        resendThread.start();
        Thread disconnectThread = new Thread(disconnectActivity, "Disconnect");
        disconnectThread.start();

        DatagramPacket packet = new DatagramPacket(new byte[0], 0);
        while (true) {
            try {
                Message nextMessage = messageQueue.take();
                if (nextMessage.address == null)
                    continue;
                System.out.println("NOW SENDING " + nextMessage.message + " TO " + nextMessage.address);
                nextMessage.sequence = nextMessage.message.getMsgSeq();
                nextMessage.timestamp = new Date();
                byte[] data = nextMessage.message.toBuilder().setMsgSeq(nextMessage.sequence).build().toByteArray();
                packet.setData(data);
                packet.setLength(data.length);
                packet.setSocketAddress(nextMessage.address);
                socket.send(packet);
                pingActivity.notifyMessage();
                if (nextMessage.message.getTypeCase() != SnakesProto.GameMessage.TypeCase.ACK) {
                    resendActivity.addPending(nextMessage);
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class DisconnectActivity implements Runnable {
        private Map<InetSocketAddress, Date> lastMessage = new HashMap<>();

        private void updateMessage(InetSocketAddress address) {
            lastMessage.put(address, new Date());
        }

        @Override
        public void run() {
            List<SnakesProto.GamePlayer> removedPlayers = new ArrayList<>();
            SnakesProto.GameMessage.Builder messageBuilder = SnakesProto.GameMessage.newBuilder();
            SnakesProto.GameMessage.RoleChangeMsg.Builder roleChangeBuilder = SnakesProto.GameMessage.RoleChangeMsg.newBuilder();
            while (true) {
                try {
                    long timeout = model.getConfig().getNodeTimeoutMs();
                    Thread.sleep(timeout);
                    Date checkDate = new Date();
                    SnakesProto.NodeRole currentRole = model.getRole();
                    if (currentRole == SnakesProto.NodeRole.VIEWER)
                        continue;
                    removedPlayers.clear();

                    List<SnakesProto.GamePlayer> players;
                    if (currentRole == SnakesProto.NodeRole.MASTER) {
                         players = model.getPlayers().getPlayersList();
                    }
                    else {
                        InetSocketAddress hostAddr = model.getHostAddr();
                        SnakesProto.GamePlayer master = model.getFirstOfRole(SnakesProto.NodeRole.MASTER);
                        if (master == null)
                            continue;
                        players = Collections.singletonList(master.toBuilder()
                                .setIpAddress(hostAddr.getAddress().getHostAddress())
                                .setPort(hostAddr.getPort())
                                .build()
                        );
                    }

                    for (SnakesProto.GamePlayer player : players) {
                        InetSocketAddress playerAddress = new InetSocketAddress(player.getIpAddress(), player.getPort());
                        if (player.getRole() != currentRole && checkDate.getTime() - lastMessage.getOrDefault(playerAddress, new Date(0)).getTime() > timeout) {
                            removedPlayers.add(player);
                        }
                    }

                    boolean needDeputy = false;
                    NetworkManager.this.resendActivity.removePendingUsers(removedPlayers);
                    if (currentRole == SnakesProto.NodeRole.MASTER) {
                        model.removePlayers(removedPlayers);

                        for (SnakesProto.GamePlayer removedPlayer : removedPlayers) {
                            if (removedPlayer.getRole() == SnakesProto.NodeRole.DEPUTY) {
                                needDeputy = true;
                            }
                            if (removedPlayer.getRole() != SnakesProto.NodeRole.VIEWER) {
                                NetworkManager.this.commit(messageBuilder
                                        .setMsgSeq(getSequenceNum())
                                        .setRoleChange(roleChangeBuilder
                                                .setSenderRole(currentRole)
                                                .setReceiverRole(SnakesProto.NodeRole.VIEWER)
                                        )
                                        .setSenderId(model.getOwnId())
                                        .setReceiverId(removedPlayer.getId())
                                        .build(), new InetSocketAddress(removedPlayer.getIpAddress(), removedPlayer.getPort()));
                            }
                        }
                    }
                    if (currentRole == SnakesProto.NodeRole.NORMAL && !removedPlayers.isEmpty()) {
                        SnakesProto.GamePlayer deputy = model.getFirstOfRole(SnakesProto.NodeRole.DEPUTY);
                        InetSocketAddress deputyAddr = new InetSocketAddress(deputy.getIpAddress(), deputy.getPort());
                        model.setHostAddr(deputyAddr);
                        NetworkManager.this.resendActivity.updateMaster(deputyAddr);
                    }
                    if (currentRole == SnakesProto.NodeRole.DEPUTY && !removedPlayers.isEmpty()) {
                        model.removePlayers(removedPlayers);
                        model.becomeMaster(-1, null);
                        currentRole = SnakesProto.NodeRole.MASTER;
                        needDeputy = true;
                        NetworkManager.this.threadManager.activateMaster();
                        List<SnakesProto.GamePlayer> updatedPlayers = model.getPlayers().getPlayersList();
                        for (SnakesProto.GamePlayer updatedPlayer : updatedPlayers) {
                            NetworkManager.this.commit(messageBuilder
                                    .setMsgSeq(getSequenceNum())
                                    .setRoleChange(roleChangeBuilder
                                            .setSenderRole(SnakesProto.NodeRole.MASTER)
                                            .setReceiverRole(updatedPlayer.getRole())
                                    )
                                    .setSenderId(model.getOwnId())
                                    .setReceiverId(updatedPlayer.getId())
                                    .build(), new InetSocketAddress(updatedPlayer.getIpAddress(), updatedPlayer.getPort()));
                        }
                    }

                    if (needDeputy) {
                        SnakesProto.GamePlayer nextDeputy = model.reelectDeputy();
                        if (nextDeputy != null) {
                            NetworkManager.this.commit(messageBuilder
                                    .setMsgSeq(getSequenceNum())
                                    .setRoleChange(roleChangeBuilder
                                            .setSenderRole(currentRole)
                                            .setReceiverRole(SnakesProto.NodeRole.DEPUTY)
                                    )
                                    .setSenderId(model.getOwnId())
                                    .setReceiverId(nextDeputy.getId())
                                    .build(), new InetSocketAddress(nextDeputy.getIpAddress(), nextDeputy.getPort()));
                        }
                    }

                } catch (InterruptedException e) {
                    System.err.println("Disconnect thread was interrupted....");
                }
            }
        }
    }

    private class ResendActivity implements Runnable {
        private BlockingQueue<Message> pendingRequests = new LinkedBlockingQueue<>();

        private void removePendingUsers(List<SnakesProto.GamePlayer> players) {
            players.forEach(player -> removePending(new InetSocketAddress(player.getIpAddress(), player.getPort())));
        }

        private void removePending(InetSocketAddress removeAddr) {
            pendingRequests.removeIf(message -> message.address.equals(removeAddr));
        }

        private void updateMaster(InetSocketAddress masterAddr) {
            pendingRequests.forEach(message -> message.address = masterAddr);
        }

        private void addPending(Message message) {
            if (message.message.getTypeCase() != SnakesProto.GameMessage.TypeCase.JOIN)
                pendingRequests.add(message);
        }

        private void confirm(long sequence, SocketAddress inetSocketAddress) {
            pendingRequests.removeIf(message -> message.sequence == sequence && message.address.equals(inetSocketAddress));
        }

        @Override
        public void run() {
            while (true) {
                SnakesProto.GameConfig config = model.getConfig();
                try {
                    Thread.sleep(config.getPingDelayMs());
                    Message pendingMessage = pendingRequests.take();
                    NetworkManager.this.commit(pendingMessage.message, pendingMessage.address);
                } catch (InterruptedException e) {
                    System.err.println("Resend thread was interrupted...");
                }

            }
        }
    }

    public class PingActivity extends VaryingActivity implements Runnable {
        private final AtomicBoolean lock = new AtomicBoolean(false);

        void notifyMessage() {
            synchronized (lock) {
                lock.set(true);
                lock.notify();
            }
        }

        @Override
        public void run() {
            SnakesProto.GameMessage.PingMsg.Builder pingBuilder = SnakesProto.GameMessage.PingMsg.newBuilder();
            SnakesProto.GameMessage.Builder msgBuilder = SnakesProto.GameMessage.newBuilder();
            msgBuilder.setPing(pingBuilder);

            while (true) {
                try {
                    synchronized (activityLock) {
                        System.out.println("Check lock: " + activityLock.get());
                        while (!activityLock.get()) {
                            System.out.println("Now waiting.....(PING)");
                            activityLock.wait();
                        }
                    }
                    synchronized (lock) {
                        lock.wait(model.getConfig().getPingDelayMs());
                        if (!lock.get()) {
                            commit(msgBuilder
                                    .setMsgSeq(NetworkManager.this.getSequenceNum())
                                    .build(), model.getHostAddr());
                        }
                        lock.set(false);
                    }
                } catch (InterruptedException e) {
                    System.out.println("Ping thread interrupted");
                }
            }
        }
    }
}
