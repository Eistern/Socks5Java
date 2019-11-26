package net.fit;

import lombok.Data;
import lombok.NoArgsConstructor;
import net.fit.proto.SnakesProto;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

@Data
@NoArgsConstructor
public class GameModel {
    private final Object stateLock = new Object();
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
        builder.addSnakes(SnakesProto.GameState.Snake.newBuilder()
                .setPlayerId(0)
                .setState(SnakesProto.GameState.Snake.SnakeState.ALIVE)
                .setHeadDirection(SnakesProto.Direction.RIGHT)
                .build());

        builder.setStateOrder(0);
        builder.setConfig(config);
        this.state = builder.build();
    }

    public void init(SnakesProto.GameState old) {
        state = old;
        config = old.getConfig();
    }

    public boolean canJoin(String ip, int port) {
        List<SnakesProto.GamePlayer> players = new ArrayList<>(state.getPlayers().getPlayersList());
        for (SnakesProto.GamePlayer player : players) {
            if (player.getIpAddress().equals(ip) && player.getPort() == port)
                return false;
        }

        List<SnakesProto.GameState.Snake> snakes = getState().getSnakesList();
        boolean[][] field = generateBoolField(snakes, FillType.FIELD);

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

    private enum FillType {
        STRICT, FIELD
    }

    private boolean[][] generateBoolField(List<SnakesProto.GameState.Snake> snakes, FillType type) {
        boolean[][] field = new boolean[config.getHeight()][config.getWidth()];
        int i, j, xFrom, xTo, yFrom, yTo;
        boolean invertX = false, invertY = false;
        for (SnakesProto.GameState.Snake snake : snakes) {
            i = -1;
            j = -1;
            for (SnakesProto.GameState.Coord nextCoord : snake.getPointsList()) {
                if (i == -1 && j == -1) {
                    i = nextCoord.getX();
                    j = nextCoord.getY();
                    continue;
                }
                xFrom = i;
                xTo = i + nextCoord.getX();
                if (xTo < xFrom) {
                    xFrom *= -1;
                    xTo *= -1;
                    invertX = true;
                }
                yFrom = j;
                yTo = j + nextCoord.getY();
                if (yTo < yFrom) {
                    yFrom *= -1;
                    yTo *= -1;
                    invertY = true;
                }
                for (int coordX = xFrom; coordX <= xTo; coordX++) {
                    for (int coordY = yTo; coordY <= j + yFrom; coordY++) {
                        fill(field, invertX ? -1 * coordX : coordX, invertY ? -1 * coordY: coordY, type);
                    }
                }
                invertX = false;
                invertY = false;
                i += nextCoord.getX();
                j += nextCoord.getY();
            }
        }
        return field;
    }

    private void fill(boolean[][] field, int x, int y, FillType type) {
        int maxX = config.getWidth();
        int maxY = config.getHeight();
        if (type == FillType.FIELD) {
            for (int i = x - 2; i <= x + 2; i++) {
                for (int j = y - 2; j <= y + 2; j++) {
                    field[(maxY + j) % maxY][(maxX + i) % maxX] = true;
                }
            }
        }
        else {
            field[(maxY + y) % maxY][(maxX + x) % maxX] = true;
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

    public synchronized void addPlayer(String name, int port, String ip) {
        SnakesProto.GamePlayer.Builder playerBuilder = SnakesProto.GamePlayer.newBuilder();
        playerBuilder
                .setId(state.getPlayers().getPlayersCount())
                .setScore(0)
                .setRole(SnakesProto.NodeRole.NORMAL)
                .setIpAddress(ip)
                .setPort(port)
                .setName(name)
                .setType(SnakesProto.PlayerType.HUMAN);
        SnakesProto.GamePlayer player = playerBuilder.build();

        SnakesProto.GameState.Builder builder = state.toBuilder();
        List<SnakesProto.GamePlayer> players = new ArrayList<>(builder.getPlayers().getPlayersList());
        players.add(player);
        builder.setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players).build());

        SnakesProto.GameState.Snake.Builder snakeBuilder = SnakesProto.GameState.Snake.newBuilder();
        SnakesProto.GameState.Coord coordHead = SnakesProto.GameState.Coord.newBuilder().setX(freeX).setY(freeY).build();
        SnakesProto.GameState.Coord coordTail = SnakesProto.GameState.Coord.newBuilder().setX(-1).setY(0).build();

        snakeBuilder.setHeadDirection(SnakesProto.Direction.RIGHT)
                .setPlayerId(player.getId())
                .addPoints(coordHead)
                .addPoints(coordTail)
                .setState(SnakesProto.GameState.Snake.SnakeState.ALIVE);
        builder.addSnakes(snakeBuilder);
        this.state = builder.build();
    }

    public synchronized void updateState(SnakesProto.GameState nextState) {
        if (nextState.getStateOrder() > state.getStateOrder()) {
            this.state = nextState;
            this.config = nextState.getConfig();
        }
    }

    public synchronized int idByIpAndPort(String ip, int port) {
        List<SnakesProto.GamePlayer> players = state.getPlayers().getPlayersList();
        for (SnakesProto.GamePlayer player : players) {
            if (player.getIpAddress().equals(ip) && player.getPort() == port)
                return player.getId();
        }
        return -1;
    }

    public synchronized void iterateState(Map<Integer, SnakesProto.Direction> updateDirection) {
        int maxX = config.getWidth();
        int maxY = config.getHeight();

        SnakesProto.GameState.Builder builder = state.toBuilder();
        builder.setStateOrder(builder.getStateOrder() + 1);

        SnakesProto.GameState.Coord.Builder coordBuilder = SnakesProto.GameState.Coord.newBuilder();

        List<SnakesProto.GameState.Snake> snakes = new ArrayList<>(builder.getSnakesList());
        List<SnakesProto.GameState.Coord> food = new ArrayList<>(builder.getFoodsList());
        Map<SnakesProto.GameState.Coord, List<SnakesProto.GameState.Snake>> contestPoints = new HashMap<>();

        for (SnakesProto.GameState.Snake snake : snakes) {
            SnakesProto.Direction newDirection = updateDirection.getOrDefault(snake.getPlayerId(), snake.getHeadDirection());

            //Если новое направление противоположно текущему, то не учитываем его
            if (newDirection.getNumber() + snake.getHeadDirection().getNumber() == 3 ||
                    newDirection.getNumber() + snake.getHeadDirection().getNumber() == 7) {
                newDirection = snake.getHeadDirection();
            }

            //Выбираем голову и следующий за ней узел, обновляем данные
            SnakesProto.GameState.Coord head = snake.getPointsList().get(0);
            SnakesProto.GameState.Coord bend = snake.getPointsList().get(1);
            SnakesProto.GameState.Coord newHead = null;
            SnakesProto.GameState.Coord newBend = null;

            switch (newDirection) {
                case UP:
                    newHead = head.toBuilder().setY((maxY + head.getY() - 1) % maxY).build();
                    newBend = coordBuilder.setY(1).setX(0).build();
                    break;
                case DOWN:
                    newHead = head.toBuilder().setY((maxY + head.getY() + 1) % maxY).build();
                    newBend = coordBuilder.setY(-1).setX(0).build();
                    break;
                case LEFT:
                    newHead = head.toBuilder().setX((maxX + head.getX() - 1) % maxX).build();
                    newBend = coordBuilder.setY(0).setX(1).build();
                    break;
                case RIGHT:
                    newHead = head.toBuilder().setX((maxX + head.getX() + 1) % maxX).build();
                    newBend = coordBuilder.setY(0).setX(-1).build();
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

            //Добавляем новую змею, претендующую на клетку поля
            if (contestPoints.containsKey(newHead)) {
                contestPoints.get(newHead).add(snake);
            }
            else {
                ArrayList<SnakesProto.GameState.Snake> contestSnakes = new ArrayList<>();
                contestSnakes.add(snake);
                contestPoints.put(newHead, contestSnakes);
            }

            //Если змея попала на клетку с едой, удаляем еду, не двигаем хвост
            if (food.contains(newHead)) {
                food.remove(newHead);
                continue;
            }

            SnakesProto.GameState.Coord tail = snake.getPointsList().get(snake.getPointsCount() - 1);
            //Если хвост был сдвиут на одну клетку, удаляем содержащий его узел
            if (Math.abs(tail.getY() + tail.getX()) == 1) {
                snake.getPointsList().remove(snake.getPointsCount() - 1);
                continue;
            }

            //Передвигаем узел с хвостом
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
        //Закончили двигать змеек (были изменены списки snakes и food)

        //Начинаем удалять змеек
        List<SnakesProto.GameState.Snake> deadSnakes = new ArrayList<>();

        //Проверка на столкновения "голова-голова"
        contestPoints.forEach((coord, contestSnakes) -> {
            if (contestSnakes.size() > 1) {
                snakes.removeAll(contestSnakes);
                deadSnakes.addAll(contestSnakes);
            }
        });

        //Проверка на столкновение "голова-тело"
        boolean[][] contestField = generateBoolField(snakes, FillType.STRICT);
        for (int i = 0; i < config.getHeight(); i++) {
            for (int j = 0; j < config.getWidth(); j++) {
                if (contestField[(maxY + i) % maxY][(maxX + j) % maxX] && (
                        contestField[(maxY + i - 1) % maxY][(maxX + j) % maxX] && contestField[(maxY + i) % maxY][(maxX + j - 1) % maxX] && (
                                contestField[(maxY + i + 1) % maxY][(maxX + j) % maxX] || contestField[(maxY + i) % maxY][(maxX + j + 1) % maxX]
                                )
                        ||
                        contestField[(maxY + i + 1) % maxY][(maxX + j) % maxX] && contestField[(maxY + i) % maxY][(maxX + j + 1) % maxX] && (
                                contestField[(maxY + i - 1) % maxY][(maxX + j) % maxX] || contestField[(maxY + i) % maxY][(maxX + j - 1) % maxX]
                                )
                        )) {
                    Iterator<SnakesProto.GameState.Snake> iterator = snakes.iterator();
                    while (iterator.hasNext()) {
                        SnakesProto.GameState.Snake nextSnake = iterator.next();
                        SnakesProto.GameState.Coord head = nextSnake.getPoints(0);
                        if (head.getY() == i && head.getX() == j) {
                            iterator.remove();
                            deadSnakes.add(nextSnake);
                        }
                    }
                }
            }
        }

        //Генерация еды из мертвых змеек
        int i, j, xFrom, xTo, yFrom, yTo;
        boolean invertX = false, invertY = false;
        for (SnakesProto.GameState.Snake snake : deadSnakes) {
            i = -1;
            j = -1;
            for (SnakesProto.GameState.Coord nextCoord : snake.getPointsList()) {
                if (i == -1 && j == -1) {
                    i = nextCoord.getX();
                    j = nextCoord.getY();
                    continue;
                }
                xFrom = i;
                xTo = i + nextCoord.getX();
                if (xTo < xFrom) {
                    xFrom *= -1;
                    xTo *= -1;
                    invertX = true;
                }
                yFrom = j;
                yTo = j + nextCoord.getY();
                if (yTo < yFrom) {
                    yFrom *= -1;
                    yTo *= -1;
                    invertY = true;
                }
                for (int coordX = xFrom; coordX <= xTo; coordX++) {
                    for (int coordY = yTo; coordY <= j + yFrom; coordY++) {
                        if (Math.random() < config.getDeadFoodProb()) {
                            food.add(coordBuilder
                                    .setY(invertY ? -1 * coordY : coordY)
                                    .setX(invertX ? -1 * coordX : coordX)
                                    .build());
                        }
                    }
                }
                invertX = false;
                invertY = false;
                i += nextCoord.getX();
                j += nextCoord.getY();
            }
        }

        //Генерируем недостающую еду
        int foodX, foodY;
        if (food.size() < config.getFoodStatic() + config.getFoodPerPlayer() * state.getPlayers().getPlayersCount()) {
            contestField = generateBoolField(snakes, FillType.STRICT);
            while (food.size() < config.getFoodStatic() + config.getFoodPerPlayer() * state.getPlayers().getPlayersCount()) {
                foodX = (int) (Math.random() * maxX);
                foodY = (int) (Math.random() * maxY);
                SnakesProto.GameState.Coord foodCoord = coordBuilder.setX(foodX).setY(foodY).build();
                if (!contestField[foodY][foodX] && !food.contains(foodCoord)) {
                    food.add(foodCoord);
                }
            }
        }

        //TODO Обновлять счет игроков

        builder.clearSnakes();
        builder.addAllSnakes(snakes);
        state = builder.build();
    }
}
