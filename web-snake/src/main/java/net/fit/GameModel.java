package net.fit;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.fit.proto.SnakesProto;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EqualsAndHashCode(callSuper = false)
@Data
@NoArgsConstructor
public class GameModel extends Observable {
    private SnakesProto.GameConfig config;
    private SnakesProto.GameState state;
    private SnakesProto.NodeRole role;
    private InetSocketAddress hostAddr;
    private int ownId = -1;
    private int freeX;
    private int freeY;

    public synchronized SnakesProto.GameConfig getConfig() {
        return config;
    }

    public SnakesProto.GameState getState() {
        return state;
    }

    public SnakesProto.GamePlayer getFirstOfRole(SnakesProto.NodeRole role) {
        return state.getPlayers().getPlayersList().parallelStream().filter(player -> player.getRole() == role).findFirst().orElse(null);
    }

    public synchronized InetSocketAddress getHostAddr() {
        if (hostAddr == null) {
            InetSocketAddress result;
            SnakesProto.GamePlayer master = getFirstOfRole(SnakesProto.NodeRole.MASTER);
            if (master != null) {
                result = new InetSocketAddress(master.getIpAddress(), master.getPort());
                hostAddr = result;
            }
            return hostAddr;
        }
        else {
            return hostAddr;
        }
    }

