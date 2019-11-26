package net.fit.gui.connection;

import net.fit.AnnouncementHolder;
import net.fit.activities.NetworkManager;

import javax.swing.*;
import java.awt.*;

public class ConnectFrame extends JFrame {
    private JListHolder gameList;
    private JButton refreshButton;
    private JButton connectButton;

    public ConnectFrame(NetworkManager manager, AnnouncementHolder datagramAnnouncement) {
        super("Current Games");

        JPanel pnPanel0 = new JPanel();
        GridBagLayout gbPanel0 = new GridBagLayout();
        GridBagConstraints gbcPanel0 = new GridBagConstraints();
        pnPanel0.setLayout(gbPanel0);

        gameList = new JListHolder();
        gbcPanel0.gridx = 1;
        gbcPanel0.gridy = 2;
        gbcPanel0.gridwidth = 18;
        gbcPanel0.gridheight = 13;
        gbcPanel0.fill = GridBagConstraints.BOTH;
        gbcPanel0.weightx = 1;
        gbcPanel0.weighty = 1;
        gbcPanel0.anchor = GridBagConstraints.CENTER;
        gbPanel0.setConstraints(gameList.getJList(), gbcPanel0);
        pnPanel0.add(gameList.getJList());

        RefreshGamesListener refreshListener = new RefreshGamesListener(datagramAnnouncement, gameList);
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(refreshListener);
        gbcPanel0.gridx = 18;
        gbcPanel0.gridy = 0;
        gbcPanel0.gridwidth = 1;
        gbcPanel0.gridheight = 1;
        gbcPanel0.fill = GridBagConstraints.BOTH;
        gbcPanel0.weightx = 1;
        gbcPanel0.weighty = 0;
        gbcPanel0.anchor = GridBagConstraints.NORTHEAST;
        gbPanel0.setConstraints(refreshButton, gbcPanel0);
        pnPanel0.add(refreshButton);

        JPanel pnPanel1 = new JPanel();
        GridBagLayout gbPanel1 = new GridBagLayout();
        GridBagConstraints gbcPanel1 = new GridBagConstraints();
        pnPanel1.setLayout(gbPanel1);
        gbcPanel0.gridx = 1;
        gbcPanel0.gridy = 0;
        gbcPanel0.gridwidth = 17;
        gbcPanel0.gridheight = 1;
        gbcPanel0.fill = GridBagConstraints.BOTH;
        gbcPanel0.weightx = 1;
        gbcPanel0.weighty = 0;
        gbcPanel0.anchor = GridBagConstraints.NORTH;
        gbcPanel0.insets = new Insets(0,0,0,20);
        gbPanel0.setConstraints(pnPanel1, gbcPanel0);
        pnPanel0.add(pnPanel1);

        ConnectListener connectListener = new ConnectListener(gameList, manager);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(connectListener);
        gbcPanel0.gridx = 0;
        gbcPanel0.gridy = 15;
        gbcPanel0.gridwidth = 20;
        gbcPanel0.gridheight = 5;
        gbcPanel0.fill = GridBagConstraints.BOTH;
        gbcPanel0.weightx = 1;
        gbcPanel0.weighty = 0;
        gbcPanel0.anchor = GridBagConstraints.NORTH;
        gbPanel0.setConstraints(connectButton, gbcPanel0);
        pnPanel0.add(connectButton);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setContentPane(pnPanel0);
        pack();
        setVisible(true);
    }
}
