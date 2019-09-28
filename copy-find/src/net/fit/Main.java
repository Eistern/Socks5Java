package net.fit;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class Main {
    private static volatile boolean timeout = false;
    private static List<ConnectedEntry> connectedCopies = new ArrayList<>();
    private static int iteration = 0;
    private static boolean hasChanged = false;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("No address specified");
            return;
        }

        InetAddress targetAddress = InetAddress.getByName(args[0]);

        int port = (int)(Math.random() * ((49150 - 49001) + 1)) + 49001;
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(targetAddress);
        socket.setSoTimeout(2000);

        Timer timer = new Timer();

        DatagramPacket packetSend = new DatagramPacket(new byte[]{}, 0, targetAddress, port);
        DatagramPacket packetRecv = new DatagramPacket(new byte[]{}, 0, targetAddress, port);

        int index;
        while (true) {
            for (int i = 49001; i < 49150; i++) {
                packetSend.setPort(i);
                socket.send(packetSend);
            }

            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    Main.timeout = true;
                }
            };
            timer.schedule(task, 4000);
            while (!timeout) {
                try {
                    socket.receive(packetRecv);
                    ConnectedEntry newEntry = new ConnectedEntry(packetRecv.getAddress().getHostAddress() + ":" + packetRecv.getPort(), 0);
                    if ((index = connectedCopies.indexOf(newEntry)) != -1)
                        connectedCopies.get(index).setIterationCount(iteration);
                    else {
                        newEntry.setIterationCount(iteration);
                        connectedCopies.add(newEntry);
                        hasChanged = true;
                    }
                }
                catch (SocketTimeoutException e) {
                    //System.out.println("No packets in 2 seconds");
                }
            }

            connectedCopies.removeIf((connectedEntry -> {
                if (iteration > connectedEntry.getIterationCount()) {
                    hasChanged = true;
                    return true;
                }
                return false;
            }));
            if (hasChanged)
                connectedCopies.forEach(connectedEntry -> System.out.println("[" + connectedEntry.getIpAddress() + "]"));

            if (connectedCopies.isEmpty())
                System.out.println("No copies found");

            System.out.println("---------New Iteration---------");

            iteration++;
            timeout = false;
            hasChanged = false;
        }
    }
}
