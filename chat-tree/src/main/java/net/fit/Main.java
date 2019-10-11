package net.fit;

import net.fit.dto.ChatNode;
import net.fit.listeners.InputListener;
import net.fit.listeners.SocketListener;
import net.fit.nodes.*;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Timer;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length % 2 != 1 || args.length < 3) {
            System.err.println("Wrong arguments");
            return;
        }

        int port = Integer.parseInt(args[1]);
        DatagramSocket socket = new DatagramSocket(port);
        ConnectedNodes nodes = new ConnectedNodes(socket);
        MessageManager manager = new MessageManager(nodes);
        InputListener inputListener = new InputListener(manager, args[0]);
        SocketListener socketListener = new SocketListener(socket, nodes, manager, Integer.parseInt(args[2]));

        for (int i = 3; i < args.length; i+=2) {
            nodes.addNode(new ChatNode(new InetSocketAddress(args[i], Integer.parseInt(args[i+1]))), true);
        }

        Thread console = new Thread(inputListener, "CONSOLE");
        Thread socketHandler = new Thread(socketListener, "SOCKET");
        Thread messageDelivery = new Thread(manager, "MESSAGES");
        messageDelivery.start();
        console.start();
        socketHandler.start();

        Timer timer = new Timer();
        timer.schedule(new MessageRepeater(nodes), 3000, 3000);
        timer.schedule(new NodeKeepAlive(nodes), 1000, 1000);
        timer.schedule(new NodeControl(nodes), 5000, 5000);
    }
}
