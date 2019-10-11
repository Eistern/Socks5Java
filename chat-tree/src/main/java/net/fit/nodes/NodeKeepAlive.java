package net.fit.nodes;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import net.fit.dto.TreePacket;

import java.util.TimerTask;

@AllArgsConstructor
public class NodeKeepAlive extends TimerTask {
    private ConnectedNodes nodes;

    @Override
    public void run() {
        try {
            nodes.broadcastMessage(new TreePacket(TreePacket.PacketType.RUOK, null), null, null);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
