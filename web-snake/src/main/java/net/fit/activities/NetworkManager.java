package net.fit.activities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;

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
    @Getter private PingActivity pingActivity = new PingActivity();
    private final ResendActivity resendActivity = new ResendActivity();
    private final DatagramSocket socket;
    private final GameModel model;

    public synchronized long getSequenceNum() {
        return sequenceNum++;
    }

    public void commit(SnakesProto.GameMessage message, SocketAddress address) throws InterruptedException {
        messageQueue.put(new Message(message, address, 0, null));
    }

    public void confirm(long sequence) {
        resendActivity.confirm(sequence);
    }

    @Override
    public void run() {
        Thread resendThread = new Thread(resendActivity, "Resend");
        resendThread.start();
        DatagramPacket packet = new DatagramPacket(new byte[0], 0);
        while (true) {
            try {
                Message nextMessage = messageQueue.take();
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

        @Override
        public void run() {
            while (true) {

            }
        }
    }

    private class ResendActivity implements Runnable {
        private BlockingQueue<Message> pendingRequests = new LinkedBlockingQueue<>();

        public void updateMaster(InetSocketAddress masterAddr) {
            pendingRequests.forEach(message -> message.address = masterAddr);
        }

        private void addPending(Message message) {
            if (message.message.getTypeCase() != SnakesProto.GameMessage.TypeCase.JOIN)
                pendingRequests.add(message);
        }

        private void confirm(long sequence) {
            pendingRequests.removeIf(message -> message.sequence == sequence);
        }

        @Override
        public void run() {
            while (true) {
                SnakesProto.GameConfig config = model.getConfig();
                try {
                    Thread.sleep(config.getPingDelayMs());
                    Message pendingMessage = pendingRequests.take();
                    commit(pendingMessage.message, pendingMessage.address);
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
                        while (!activityLock.get()) {
                            activityLock.wait();
                        }
                    }
                    synchronized (lock) {
                        lock.wait(model.getConfig().getPingDelayMs());
                        if (!lock.get()) {
                            commit(msgBuilder
                                    .setMsgSeq(NetworkManager.this.getSequenceNum())
                                    .build(), model.getHost());
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
