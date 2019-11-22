package net.fit;

import lombok.Data;
import net.fit.proto.SnakesProto;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

@Data
public class GameModel {
    private SnakesProto.GameConfig config;
    private SnakesProto.GameState state;
    private int freeX;
    private int freeY;

    public void init(SnakesProto.GameConfig config, String name, int port) {
        this.config = config;
        SnakesProto.GameState.Builder builder = SnakesProto.GameState.newBuilder();

        List<SnakesProto.GamePlayer> players = new ArrayList<>();
        SnakesProto.GamePlayer.Builder playerBuilder = SnakesProto.GamePlayer.newBuilder();
        playerBuilder.setId(0)
            .setIpAddress("")
            .setName(name)
            .setPort(port)
            .setScore(0)
            .setRole(SnakesProto.NodeRole.MASTER)
            .setType(SnakesProto.PlayerType.HUMAN);

        players.add(playerBuilder.build());
        builder.setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players));
        builder.setSnakes(0, SnakesProto.GameState.Snake.getDefaultInstance());

        builder.setStateOrder(0);
    }

    public void init(SnakesProto.GameState old) {
        state = old;
        config = old.getConfig();
    }

    public boolean canJoin() {
        List<SnakesProto.GameState.Snake> snakes = getState().getSnakesList();
        boolean[][] field = generateBoolFiled(snakes);

        for (int x = 0; x < config.getHeight(); x++) {
            for (int y = 0; y < config.getWidth(); y++) {
                if (!field[x][y]) {
                    freeX = x;
                    freeY = y;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean[][] generateBoolFiled(List<SnakesProto.GameState.Snake> snakes) {
        boolean[][] field = new boolean[config.getHeight()][config.getWidth()];
        int i, j;
        for (SnakesProto.GameState.Snake snake : snakes) {
            i = -1;
            j = -1;
            for (SnakesProto.GameState.Coord nextCoord : snake.getPointsList()) {
                if (i == -1 && j == -1) {
                    i = nextCoord.getX();
                    j = nextCoord.getY();
                    continue;
                }
                for (int coordX = i; coordX <= i + nextCoord.getX(); i++) {
                    for (int coordY = j; coordY <= j + nextCoord.getY(); coordY++) {
                        fill(field, coordX, coordY);
                    }
                }
                i += nextCoord.getX();
                j += nextCoord.getY();
            }
        }
        return field;
    }

    private void fill(boolean[][] field, int x, int y) {
        int maxX = config.getWidth();
        int maxY = config.getHeight();
        for (int i = x - 2; i <= x + 2; i++) {
            for (int j = y - 2; j <= y + 2; j++) {
                field[(maxY + j) % maxY][(maxX + i) % maxX] = true;
            }
        }
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

    public void updateState(SnakesProto.GameState nextState) {
        this.state = nextState;
        this.config = nextState.getConfig();
    }

    public int idByIpAndPort(String ip, int port) {
        List<SnakesProto.GamePlayer> players = state.getPlayers().getPlayersList();
        for (SnakesProto.GamePlayer player : players) {
            if (player.getIpAddress().equals(ip) && player.getPort() == port)
                return player.getId();
        }
        return -1;
    }

    public void iterateState(Map<Integer, SnakesProto.Direction> updateDirection) {
        int maxX = config.getWidth();
        int maxY = config.getHeight();

        SnakesProto.GameState.Builder builder = state.toBuilder();
        builder.setStateOrder(builder.getStateOrder() + 1);

        List<SnakesProto.GameState.Snake> snakes = builder.getSnakesList();
        Map<SnakesProto.GameState.Coord, List<SnakesProto.GameState.Snake>> contestPoints = new HashMap<>();

        for (SnakesProto.GameState.Snake snake : snakes) {
            SnakesProto.Direction newDirection = updateDirection.getOrDefault(snake.getPlayerId(), snake.getHeadDirection());

            if (newDirection.getNumber() + snake.getHeadDirection().getNumber() == 3 ||
                    newDirection.getNumber() + snake.getHeadDirection().getNumber() == 7) {
                newDirection = snake.getHeadDirection();
            }

            SnakesProto.GameState.Coord head = snake.getPointsList().get(0);
            SnakesProto.GameState.Coord bend = snake.getPointsList().get(1);
            SnakesProto.GameState.Coord newHead = null;
            SnakesProto.GameState.Coord newBend = null;

            switch (newDirection) {
                case UP:
                    newHead = head.toBuilder().setY((maxY + head.getY() - 1) % maxY).build();
                    newBend = SnakesProto.GameState.Coord.newBuilder().setY(1).setX(0).build();
                    break;
                case DOWN:
                    newHead = head.toBuilder().setY((maxY + head.getY() + 1) % maxY).build();
                    newBend = SnakesProto.GameState.Coord.newBuilder().setY(-1).setX(0).build();
                    break;
                case LEFT:
                    newHead = head.toBuilder().setX((maxX + head.getX() - 1) % maxX).build();
                    newBend = SnakesProto.GameState.Coord.newBuilder().setY(0).setX(1).build();
                    break;
                case RIGHT:
                    newHead = head.toBuilder().setX((maxX + head.getX() + 1) % maxX).build();
                    newBend = SnakesProto.GameState.Coord.newBuilder().setY(0).setX(-1).build();
                    break;
            }

            if (newDirection.equals(snake.getHeadDirection())) {
                newBend = newBend.toBuilder().setX(newBend.getX() + bend.getX()).setY(newBend.getY() + bend.getY()).build();
                snake.getPointsList().set(0, newHead);
            }
            else {
                snake.getPointsList().add(0, newHead);
            }
            snake.getPointsList().set(1, newBend);

            if (contestPoints.containsKey(newHead)) {
                contestPoints.get(newHead).add(snake);
            }
            else {
                ArrayList<SnakesProto.GameState.Snake> contestSnakes = new ArrayList<>();
                contestSnakes.add(snake);
                contestPoints.put(newHead, contestSnakes);
            }

            if (builder.getFoodsList().contains(newHead)) {
                builder.getFoodsList().remove(newHead);
                continue;
            }

            SnakesProto.GameState.Coord tail = snake.getPointsList().get(snake.getPointsCount() - 1);
            if (Math.abs(tail.getY() + tail.getX()) == 1) {
                snake.getPointsList().remove(snake.getPointsCount() - 1);
                continue;
            }

            SnakesProto.GameState.Coord newTail;
            if (tail.getX() == 0) {
                if (tail.getY() > 0) {
                    newTail = tail.toBuilder().setY(tail.getY() - 1).build();
                }
                else {
                    newTail = tail.toBuilder().setY(tail.getY() + 1).build();
                }
            }
            else {
                if (tail.getX() > 0) {
                    newTail = tail.toBuilder().setX(tail.getX() - 1).build();
                }
                else {
                    newTail = tail.toBuilder().setY(tail.getX() + 1).build();
                }
            }
            snake.getPointsList().set(snake.getPointsCount() - 1, newTail);
        }
        state = builder.build();
    }
}
