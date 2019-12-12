package net.fit.gui.view;

import lombok.RequiredArgsConstructor;
import net.fit.ConfigService;
import net.fit.GameModel;
import net.fit.activities.NetworkManager;
import net.fit.proto.SnakesProto;
import net.fit.thread.ThreadManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;

@RequiredArgsConstructor
public class ExitButtonListener implements ActionListener {
    private final GameModel model;
    private final NetworkManager networkManager;
    private final ThreadManager threadManager;

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            SnakesProto.GamePlayer player = null;
            switch (model.getRole()) {
                case MASTER:
                    player = model.getFirstOfRole(SnakesProto.NodeRole.DEPUTY);
                    break;
                case DEPUTY:
                case NORMAL:
                    player = model.getFirstOfRole(SnakesProto.NodeRole.MASTER);
                    if (player != null)
                        player = player.toBuilder().setPort(model.getHostAddr().getPort()).setIpAddress(model.getHostAddr().getAddress().getHostAddress()).build();
                    break;
                default:
                    break;
            }
            if (player != null) {
                networkManager.commit(SnakesProto.GameMessage.newBuilder()
                        .setMsgSeq(networkManager.getSequenceNum())
                        .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                .setSenderRole(SnakesProto.NodeRole.VIEWER)
                                .setReceiverRole(player.getRole())
                                .build())
                        .setSenderId(model.getOwnId())
                        .setReceiverId(player.getId())
                        .build(), new InetSocketAddress(player.getIpAddress(), player.getPort()));
            }
            threadManager.pauseActivities();
            model.init(ConfigService.getSystemConfig());
        } catch (ClassNotFoundException | IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
