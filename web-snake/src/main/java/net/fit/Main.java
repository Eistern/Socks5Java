package net.fit;

import net.fit.thread.ThreadManager;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length < 1) {
            System.err.println("Usage: <port>");
            return;
        }

        GameModel model = new GameModel();
        MulticastSocket multicastSocket = new MulticastSocket(9192);
        model.init(ConfigService.getSystemConfig());
        AnnouncementHolder datagramAnnouncements = new AnnouncementHolder();

        int currentPort = Integer.parseInt(args[0]);
        DatagramSocket datagramSocket = new DatagramSocket(currentPort);
        ThreadManager threadManager = new ThreadManager(multicastSocket, datagramSocket, model, datagramAnnouncements, currentPort);
    }
}
