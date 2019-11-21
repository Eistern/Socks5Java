package net.fit;

import lombok.Data;
import net.fit.proto.SnakesProto;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class GameModel {
    private SnakesProto.GameConfig config;
    private SnakesProto.GameState state;

    public void init(SnakesProto.GameConfig config) {
        this.config = config;
        SnakesProto.GameState.Builder builder = SnakesProto.GameState.newBuilder();
        List<SnakesProto.GamePlayer> players = Collections.synchronizedList(new ArrayList<>());
        builder.setStateOrder(0);
        builder.setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players));
    }

    public void init(SnakesProto.GameState old) {
        state = old;
        config = old.getConfig();
    }

    public boolean canJoin() {
        return true;
    }

    public SnakesProto.GamePlayers getPlayers() {
        return state.getPlayers();
    }

    public SocketAddress getHost() {
        List<SnakesProto.GamePlayer> players = state.getPlayers().getPlayersList();
        for (SnakesProto.GamePlayer player : players) {
            if (player.getRole() == SnakesProto.NodeRole.MASTER)
                return new InetSocketAddress(player.getIpAddress(), player.getPort());
        }
        return null;
    }

    public void updateState(Map<SnakesProto.GamePlayer, SnakesProto.Direction> updateDirection) {
        SnakesProto.GameState.Builder builder = state.toBuilder();
        builder.setStateOrder(builder.getStateOrder() + 1);
        state = builder.build();
    }
}
