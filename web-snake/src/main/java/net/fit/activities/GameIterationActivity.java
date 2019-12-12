package net.fit.activities;

import lombok.AllArgsConstructor;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;
import net.fit.thread.ThreadManager;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@AllArgsConstructor
public class GameIterationActivity extends VaryingActivity implements Runnable {
    private GameModel model;
    private DatagramListener listener;
    private NetworkManager manager;
    private ThreadManager threadManager;

    @Override
    public void run() {
        SnakesProto.GameMessage.Builder messageBuilder = SnakesProto.GameMessage.newBuilder();
        SnakesProto.GameMessage.StateMsg.Builder builder = SnakesProto.GameMessage.StateMsg.newBuilder();
        while (true) {
            try {
                synchronized (activityLock) {
                    while (!activityLock.get()) {
                        activityLock.wait();
                    }
                }
                Thread.sleep(model.getConfig().getStateDelayMs());
                List<SnakesProto.GamePlayer> deadPlayers = model.iterateState(listener.getRecentDirections());
                SnakesProto.GameState currentState = model.getState();
                int ownId = model.getOwnId();
                List<SnakesProto.GamePlayer> players = currentState.getPlayers().getPlayersList();
                players.forEach(
                        player -> {
                            try {
                                if (player.getId() != ownId) {
                                    manager.commit(messageBuilder
                                            .setState(builder.setState(currentState))
                                            .setMsgSeq(manager.getSequenceNum()).build(), new InetSocketAddress(player.getIpAddress(), player.getPort()));
                                }
                            } catch (InterruptedException e) {
                                System.err.println("Can't send current state");
                            }
                        }
                );

                AtomicBoolean deputyLost = new AtomicBoolean(false);
                AtomicBoolean masterLost = new AtomicBoolean(false);
                deadPlayers.forEach(player -> {
                    try {
                        if (player.getRole() == SnakesProto.NodeRole.DEPUTY)
                            deputyLost.set(true);
                        if (player.getRole() == SnakesProto.NodeRole.MASTER)
                            masterLost.set(true);
                        manager.commit(messageBuilder
                                .setMsgSeq(manager.getSequenceNum())
                                .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                        .setSenderRole(SnakesProto.NodeRole.MASTER)
                                        .setReceiverRole(SnakesProto.NodeRole.VIEWER)
                                        .build())
                                .setSenderId(model.getOwnId())
                                .setReceiverId(player.getId())
                                .build(), new InetSocketAddress(player.getIpAddress(), player.getPort()));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                if (deputyLost.get()) {
                    SnakesProto.GamePlayer deputy = model.reelectDeputy();
                    if (deputy != null) {
                        manager.commit(messageBuilder
                                .setMsgSeq(manager.getSequenceNum())
                                .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                        .setSenderRole(SnakesProto.NodeRole.MASTER)
                                        .setReceiverRole(SnakesProto.NodeRole.DEPUTY)
                                        .build())
                                .setSenderId(model.getOwnId())
                                .setReceiverId(deputy.getId())
                                .build(), new InetSocketAddress(deputy.getIpAddress(), deputy.getPort()));
                    }
                }
                if (masterLost.get()) {
                    SnakesProto.GamePlayer deputyAddr = model.getFirstOfRole(SnakesProto.NodeRole.DEPUTY);
                    if (deputyAddr == null) {
                        deputyAddr = model.reelectDeputy();
                    }
                    if (deputyAddr != null) {
                        manager.commit(messageBuilder
                                .setMsgSeq(manager.getSequenceNum())
                                .setRoleChange(SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                        .setSenderRole(SnakesProto.NodeRole.VIEWER)
                                        .setReceiverRole(SnakesProto.NodeRole.MASTER)
                                        .build())
                                .setSenderId(model.getOwnId())
                                .setReceiverId(deputyAddr.getId())
                                .build(), new InetSocketAddress(deputyAddr.getIpAddress(), deputyAddr.getPort()));
                        threadManager.activateClient();
                    }
                    else {
                        threadManager.pauseActivities();
                    }
                }
            } catch (InterruptedException e) {
                System.err.println("Game iteration activity interrupted");
            }
        }
    }
}
