package net.fit.thread;

import net.fit.AnnouncementHolder;
import net.fit.GameModel;
import net.fit.activities.*;
import net.fit.gui.connection.ConnectFrame;
import net.fit.gui.score.ScoreFrame;
import net.fit.gui.view.ViewFrame;

import java.net.DatagramSocket;
import java.net.MulticastSocket;

public class ThreadManager {
    private final DatagramSocket datagramSocket;
    private final GameModel model;
    private final DatagramListener datagramListener;
    private final NetworkManager networkManager;
    private GameIterationActivity gameIterationActivity = null;
    private NetworkManager.PingActivity pingActivity = null;
    private AnnouncementActivity announcementActivity = null;
    private State previousState = State.NONE;

    enum State {
        CLIENT, MASTER, NONE, PAUSED
    }

    public ThreadManager(MulticastSocket multicastSocket, DatagramSocket datagramSocket, GameModel model, AnnouncementHolder datagramAnnouncements, int currentPort) {
        this.datagramSocket = datagramSocket;
        this.model = model;

        this.networkManager = new NetworkManager(this, datagramSocket, model);
        this.datagramListener = new DatagramListener(model, this.networkManager, datagramSocket, this);
        AnnouncementListener announcementListener = new AnnouncementListener(multicastSocket, datagramAnnouncements);

        Thread networkManagerThread = new Thread(this.networkManager, "Sender");
        Thread datagramListenerThread = new Thread(this.datagramListener, "Listener");
        Thread announcementListenerThread = new Thread(announcementListener, "Announcement Listener");
        networkManagerThread.start();
        datagramListenerThread.start();
        announcementListenerThread.start();

        ConnectFrame frame = new ConnectFrame(networkManager, datagramAnnouncements, model, this, currentPort);
        frame.setVisible(true);

        ViewFrame viewFrame = new ViewFrame(model, this.networkManager, this);
        viewFrame.setVisible(true);

        ScoreFrame scoreFrame = new ScoreFrame(model);
        scoreFrame.setVisible(true);
    }

    public synchronized void pauseActivities() {
        System.out.println("PAUSING.....");
        if (previousState == State.PAUSED) {
            return;
        }
        if (previousState == State.CLIENT) {
            pingActivity.stopActivity();
        }
        if (previousState == State.MASTER) {
            gameIterationActivity.stopActivity();
            announcementActivity.stopActivity();
        }
        previousState = State.PAUSED;
    }

    public synchronized void activateMaster() {
        if (previousState == State.MASTER) {
            return;
        }
        if (previousState == State.CLIENT) {
            pingActivity.stopActivity();
        }
        if (previousState == State.NONE || (announcementActivity == null && gameIterationActivity == null)) {
            this.gameIterationActivity = new GameIterationActivity(model, datagramListener, networkManager, this);
            this.announcementActivity = new AnnouncementActivity(datagramSocket, model, networkManager);
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

    public synchronized void activateClient() {
        if (previousState == State.CLIENT) {
            return;
        }
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
