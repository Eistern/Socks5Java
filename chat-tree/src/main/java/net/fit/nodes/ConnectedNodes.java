package net.fit.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.fit.dto.ChatNode;
import net.fit.dto.Message;
import net.fit.dto.Pair;
import net.fit.dto.TreePacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Semaphore;

public class ConnectedNodes {
    private List<ChatNode> connectedNodes = new ArrayList<>();
    private DatagramSocket socket;
    private final ObjectMapper mapper;
    private final Semaphore mutex = new Semaphore(1);
    private Date lastDisconnect = null;
    private List<Message> undefinedMessages = null;
    private final long INHERITANCE_TIME = 1000;
    private final long TIMEOUT = 2000;
    private final int CRITICAL_TRUST_LEVEL = 4;

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
            if (lastDisconnect != null && new Date().getTime() - lastDisconnect.getTime() < INHERITANCE_TIME) {
                List<Message> inheritedMessages = new ArrayList<>(undefinedMessages);
                inheritedMessages.forEach(message -> message.setTimeSent(new Date()));
                node.setUnconfirmedMessages(inheritedMessages);
            }
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
        broadcastMessage(replacerPacket, newReplacer.getAddress(), null);
    }

    public void removeNode(ChatNode node) throws IOException {
        TreePacket packet = new TreePacket(TreePacket.PacketType.DISCONNECT_NODE, null);
        byte[] data = mapper.writeValueAsBytes(packet);
        ChatNode removedNode = null;

        try {
            mutex.acquire();
            socket.send(new DatagramPacket(data, data.length, node.getAddress()));
            int ind = connectedNodes.indexOf(node);
            removedNode = connectedNodes.remove(ind);
            lastDisconnect = new Date();
            undefinedMessages = removedNode.getUnconfirmedMessages();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            mutex.release();
        }
    }

    public void pardonNode(InetSocketAddress address) {
        ChatNode node = new ChatNode(address);
        int ind = connectedNodes.indexOf(node);
        ChatNode actualNode = connectedNodes.get(ind);
        actualNode.pardon();
    }

    public void unicastMessage(TreePacket packet, InetSocketAddress to) throws IOException {
        byte[] data = mapper.writeValueAsBytes(packet);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length, to);
        int ind = connectedNodes.indexOf(new ChatNode(to));
        ChatNode node = connectedNodes.get(ind);
        socket.send(datagramPacket);
        if (packet.getPacketType() == TreePacket.PacketType.MESSAGE)
            node.addUnconfirmedMessage((Message) packet.getData());
        if (packet.getPacketType() == TreePacket.PacketType.RUOK)
            node.incrementTrust();
    }

    void broadcastMessage(TreePacket packet, InetSocketAddress except, InetSocketAddress from) throws JsonProcessingException {
        byte[] data = mapper.writeValueAsBytes(packet);
        DatagramPacket datagramPacket = new DatagramPacket(data, data.length);

        try {
            mutex.acquire();
            connectedNodes.forEach(node -> {
                if (!node.getAddress().equals(except)) {
                    try {
                        datagramPacket.setSocketAddress(node.getAddress());
                        socket.send(datagramPacket);
                        if (packet.getPacketType() == TreePacket.PacketType.MESSAGE && !node.getAddress().equals(from))
                            node.addUnconfirmedMessage((Message) packet.getData());
                        if (packet.getPacketType() == TreePacket.PacketType.RUOK)
                            node.incrementTrust();
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

    List<Pair<Message, InetSocketAddress>> getUnsentMessages() {
        Date currentTime = new Date();
        List<Pair<Message, InetSocketAddress>> result = new ArrayList<>();
        try {
            mutex.acquire();
            for (ChatNode connectedNode : connectedNodes) {
                if (connectedNode.getTrust() > CRITICAL_TRUST_LEVEL)
                    continue;
                ListIterator<Message> iter = connectedNode.getUnconfirmedMessages().listIterator();
                while (iter.hasNext()) {
                    Message currentMessage = iter.next();
                    if (currentTime.getTime() - currentMessage.getTimeSent().getTime() > TIMEOUT) {
                        result.add(new Pair<>(currentMessage, connectedNode.getAddress()));
                        iter.remove();
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            mutex.release();
        }
        return result;
    }

    List<ChatNode> getBadNodes() {
        List<ChatNode> result = new ArrayList<>();
        for (ChatNode connectedNode : connectedNodes) {
            if (connectedNode.getTrust() > CRITICAL_TRUST_LEVEL)
                result.add(connectedNode);
        }
        return result;
    }
}
