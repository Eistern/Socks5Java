package net.fit.activities;

import lombok.AllArgsConstructor;
import net.fit.GameModel;
import net.fit.proto.SnakesProto;

import java.net.InetSocketAddress;
import java.util.List;

@AllArgsConstructor
public class GameIterationActivity extends VaryingActivity implements Runnable {
    private GameModel model;
    private DatagramListener listener;
    private NetworkManager manager;

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
                model.iterateState(listener.getRecentDirections());
                SnakesProto.GameState currentState = model.getState();
                List<SnakesProto.GamePlayer> players = currentState.getPlayers().getPlayersList();
                players.forEach(
                        player -> {
                            try {
                                if (!player.getIpAddress().equals("127.0.0.1")) {
//                                    System.out.println("SENDING STATE TO: " + player.getIpAddress());
                                    manager.commit(messageBuilder
                                            .setState(builder.setState(currentState))
                                            .setMsgSeq(manager.getSequenceNum()).build(), new InetSocketAddress(player.getIpAddress(), player.getPort()));
                                }
                            } catch (InterruptedException e) {
                                System.err.println("Can't send current state");
                            }
                        }
                );
            } catch (InterruptedException e) {
                System.err.println("Game iteration activity interrupted");
            }
        }
    }
}
