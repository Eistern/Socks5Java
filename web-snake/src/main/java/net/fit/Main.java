package net.fit;

import net.fit.thread.ThreadManager;

import java.io.IOException;
import java.net.MulticastSocket;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        GameModel model = new GameModel();
        MulticastSocket multicastSocket = new MulticastSocket(9192);
        model.init(ConfigService.getSystemConfig());
        AnnouncementHolder datagramAnnouncements = new AnnouncementHolder();

        ThreadManager threadManager = new ThreadManager(multicastSocket, model, datagramAnnouncements);
    }
}
