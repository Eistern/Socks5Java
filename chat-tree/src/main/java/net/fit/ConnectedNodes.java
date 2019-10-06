package net.fit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ConnectedNodes {
    private List<ChatNode> connectedNodes = new ArrayList<>();
    private DatagramSocket socket;
    private final ObjectMapper mapper;

    public ConnectedNodes(DatagramSocket socket) {
        this.socket = socket;
        this.mapper = new ObjectMapper();
    }

    public void addNode(ChatNode node) throws IOException {
        TreePacket packet = new TreePacket(TreePacket.PacketType.CONNECT_NODE, null);
        byte[] data = mapper.writeValueAsBytes(packet);
        socket.send(new DatagramPacket(data, data.length, node.getAddress()));
        connectedNodes.add(node);
    }

    public void removeNode(ChatNode node) throws IOException {
        TreePacket packet = new TreePacket(TreePacket.PacketType.DISCONNECT_NODE, null);
        byte[] data = mapper.writeValueAsBytes(packet);
        socket.send(new DatagramPacket(data, data.length, node.getAddress()));
        connectedNodes.remove(node);
    }

    public void broadcastMessage(Message message, InetSocketAddress except) throws JsonProcessingException {
        TreePacket packet = new TreePacket(TreePacket.PacketType.MESSAGE, message);
        byte[] data = mapper.writeValueAsBytes(packet);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
        connectedNodes.forEach(node -> {
            if (!node.getAddress().equals(except)) {
                try {
                    datagramPacket.setSocketAddress(node.getAddress());
                    socket.send(datagramPacket);
                    node.addMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void ackMessage(Message message, InetSocketAddress from) {
        ChatNode node = new ChatNode(from);
        int index = connectedNodes.indexOf(node);
        node = connectedNodes.get(index);
        node.messageSent(message);
    }

    public ChatNode findReplacer() {
        if (connectedNodes.isEmpty())
            return null;
        ChatNode node = connectedNodes.get(0);
        for (ChatNode connectedNode : connectedNodes) {
            if (connectedNode.getAddress().hashCode() < node.getAddress().hashCode())
                node = connectedNode;
        }
        return node;
    }
}
