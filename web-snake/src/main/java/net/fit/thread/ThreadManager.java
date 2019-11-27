package net.fit.thread;

import net.fit.AnnouncementHolder;
import net.fit.GameModel;
import net.fit.activities.AnnouncementActivity;
import net.fit.activities.DatagramListener;
import net.fit.activities.GameIterationActivity;
import net.fit.activities.NetworkManager;
import net.fit.gui.connection.ConnectFrame;

import java.net.MulticastSocket;

public class ThreadManager {
    private final MulticastSocket multicastSocket;
    private final GameModel model;
    private final DatagramListener datagramListener;
    private final NetworkManager networkManager;
    private GameIterationActivity gameIterationActivity = null;
    private NetworkManager.PingActivity pingActivity = null;
    private AnnouncementActivity announcementActivity = null;
    private State previousState = State.NONE;

    enum State {
        CLIENT, MASTER, NONE
    }

    public ThreadManager(MulticastSocket multicastSocket, GameModel model, AnnouncementHolder datagramAnnouncements) {
        this.multicastSocket = multicastSocket;
        this.model = model;

        this.networkManager = new NetworkManager(multicastSocket, model);
        this.datagramListener = new DatagramListener(model, this.networkManager, multicastSocket, datagramAnnouncements);

        Thread networkManagerThread = new Thread(this.networkManager, "Sender");
        Thread datagramListenerThread = new Thread(this.datagramListener, "Listener");
        networkManagerThread.start();
        datagramListenerThread.start();

        ConnectFrame frame = new ConnectFrame(networkManager, datagramAnnouncements);
        frame.setVisible(true);
    }

    public void activateMaster() {
        if (previousState == State.CLIENT) {
            pingActivity.stopActivity();
        }
        if (previousState == State.NONE || (announcementActivity == null && gameIterationActivity == null)) {
            this.gameIterationActivity = new GameIterationActivity(model, datagramListener);
            this.announcementActivity = new AnnouncementActivity(multicastSocket, model, networkManager);
            Thread gameIterationThread = new Thread(this.gameIterationActivity, "GameIteration");
            Thread announcementThread = new Thread(this.announcementActivity, "Announcement");
            gameIterationThread.start();
            announcementThread.start();
        } else {
            gameIterationActivity.resumeActivity();
            announcementActivity.resumeActivity();
        }
        previousState = State.MASTER;
    }

    public void activateClient() {
        if (previousState == State.MASTER) {
            gameIterationActivity.stopActivity();
            announcementActivity.stopActivity();
        }
        if (previousState == State.NONE || pingActivity == null) {
            this.pingActivity = networkManager.getPingActivity();
            Thread pingThread = new Thread(this.pingActivity, "Ping");
            pingThread.start();
        } else {
            pingActivity.resumeActivity();
        }
        previousState = State.CLIENT;
    }
}
