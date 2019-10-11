package net.fit.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.fit.dto.ChatNode;
import net.fit.dto.Message;
import net.fit.dto.TreePacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ConnectedNodes {
    private List<ChatNode> connectedNodes = new ArrayList<>();
    private DatagramSocket socket;
    private final ObjectMapper mapper;
    private final Semaphore mutex = new Semaphore(1);
    private Date lastDisconnect;

    public ConnectedNodes(DatagramSocket socket) {
        this.socket = socket;
        this.mapper = new ObjectMapper();
    }

    public void addNode(ChatNode node, boolean notify) throws IOException {
        if (notify) {
            TreePacket packet = new TreePacket(TreePacket.PacketType.CONNECT_NODE, null);
            byte[] data = mapper.writeValueAsBytes(packet);
            socket.send(new DatagramPacket(data, data.length, node.getAddress()));
        }

        try {
            mutex.acquire();
            connectedNodes.add(node);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            mutex.release();
        }

        ChatNode newReplacer = findReplacer();
        TreePacket replacerPacket = new TreePacket(TreePacket.PacketType.UPDATE_REPLACER, newReplacer);
        assert newReplacer != null;
        broadcastMessage(replacerPacket, newReplacer.getAddress());
    }

    public void removeNode(ChatNode node) throws IOException {
        TreePacket packet = new TreePacket(TreePacket.PacketType.DISCONNECT_NODE, null);
        byte[] data = mapper.writeValueAsBytes(packet);

        try {
            mutex.acquire();
            socket.send(new DatagramPacket(data, data.length, node.getAddress()));
            connectedNodes.remove(node);
            lastDisconnect = new Date();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            mutex.release();
        }
    }

    void broadcastMessage(TreePacket packet, InetSocketAddress except) throws JsonProcessingException {
        byte[] data = mapper.writeValueAsBytes(packet);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length);

        try {
            mutex.acquire();
            connectedNodes.forEach(node -> {
                if (!node.getAddress().equals(except)) {
                    try {
                        datagramPacket.setSocketAddress(node.getAddress());
                        socket.send(datagramPacket);
                        if (packet.getPacketType() == TreePacket.PacketType.MESSAGE)
                            node.addUnconfirmedMessage((Message) packet.getData());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            mutex.release();
        }
    }

    public boolean ackMessage(Message message, InetSocketAddress from) {
        ChatNode node = new ChatNode(from);
        int index = connectedNodes.indexOf(node);
        if (index == -1)
            return false;
        node = connectedNodes.get(index);
        return node.messageSent(message);
    }

    private ChatNode findReplacer() {
        if (connectedNodes.isEmpty())
            return null;
        ChatNode node = connectedNodes.get(0);
        for (ChatNode connectedNode : connectedNodes) {
            if (connectedNode.getAddress().hashCode() < node.getAddress().hashCode())
                node = connectedNode;
        }
        return node;
    }

    public void updateReplacer(ChatNode of, ChatNode replacer) {
        int index;
        try {
            mutex.acquire();
            if ((index = connectedNodes.indexOf(of)) != -1) {
                ChatNode changingNode = connectedNodes.remove(index);
                changingNode.setReplacer(replacer);
                connectedNodes.add(changingNode);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            mutex.release();
        }
    }
}
