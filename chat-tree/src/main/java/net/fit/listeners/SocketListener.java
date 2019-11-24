package net.fit.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.fit.dto.ChatNode;
import net.fit.dto.Message;
import net.fit.dto.TreePacket;
import net.fit.nodes.ConnectedNodes;
import net.fit.nodes.MessageManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

@RequiredArgsConstructor
public class SocketListener implements Runnable {
    private final DatagramSocket socket;
    private final ConnectedNodes nodes;
    private final MessageManager manager;
    private final int loss;

    @Override
    public void run() {
        ObjectMapper mapper = new ObjectMapper();
        DatagramPacket received = new DatagramPacket(new byte[1024], 1024);
        System.out.println("Start listen socket");
        while (true) {
            try {
                socket.receive(received);

                if (Math.random() * 100 < loss) {
                    System.out.println("Packet lost");
                    continue;
                }

                InetSocketAddress address = new InetSocketAddress(received.getAddress(), received.getPort());
                TreePacket receivedPacket = mapper.readValue(received.getData(), TreePacket.class);
                switch (receivedPacket.getPacketType()) {
                    case MESSAGE:
                        if (!nodes.ackMessage((Message) receivedPacket.getData(), address)) {
                            Message message = (Message) receivedPacket.getData();
                            System.out.println(message.getPrintingRep());
                            manager.addMessage(message, new InetSocketAddress(received.getAddress(), received.getPort()));
                        }
                        break;
                    case CONNECT_NODE:
                        nodes.addNode(new ChatNode(address), false);
                        break;
                    case DISCONNECT_NODE:
                        nodes.removeNode(new ChatNode(address));
                        break;
                    case UPDATE_REPLACER:
                        nodes.updateReplacer(new ChatNode(address), (ChatNode) receivedPacket.getData());
                        break;
                    case IMOK:
                        nodes.pardonNode(address);
                    case RUOK:
                        nodes.unicastMessage(new TreePacket(TreePacket.PacketType.IMOK, null), address);
                    default:
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
