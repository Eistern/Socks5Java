package net.fit.nodes;

import lombok.AllArgsConstructor;
import net.fit.dto.ChatNode;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;

@AllArgsConstructor
public class NodeControl extends TimerTask {
    private ConnectedNodes nodes;

    @Override
    public void run() {
        List<ChatNode> badNodes = nodes.getBadNodes();
        for (ChatNode badNode : badNodes) {
            try {
                System.out.println("Node " + badNode.getAddress() + " will be removed");
                nodes.removeNode(badNode);
                if (badNode.getReplacer() != null)
                    nodes.addNode(badNode.getReplacer(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
