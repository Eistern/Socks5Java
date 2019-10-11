package net.fit.nodes;

import net.fit.dto.Message;
import net.fit.dto.Pair;
import net.fit.dto.TreePacket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.TimerTask;

public class MessageRepeater extends TimerTask {
    private final ConnectedNodes nodes;

    public MessageRepeater(ConnectedNodes nodes) {
        this.nodes = nodes;
    }

    @Override
    public void run() {
        List<Pair<Message, InetSocketAddress>> messagesToRepeat = nodes.getUnsentMessages();
        for (Pair<Message, InetSocketAddress> addressPair : messagesToRepeat) {
            try {
                nodes.unicastMessage(new TreePacket(TreePacket.PacketType.MESSAGE, addressPair.getKey()), addressPair.getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
