package net.fit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.fit.proto.SnakesProto;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
public class GameModel extends Observable {
    private final Object stateLock = new Object();
    private SnakesProto.GameConfig config;
    private SnakesProto.GameState state;
    private int freeX;
    private int freeY;

    public void init(SnakesProto.GameConfig config) {
        this.config = config;
        SnakesProto.GameState.Builder builder = SnakesProto.GameState.newBuilder();

        List<SnakesProto.GamePlayer> players = new ArrayList<>();
        builder.setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players));

        builder.setStateOrder(0);
        builder.setConfig(config);
        this.state = builder.build();
    }

    public boolean canJoin(String ip, int port) {
        List<SnakesProto.GamePlayer> players = new ArrayList<>(state.getPlayers().getPlayersList());
        for (SnakesProto.GamePlayer player : players) {
            if (player.getIpAddress().equals(ip) && player.getPort() == port)
                return false;
        }

        List<SnakesProto.GameState.Snake> snakes = getState().getSnakesList();
        int[][] field = generateIntField(snakes, FillType.FIELD);

        for (int x = 0; x < config.getHeight(); x++) {
            for (int y = 0; y < config.getWidth(); y++) {
                if (field[x][y] == 0) {
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

    private int[][] generateIntField(List<SnakesProto.GameState.Snake> snakes, FillType type) {
        int[][] field = new int[config.getHeight()][config.getWidth()];
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
                    for (int coordY = yFrom; coordY <= yTo; coordY++) {
                        fill(field, invertX ? -1 * coordX : coordX, invertY ? -1 * coordY: coordY, snake.getPlayerId(), type);
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

    private void fill(int[][] field, int x, int y, int with, FillType type) {
        int maxX = config.getWidth();
        int maxY = config.getHeight();
        if (type == FillType.FIELD) {
            for (int i = x - 2; i <= x + 2; i++) {
                for (int j = y - 2; j <= y + 2; j++) {
                    field[(maxY + j) % maxY][(maxX + i) % maxX] = with;
                }
            }
        }
        else {
            field[(maxY + y) % maxY][(maxX + x) % maxX] = with;
        }
    }

    private SnakesProto.GameState.Coord getTailCoords(List<SnakesProto.GameState.Coord> coords) {
        int x = -1;
        int y = -1;
        for (SnakesProto.GameState.Coord coord : coords) {
            if (x == -1 && y == -1) {
                x = coord.getX();
                y = coord.getY();
                continue;
            }
            x += coord.getX();
            y += coord.getY();
        }
        return SnakesProto.GameState.Coord.newBuilder().setX(x).setY(y).build();
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
        System.out.println(ip + ":" + port);
        SnakesProto.GamePlayer.Builder playerBuilder = SnakesProto.GamePlayer.newBuilder();
        playerBuilder
                .setId(state.getPlayers().getPlayersCount() + 1)
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
        this.setChanged();
        this.notifyObservers();
    }

    public synchronized void updateState(SnakesProto.GameState nextState) {
        if (nextState.getStateOrder() > state.getStateOrder()) {
            this.state = nextState;
            this.config = nextState.getConfig();
            this.setChanged();
            this.notifyObservers();
        }
    }

    public synchronized int idByIpAndPort(String ip, int port) {
        System.out.println("Find " + ip + ":" + port);
        List<SnakesProto.GamePlayer> players = state.getPlayers().getPlayersList();
        for (SnakesProto.GamePlayer player : players) {
            if (player.getIpAddress().equals(ip) && player.getPort() == port)
                return player.getId();
        }
        System.out.println("Player not found");
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

        int[][] contestField = generateIntField(builder.getSnakesList(), FillType.STRICT);
        for (int i = 0; i < snakes.size(); i++) {
            SnakesProto.GameState.Snake snake = snakes.get(i);
            List<SnakesProto.GameState.Coord> pointsList = new ArrayList<>(snake.getPointsList());
            SnakesProto.Direction newDirection = updateDirection.getOrDefault(snake.getPlayerId(), snake.getHeadDirection());

            //Если новое направление противоположно текущему, то не учитываем его
            if (newDirection.getNumber() + snake.getHeadDirection().getNumber() == 3 ||
                    newDirection.getNumber() + snake.getHeadDirection().getNumber() == 7) {
                newDirection = snake.getHeadDirection();
            }

            //Выбираем голову и следующий за ней узел, обновляем данные
            SnakesProto.GameState.Coord head = pointsList.get(0);
            SnakesProto.GameState.Coord bend = pointsList.get(1);
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
                pointsList.set(0, newHead);
            } else {
                pointsList.add(0, newHead);
            }
            pointsList.set(1, newBend);

            //Добавляем новую змею, претендующую на клетку поля
            if (!contestPoints.containsKey(newHead)) {
                ArrayList<SnakesProto.GameState.Snake> contestSnakes = new ArrayList<>();
                contestPoints.put(newHead, contestSnakes);
            }

            //Если змея попала на клетку с едой, удаляем еду, не двигаем хвост
            if (food.contains(newHead)) {
                food.remove(newHead);
                SnakesProto.GameState.Snake resultSnake = snake.toBuilder().clearPoints().addAllPoints(pointsList).setHeadDirection(newDirection).build();
                snakes.set(i, resultSnake);
                contestPoints.get(newHead).add(resultSnake);
                continue;
            }

            SnakesProto.GameState.Coord tail = pointsList.get(pointsList.size() - 1);
            SnakesProto.GameState.Coord absoluteCoord = getTailCoords(pointsList);
            contestField[(maxX + absoluteCoord.getX()) % maxX][(maxY + absoluteCoord.getY()) % maxY] = 0;

            //Если хвост был сдвиут на одну клетку, удаляем содержащий его узел
            if (Math.abs(tail.getY() + tail.getX()) == 1) {
                pointsList.remove(pointsList.size() - 1);
                SnakesProto.GameState.Snake resultSnake = snake.toBuilder().clearPoints().addAllPoints(pointsList).setHeadDirection(newDirection).build();
                snakes.set(i, resultSnake);
                contestPoints.get(newHead).add(resultSnake);
                continue;
            }

            //Передвигаем узел с хвостом
            SnakesProto.GameState.Coord newTail;
            if (tail.getX() == 0) {
                if (tail.getY() > 0) {
                    newTail = tail.toBuilder().setY(tail.getY() - 1).build();
                } else {
                    newTail = tail.toBuilder().setY(tail.getY() + 1).build();
                }
            } else {
                if (tail.getX() > 0) {
                    newTail = tail.toBuilder().setX(tail.getX() - 1).build();
                } else {
                    newTail = tail.toBuilder().setX(tail.getX() + 1).build();
                }
            }
            pointsList.set(pointsList.size() - 1, newTail);

            SnakesProto.GameState.Snake resultSnake = snake.toBuilder().clearPoints().addAllPoints(pointsList).setHeadDirection(newDirection).build();
            snakes.set(i, resultSnake);
            contestPoints.get(newHead).add(resultSnake);
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
        int[][] finalContestField = contestField;
        contestPoints.forEach((coord, contestSnakes) -> {
            if (finalContestField[coord.getX()][coord.getY()] != 0) {
                System.out.println(Arrays.deepToString(finalContestField));
                snakes.removeAll(contestSnakes);
                deadSnakes.addAll(contestSnakes);
            }
        });

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
                                    .setY((maxY + (invertY ? -1 * coordY : coordY)) % maxY)
                                    .setX((maxX + (invertX ? -1 * coordX : coordX)) % maxX)
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
            contestField = generateIntField(snakes, FillType.STRICT);
            while (food.size() < config.getFoodStatic() + config.getFoodPerPlayer() * state.getPlayers().getPlayersCount()) {
                foodX = (int) (Math.random() * maxX);
                foodY = (int) (Math.random() * maxY);
                SnakesProto.GameState.Coord foodCoord = coordBuilder.setX(foodX).setY(foodY).build();
                if (contestField[foodY][foodX] == 0 && !food.contains(foodCoord)) {
                    food.add(foodCoord);
                }
            }
        }

        //TODO Обновлять счет игроков

        builder.clearSnakes();
        builder.addAllSnakes(snakes);
        builder.clearFoods();
        builder.addAllFoods(food);
        state = builder.build();
        this.setChanged();
        this.notifyObservers();
    }
}
