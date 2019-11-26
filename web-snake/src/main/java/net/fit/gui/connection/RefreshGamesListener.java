package net.fit.gui.connection;

import lombok.RequiredArgsConstructor;
import net.fit.AnnouncementHolder;
import net.fit.proto.SnakesProto;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.util.List;

@RequiredArgsConstructor
public class RefreshGamesListener implements ActionListener {
    private final AnnouncementHolder datagramAnnouncements;
    private final JListHolder listHolder;

    @Override
    public void actionPerformed(ActionEvent e) {
        JButton source = (JButton) e.getSource();
        source.setText("Processing...");
        List<SnakesProto.GameMessage.AnnouncementMsg> list = datagramAnnouncements.getAnnouncements();
        List<InetSocketAddress> origins = datagramAnnouncements.getOrigins();
        listHolder.setAnnouncementList(list, origins);
        source.setText("Refresh");
    }
}
