package net.fit.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.fit.dto.Message;
import net.fit.dto.Pair;
import net.fit.dto.TreePacket;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class MessageManager implements Runnable {
    private List<Pair<Message, InetSocketAddress>> messagesToBroadcast = new ArrayList<>();
    private ConnectedNodes nodes;
    private final Object listLock = new Object();

    public MessageManager(ConnectedNodes nodes) {
        this.nodes = nodes;
    }

    public void addMessage(Message newMessage, InetSocketAddress from) {
        synchronized (listLock) {
            messagesToBroadcast.add(new Pair<>(newMessage, from));
            listLock.notify();
        }
    }

    @Override
    public void run() {
        while (true) {
            synchronized (listLock) {
                while (messagesToBroadcast.isEmpty()) {
                    try {
                        listLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Pair<Message, InetSocketAddress> message = messagesToBroadcast.remove(0);
                nodes.broadcastMessage(new TreePacket(TreePacket.PacketType.MESSAGE, message.getKey()), null, message.getValue());
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
