package net.fit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws JsonProcessingException {
        ChatNode node = new ChatNode(new InetSocketAddress(InetAddress.getLoopbackAddress(), 4000));
        Message message = new Message(UUID.randomUUID(), "Hello", new Date());
        ObjectMapper mapper = new ObjectMapper();

        String packetJson;
        TreePacket packet = new TreePacket(TreePacket.PacketType.CONNECT_NODE, node);
        System.out.println(packetJson = mapper.writeValueAsString(packet));

        TreePacket packet2 = mapper.readValue(packetJson, TreePacket.class);
        System.out.println(packet.equals(packet2));

        TreePacket packet1 = new TreePacket(TreePacket.PacketType.CONNECT_NODE, node);
        System.out.println(mapper.writeValueAsString(null));
    }
}
