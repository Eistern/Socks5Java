package net.fit.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.fit.dto.Message;
import net.fit.dto.TreePacket;

import java.util.ArrayList;
import java.util.List;

public class MessageManager implements Runnable {
    private List<Message> messagesToBroadcast = new ArrayList<>();
    private ConnectedNodes nodes;
    private final Object listLock = new Object();

    public MessageManager(ConnectedNodes nodes) {
        this.nodes = nodes;
    }

    public void addMessage(Message newMessage) {
        synchronized (listLock) {
            messagesToBroadcast.add(newMessage);
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
                Message message = messagesToBroadcast.remove(0);
                nodes.broadcastMessage(new TreePacket(TreePacket.PacketType.MESSAGE, message), null);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
}
