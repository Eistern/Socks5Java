package net.fit.gui.view;

import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.activities.NetworkManager;
import net.fit.proto.SnakesProto;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.InetSocketAddress;

@RequiredArgsConstructor
public class ButtonListener implements KeyListener {
    private final GameModel model;
    private final NetworkManager networkManager;
    private SnakesProto.GameMessage.SteerMsg.Builder steerBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
    private SnakesProto.GameMessage.Builder messageBuilder = SnakesProto.GameMessage.newBuilder();

    @Override
    public void keyTyped(KeyEvent e) {
        keyPressed(e);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        SnakesProto.Direction direction = SnakesProto.Direction.RIGHT;
        switch (e.getExtendedKeyCode()) {
            case 37:
                direction = SnakesProto.Direction.LEFT;
                break;
            case 38:
                direction = SnakesProto.Direction.UP;
                break;
            case 39:
                direction = SnakesProto.Direction.RIGHT;
                break;
            case 40:
                direction = SnakesProto.Direction.DOWN;
                break;
            default:
                break;
        }
        try {
            networkManager.commit(messageBuilder
                    .setMsgSeq(networkManager.getSequenceNum())
                    .setSteer(steerBuilder.setDirection(direction)).build(), new InetSocketAddress("26.83.213.208", 9192));

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
