package net.fit;

import net.fit.activities.AnnouncementActivity;
import net.fit.activities.DatagramListener;
import net.fit.activities.NetworkManager;
import net.fit.gui.ConnectFrame;

import java.io.IOException;
import java.net.MulticastSocket;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        GameModel model = new GameModel();
        MulticastSocket multicastSocket = new MulticastSocket(9192);
        model.init(ConfigService.getSystemConfig());
        AnnouncementHolder datagramAnnouncements = new AnnouncementHolder();
        NetworkManager networkManager = new NetworkManager(multicastSocket, model);
        DatagramListener datagramListener = new DatagramListener(model, networkManager, multicastSocket, datagramAnnouncements);
        AnnouncementActivity announcementActivity = new AnnouncementActivity(multicastSocket, model, networkManager);

        Thread networkManagerThread = new Thread(networkManager);
        Thread datagramListenerThread = new Thread(datagramListener);
        Thread announcementActivityThread = new Thread(announcementActivity);

        networkManagerThread.start();
        datagramListenerThread.start();
        announcementActivityThread.start();

        ConnectFrame frame = new ConnectFrame(networkManager, datagramAnnouncements);
        frame.setVisible(true);
        System.out.println("Test");
    }
}
