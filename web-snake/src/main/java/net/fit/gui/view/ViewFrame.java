package net.fit.gui.view;

import net.fit.GameModel;
import net.fit.activities.NetworkManager;
import net.fit.proto.SnakesProto;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ViewFrame extends JFrame implements Observer {
    private final GameModel model;

    public ViewFrame(GameModel model, NetworkManager manager) {
        super("Snake");
        this.model = model;

        setBackground(Color.WHITE);
        int maxHeight = 480;
        int maxWidth = 640;
        setSize(maxWidth, maxHeight);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addKeyListener(new ButtonListener(model, manager));

        model.addObserver(this);
    }

    public void paint(Graphics g) {
        super.paint(g);
        int maxWidth = getWidth();
        int maxHeight = getHeight();
        Graphics2D graphics2D = (Graphics2D) g;
        float width = model.getConfig().getWidth() + 6;
        float height = model.getConfig().getHeight() + 6;
        float boxWidth = maxWidth / width, boxHeight = maxHeight / height;
        float stroke = Math.max(boxHeight, boxWidth) / 2;

        graphics2D.setColor(Color.BLACK);
        for (int i = 3; i < width - 2; i++) {
            Line2D line = new Line2D.Float(boxWidth * i, boxHeight * 3, boxWidth * i, maxHeight - boxHeight * 3);
            graphics2D.draw(line);
        }
        for (int i = 3; i < height - 2; i++) {
            Line2D line = new Line2D.Float(boxWidth * 3, boxHeight * i, maxWidth - boxWidth * 3, boxHeight * i);
            graphics2D.draw(line);
        }

        float maxBoxWidth = boxWidth * (width - 3);
        float minBoxWidth = boxWidth * 3;
        float maxBoxHeight = boxHeight * (height - 3);
        float minBoxHeight = boxHeight * 3;

        graphics2D.setStroke(new BasicStroke(stroke));
        List<SnakesProto.GameState.Snake> snakes = model.getState().getSnakesList();
        float currentX, currentY, shiftX, shiftY;
        for (SnakesProto.GameState.Snake snake : snakes) {
            graphics2D.setColor(new Color((int) Math.sinh(snake.getPlayerId() + 50) % 0x10000110));
            currentX = -1;
            currentY = -1;
            Ellipse2D.Float head = null;
            List<SnakesProto.GameState.Coord> coords = snake.getPointsList();
            for (SnakesProto.GameState.Coord coord : coords) {
                shiftX = 0;
                shiftY = 0;
                if (currentX == -1 && currentY == -1) {
                    currentX = (float) ((coord.getX() + 3.5) * boxWidth);
                    currentY = (float) ((coord.getY() + 3.5) * boxHeight);
                    head = new Ellipse2D.Float(currentX, currentY, stroke / 3, stroke / 3);
                    continue;
                }

                if (currentX + (coord.getX() * boxWidth) > maxBoxWidth) {
                    Line2D.Float line = new Line2D.Float(
                            currentX,
                            currentY,
                            maxBoxWidth,
                            currentY + (coord.getY() * boxHeight));
                    graphics2D.draw(line);
                    shiftX = -1 * (maxBoxWidth - currentX);
                    currentX = minBoxWidth;
                }
                if (currentX + (coord.getX() * boxWidth) < minBoxWidth) {
                    Line2D.Float line = new Line2D.Float(
                            currentX,
                            currentY,
                            minBoxWidth,
                            currentY + (coord.getY() * boxHeight));
                    graphics2D.draw(line);
                    shiftX = (currentX - minBoxWidth);
                    currentX = maxBoxWidth;
                }
                if (currentY + (coord.getY() * boxHeight) > maxBoxHeight) {
                    Line2D.Float line = new Line2D.Float(
                            currentX,
                            currentY,
                            currentX + (coord.getX() * boxWidth),
                            maxBoxHeight);
                    graphics2D.draw(line);
                    shiftY = -1 * (maxBoxHeight - currentY);
                    currentY = minBoxHeight;
                }
                if (currentY + (coord.getY() * boxHeight) < minBoxHeight) {
                    Line2D.Float line = new Line2D.Float(
                            currentX,
                            currentY,
                            currentX + (coord.getX() * boxWidth),
                            minBoxHeight);
                    graphics2D.draw(line);
                    shiftY = (currentY - minBoxHeight);
                    currentY = maxBoxHeight;
                }

                Line2D.Float line = new Line2D.Float(
                        currentX,
                        currentY,
                        currentX + (coord.getX() * boxWidth) + shiftX,
                        currentY + (coord.getY() * boxHeight) + shiftY);
                graphics2D.draw(line);
                currentX += coord.getX() * boxWidth + shiftX;
                currentY += coord.getY() * boxHeight + shiftY;
            }
            graphics2D.setColor(Color.BLACK);
            graphics2D.draw(head);
        }

        List<SnakesProto.GameState.Coord> foods = model.getState().getFoodsList();
        graphics2D.setColor(Color.RED);
        for (SnakesProto.GameState.Coord coord : foods) {
            currentX = (float) ((coord.getX() + 3.5) * boxWidth);
            currentY = (float) ((coord.getY() + 3.5) * boxHeight);
            Ellipse2D food = new Ellipse2D.Float(currentX, currentY, stroke / 3, stroke / 3);
            graphics2D.draw(food);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        this.repaint();
    }
}
