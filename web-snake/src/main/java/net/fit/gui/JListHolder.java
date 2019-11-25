package net.fit.gui;

import net.fit.proto.SnakesProto;

import javax.swing.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

class JListHolder {
    private JList<String> jList = new JList<>();
    private List<SocketAddress> addressesList = new ArrayList<>();

    JList<String> getJList() {
        return jList;
    }

    synchronized void setAnnouncementList(List<SnakesProto.GameMessage.AnnouncementMsg> announcementList, List<InetSocketAddress> addressesList) {
        this.addressesList.clear();
        this.addressesList.addAll(addressesList);
        List<String> dataArray = new ArrayList<>();
        for (int i = 0; i < announcementList.size(); i++) {
            SnakesProto.GameMessage.AnnouncementMsg announcement = announcementList.get(i);
            int x = announcement.getConfig().getWidth();
            int y = announcement.getConfig().getHeight();
            String address = addressesList.get(i).getHostName();
            int port = addressesList.get(i).getPort();
            String description = address + ":" + port + " [" + x + " * " + y + "]";
            dataArray.add(description + (announcement.hasCanJoin() ? " [" + announcement.getCanJoin() + "]" : ""));
        }
        jList.setListData(dataArray.toArray(new String[0]));
    }

    synchronized SocketAddress getOrigin(int ind) {
        return addressesList.get(ind);
    }
}
