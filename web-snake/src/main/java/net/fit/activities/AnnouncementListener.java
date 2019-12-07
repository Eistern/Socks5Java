package net.fit.activities;

import lombok.RequiredArgsConstructor;
import net.fit.AnnouncementHolder;
import net.fit.proto.SnakesProto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

@RequiredArgsConstructor
public class AnnouncementListener implements Runnable {
    private final MulticastSocket socket;
    private final AnnouncementHolder announcementHolder;

    @Override
    public void run() {
        SnakesProto.GameMessage message;
        DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
        try {
            socket.joinGroup(InetAddress.getByName("239.192.0.4"));
        } catch (IOException e) {
            System.err.println("Unknown host name for group address, can't receive Announcement messages");
        }
        while (true) {
            try {
                socket.receive(packet);
                message = SnakesProto.GameMessage.parseFrom(Arrays.copyOf(packet.getData(), packet.getLength()));
                switch (message.getTypeCase()) {
                    case ANNOUNCEMENT:
                        System.out.println("GOT ANNOUNCEMENT FROM: " + packet.getSocketAddress());
                        announcementHolder.addAnnouncement(message.getAnnouncement(), (InetSocketAddress) packet.getSocketAddress());
                        break;
                    default:
                        System.err.println("Got unknown message type on multicast socket: " + message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
