package net.fit;

import net.fit.proto.SnakesProto;

import java.net.InetSocketAddress;
import java.util.*;

public class AnnouncementHolder {
    private Map<InetSocketAddress, SnakesProto.GameMessage.AnnouncementMsg> announcements = new HashMap<>();
    private Map<InetSocketAddress, Date> announcementsDate = new HashMap<>();

    public synchronized void addAnnouncement(SnakesProto.GameMessage.AnnouncementMsg announcementMsg, InetSocketAddress origin) {
        announcements.put(origin, announcementMsg);
        announcementsDate.put(origin, new Date());
    }

    private void clear() {
        Date checkDate = new Date();
        announcementsDate.forEach(((inetSocketAddress, date) -> {
            if (checkDate.getTime() - date.getTime() > 1000)
                announcements.remove(inetSocketAddress);
        }));
    }

    public synchronized List<SnakesProto.GameMessage.AnnouncementMsg> getAnnouncements() {
        clear();
        return new ArrayList<>(announcements.values());
    }

    public synchronized List<InetSocketAddress> getOrigins() {
        clear();
        return new ArrayList<>(announcements.keySet());
    }
}
