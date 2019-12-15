package net.fit.gui.connection;

import lombok.RequiredArgsConstructor;
import net.fit.ConfigService;
import net.fit.GameModel;
import net.fit.activities.NetworkManager;
import net.fit.proto.SnakesProto;
import net.fit.thread.ThreadManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketAddress;

@RequiredArgsConstructor
public class ConnectListener implements ActionListener {
    private final JListHolder listHolder;
    private final NetworkManager networkManager;
    private final GameModel model;
    private final ThreadManager threadManager;
    private final int currentPort;

    @Override
    public void actionPerformed(ActionEvent e) {
        int ind = listHolder.getJList().getSelectedIndex();
        if (ind == -1)
            return;
        SocketAddress originAddress = listHolder.getOrigin(ind);
        try {
            model.init(ConfigService.getSystemConfig());
            model.setRole(SnakesProto.NodeRole.NORMAL);
            model.setOpenedToAck(true);
            networkManager.commit(SnakesProto.GameMessage.newBuilder().setJoin(
                    SnakesProto.GameMessage.JoinMsg.newBuilder()
                            .setOnlyView(false)
                            .setPlayerType(SnakesProto.PlayerType.HUMAN)
                            .setName("Eistern-" + currentPort)
                            .build())
                    .setMsgSeq(networkManager.getSequenceNum())
                    .build(), originAddress);
        } catch (InterruptedException | IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
        threadManager.activateClient();
    }
}
