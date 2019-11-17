package net.fit;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import net.fit.proto.SnakesProto;

import java.util.List;

@Data
@RequiredArgsConstructor
public class GameModel {
    private final SnakesProto.GameConfig config;
    private List<SnakesProto.GamePlayer> players;

    public boolean canJoin() {
        return true;
    }

    public SnakesProto.GamePlayers getPlayers() {
        SnakesProto.GamePlayers.Builder builder = SnakesProto.GamePlayers.newBuilder();
        builder.addAllPlayers(players);
        return builder.build();
    }
}
