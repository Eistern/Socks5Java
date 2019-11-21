package net.fit.activities;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RequiredArgsConstructor
public class NetworkManager implements Runnable {
    @AllArgsConstructor
    private static class Message {
        private SnakesProto.GameMessage message;
        private long sequence;
        private Date timestamp;
    }

    private long sequenceNum = 0;
    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private PingActivity pingActivity = new PingActivity();
    private final DatagramSocket socket;
    private final GameModel model;

    private synchronized long getSequenceNum() {
        return sequenceNum++;
    }

    public void commit(SnakesProto.GameMessage message) throws InterruptedException {
        messageQueue.put(new Message(message, 0, null));
    }

    public void confirm(long sequence) {
        messageQueue.removeIf(message -> message.sequence == sequence);
    }

    @Override
    public void run() {
        while (true) {
            DatagramPacket packet = new DatagramPacket(new byte[0], 0);
            try {
                Message nextMessage = messageQueue.take();
                nextMessage.sequence = getSequenceNum();
                nextMessage.timestamp = new Date();
                byte[] data = nextMessage.message.toBuilder().setMsgSeq(nextMessage.sequence).build().toByteArray();
                packet.setData(data);
                packet.setLength(data.length);
                packet.setSocketAddress(model.getHost());
                socket.send(packet);
                pingActivity.notifyMessage();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class PingActivity implements Runnable {
        private Boolean lock = Boolean.FALSE;

        void notifyMessage() {
            lock = Boolean.TRUE;
            lock.notify();
        }

        @Override
        public void run() {
            SnakesProto.GameMessage.PingMsg.Builder pingBuilder = SnakesProto.GameMessage.PingMsg.newBuilder();
            SnakesProto.GameMessage.Builder msgBuilder = SnakesProto.GameMessage.newBuilder();
            msgBuilder.setPing(pingBuilder);

            while (true) {
                try {
                    lock.wait(model.getConfig().getPingDelayMs());
                    if (!lock) {
                        commit(msgBuilder.build());
                    }
                    lock = Boolean.FALSE;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
