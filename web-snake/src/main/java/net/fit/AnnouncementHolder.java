package net.fit;

import net.fit.proto.SnakesProto;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class AnnouncementHolder {
    private List<InetSocketAddress> origins = new ArrayList<>();
    private List<SnakesProto.GameMessage.AnnouncementMsg> announcements = new ArrayList<>();

    public synchronized void addAnnouncement(SnakesProto.GameMessage.AnnouncementMsg announcementMsg, InetSocketAddress origin) {
        announcements.add(announcementMsg);
        origins.add(origin);
    }

    public synchronized List<SnakesProto.GameMessage.AnnouncementMsg> getAnnouncements() {
        List<SnakesProto.GameMessage.AnnouncementMsg> announcementMsgs = new ArrayList<>(announcements);
        announcements.clear();
        return announcementMsgs;
    }

    public synchronized List<InetSocketAddress> getOrigins() {
        List<InetSocketAddress> origins = new ArrayList<>(this.origins);
        this.origins.clear();
        return origins;
    }
}