    public synchronized void setPlayerRole(int playerId, SnakesProto.NodeRole newRole) {
        if (role != SnakesProto.NodeRole.MASTER)
            return;
        List<SnakesProto.GamePlayer> players = new ArrayList<>(this.state.getPlayers().getPlayersList());
        SnakesProto.GamePlayer modifyingPlayer = players.parallelStream().filter(player -> player.getId() == playerId).findFirst().orElse(null);
        if (modifyingPlayer != null)
            if (modifyingPlayer.getRole() != newRole) {
                players.remove(modifyingPlayer);
                players.add(modifyingPlayer.toBuilder().setRole(newRole).build());
                this.state = state.toBuilder().setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players).build()).build();
            }
    }

    public synchronized boolean becomeMaster(int senderId, SocketAddress address) {
        if (this.role != SnakesProto.NodeRole.DEPUTY)
            return false;
        this.role = SnakesProto.NodeRole.MASTER;
        List<SnakesProto.GamePlayer> players = new ArrayList<>(state.getPlayers().getPlayersList());
        SnakesProto.GamePlayer deputy = getFirstOfRole(SnakesProto.NodeRole.DEPUTY);
        if (deputy == null) {
            return false;
        }
        players.remove(deputy);
        SnakesProto.GamePlayer previousMaster = players.parallelStream().filter(player -> player.getId() == senderId).findFirst().orElse(null);
        if (previousMaster != null) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
            players.remove(previousMaster);
            players.add(previousMaster.toBuilder().setIpAddress(inetSocketAddress.getAddress().getHostAddress()).setPort(inetSocketAddress.getPort()).build());
        }
        players.add(deputy.toBuilder().setRole(SnakesProto.NodeRole.MASTER).build());
        this.state = state.toBuilder().setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players).build()).build();
        this.hostAddr = null;
        return true;
    }

    public void init(SnakesProto.GameConfig config) {
        this.config = config;
        this.hostAddr = null;
        this.role = SnakesProto.NodeRole.MASTER;
        SnakesProto.GameState.Builder builder = SnakesProto.GameState.newBuilder();

        List<SnakesProto.GamePlayer> players = new ArrayList<>();
        builder.setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(players));

        builder.setStateOrder(0);
        builder.setConfig(config);
        this.state = builder.build();
    }

    public void init(SnakesProto.GameConfig config, String ip, int port, String name) {
        init(config);
        this.role = SnakesProto.NodeRole.MASTER;
        canJoin(ip, port);
        this.ownId = 1;
        addPlayer(name, port, ip);
    }

    public boolean canJoin(String ip, int port) {
        List<SnakesProto.GamePlayer> players = state.getPlayers().getPlayersList();
        for (SnakesProto.GamePlayer player : players) {
            if (player.getIpAddress().equals(ip) && player.getPort() == port)
                return false;
        }

        List<SnakesProto.GameState.Snake> snakes = state.getSnakesList();
        int[][] field = generateIntField(snakes, FillType.FIELD);

        List<SnakesProto.GameState.Coord> foods = state.getFoodsList();
        for (SnakesProto.GameState.Coord food : foods) {
            fill(field, food.getX(), food.getY(), -1, FillType.FIELD);
        }

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
        return state.getPlayers().toBuilder().build();
    }

    public synchronized void removePlayers(List<SnakesProto.GamePlayer> players) {
        List<SnakesProto.GamePlayer> playerList = new ArrayList<>(state.getPlayers().getPlayersList());
        List<Integer> playersId = players.parallelStream().mapToInt(SnakesProto.GamePlayer::getId).boxed().collect(Collectors.toList());
        playerList.removeIf(player -> playersId.contains(player.getId()));
        this.state = state.toBuilder().setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(playerList).build()).build();
    }

    public synchronized SnakesProto.GameMessage.RoleChangeMsg addPlayer(String name, int port, String ip) {
        System.out.println("Adding " + ip + ":" + port);
        SnakesProto.GameState.Builder builder = state.toBuilder();
        List<SnakesProto.GamePlayer> players = new ArrayList<>(builder.getPlayers().getPlayersList());

        SnakesProto.NodeRole nextRole = SnakesProto.NodeRole.NORMAL;
        if (players.size() == 0) {
            nextRole = SnakesProto.NodeRole.MASTER;
        } else if (players.parallelStream().noneMatch(player -> player.getRole() == SnakesProto.NodeRole.DEPUTY)) {
            nextRole = SnakesProto.NodeRole.DEPUTY;
        }

        SnakesProto.GamePlayer.Builder playerBuilder = SnakesProto.GamePlayer.newBuilder();
        playerBuilder
                .setId(state.getPlayers().getPlayersCount() + 1)
                .setScore(0)
                .setRole(nextRole)
                .setIpAddress(ip)
                .setPort(port)
                .setName(name)
                .setType(SnakesProto.PlayerType.HUMAN);
        SnakesProto.GamePlayer player = playerBuilder.build();

        players.add(player);
        builder.clearPlayers();
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

        if (nextRole == SnakesProto.NodeRole.NORMAL) {
            return null;
        }
        return SnakesProto.GameMessage.RoleChangeMsg.newBuilder().setSenderRole(SnakesProto.NodeRole.MASTER).setReceiverRole(nextRole).build();
    }

    public SnakesProto.GamePlayer reelectDeputy() {
        List<SnakesProto.GamePlayer> currentPlayers = new ArrayList<>(state.getPlayers().getPlayersList());
        SnakesProto.GamePlayer candidate = currentPlayers.parallelStream().filter(player -> player.getRole() != SnakesProto.NodeRole.MASTER && player.getRole() != SnakesProto.NodeRole.VIEWER).findFirst().orElse(null);
        if (candidate != null) {
            currentPlayers.remove(candidate);
            currentPlayers.add(candidate.toBuilder().setRole(SnakesProto.NodeRole.DEPUTY).build());
            this.state = state.toBuilder().setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(currentPlayers).build()).build();
        }
        return candidate;
    }

    public synchronized void updateState(SnakesProto.GameState nextState, InetSocketAddress hostAddress) {
        if (role == SnakesProto.NodeRole.MASTER) {
            System.err.println("SEND update to MASTER");
            return;
        }
        if (hostAddr == null) {
            this.hostAddr = hostAddress;
        }
        if (hostAddress.equals(this.hostAddr) && nextState.getStateOrder() > state.getStateOrder()) {
            this.state = nextState;
            this.config = nextState.getConfig();
            this.setChanged();
            this.notifyObservers();
            System.out.println("State changed");
        }
    }

    public synchronized int idByIpAndPort(String ip, int port) {
        System.out.println("Find " + ip + ":" + port);
        List<SnakesProto.GamePlayer> players = new ArrayList<>(state.getPlayers().getPlayersList());

        if (hostAddr != null && ip.equals(hostAddr.getAddress().getHostAddress()) && port == hostAddr.getPort()) {
            System.out.println("TRY TO FIND MASTER");
            SnakesProto.GamePlayer result = players.parallelStream().filter(player -> player.getRole() == SnakesProto.NodeRole.MASTER).findFirst().orElse(null);
            if (result != null) {
                return result.getId();
            }
            return -1;
        }

        SnakesProto.GamePlayer result = players.parallelStream().filter(player -> player.getIpAddress().equals(ip) && player.getPort() == port).findFirst().orElse(null);
        if (result == null) {
            System.out.println("Player not found");
            return -1;
        }
        return result.getId();
    }

    public synchronized List<SnakesProto.GamePlayer> iterateState(Map<Integer, SnakesProto.Direction> updateDirection) {
        int maxX = config.getWidth();
        int maxY = config.getHeight();

        SnakesProto.GameState.Builder builder = state.toBuilder();
        builder.setStateOrder(builder.getStateOrder() + 1);

        SnakesProto.GameState.Coord.Builder coordBuilder = SnakesProto.GameState.Coord.newBuilder();

        List<SnakesProto.GameState.Snake> snakes = new ArrayList<>(builder.getSnakesList());
        List<SnakesProto.GameState.Coord> food = new ArrayList<>(builder.getFoodsList());
        List<SnakesProto.GamePlayer> players = new ArrayList<>(builder.getPlayers().getPlayersList());

        Collector<SnakesProto.GamePlayer, ?, Map<Integer, Integer>> collector = Collectors.toMap(SnakesProto.GamePlayer::getId, SnakesProto.GamePlayer::getScore);
        Map<Integer, Integer> scoreMap = players.parallelStream().collect(collector);

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
                scoreMap.put(resultSnake.getPlayerId(), scoreMap.getOrDefault(resultSnake.getPlayerId(), 0) + 1);
                continue;
            }

            SnakesProto.GameState.Coord tail = pointsList.get(pointsList.size() - 1);
            SnakesProto.GameState.Coord absoluteCoord = getTailCoords(pointsList);
            fill(contestField, absoluteCoord.getX(), absoluteCoord.getY(), 0, FillType.STRICT);

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
        List<Integer> deadPlayerIds = new ArrayList<>();
        List<SnakesProto.GameState.Snake> deadSnakes = new ArrayList<>();

        //Проверка на столкновение "голова-тело"
        int[][] finalContestField = contestField;
        contestPoints.forEach((coord, contestSnakes) -> {
            if (finalContestField[coord.getY()][coord.getX()] != 0) {
                scoreMap.put(finalContestField[coord.getY()][coord.getX()], scoreMap.get(finalContestField[coord.getY()][coord.getX()]) + contestSnakes.size());
                snakes.removeAll(contestSnakes);
                deadSnakes.addAll(contestSnakes);
                deadPlayerIds.addAll(contestSnakes.parallelStream().map(SnakesProto.GameState.Snake::getPlayerId).collect(Collectors.toCollection(ArrayList::new)));
            }
        });

        //Проверка на столкновения "голова-голова"
        contestPoints.forEach((coord, contestSnakes) -> {
            if (contestSnakes.size() > 1) {
                snakes.removeAll(contestSnakes);
                deadSnakes.addAll(contestSnakes);
                deadPlayerIds.addAll(contestSnakes.parallelStream().map(SnakesProto.GameState.Snake::getPlayerId).collect(Collectors.toCollection(ArrayList::new)));
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
                    for (int coordY = yFrom; coordY <= yTo; coordY++) {
                        int Y = (maxY + (invertY ? -1 * coordY : coordY)) % maxY;
                        int X = (maxX + (invertX ? -1 * coordX : coordX)) % maxX;
                        if (Math.random() < config.getDeadFoodProb() && contestField[Y][X] == 0) {
                            food.add(coordBuilder
                                    .setY(Y)
                                    .setX(X)
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

        List<SnakesProto.GamePlayer> deadPlayers = new ArrayList<>();
        List<SnakesProto.GamePlayer> updatedPlayers = new ArrayList<>();
        players.forEach(player -> {
            SnakesProto.GamePlayer.Builder playerBuilder = player.toBuilder();
            if (deadPlayerIds.contains(player.getId())) {
                deadPlayers.add(player);
                playerBuilder.setRole(SnakesProto.NodeRole.VIEWER);
            }
            playerBuilder.setScore(scoreMap.get(player.getId()));
            updatedPlayers.add(playerBuilder.build());
        });

        builder.clearPlayers();
        builder.setPlayers(SnakesProto.GamePlayers.newBuilder().addAllPlayers(updatedPlayers).build());
        builder.clearSnakes();
        builder.addAllSnakes(snakes);
        builder.clearFoods();
        builder.addAllFoods(food);
        state = builder.build();
        this.setChanged();
        this.notifyObservers();

        return deadPlayers;
    }
}
