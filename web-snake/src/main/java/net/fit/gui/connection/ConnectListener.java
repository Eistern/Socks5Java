package net.fit.gui.connection;

import lombok.RequiredArgsConstructor;
import net.fit.activities.NetworkManager;
import net.fit.proto.SnakesProto;
import net.fit.thread.ThreadManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.SocketAddress;

@RequiredArgsConstructor
public class ConnectListener implements ActionListener {
    private final JListHolder listHolder;
    private final NetworkManager networkManager;
    private final ThreadManager threadManager;

    @Override
    public void actionPerformed(ActionEvent e) {
        int ind = listHolder.getJList().getSelectedIndex();
        if (ind == -1)
            return;
        SocketAddress originAddress = listHolder.getOrigin(ind);
        try {
            networkManager.commit(SnakesProto.GameMessage.newBuilder().setJoin(
                    SnakesProto.GameMessage.JoinMsg.newBuilder()
                            .setOnlyView(false)
                            .setPlayerType(SnakesProto.PlayerType.HUMAN)
                            .setName("Eistern")
                            .build())
                    .setMsgSeq(networkManager.getSequenceNum())
                    .build(), originAddress);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        threadManager.activateClient();
    }
}
