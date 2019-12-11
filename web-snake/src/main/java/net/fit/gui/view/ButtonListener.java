package net.fit.gui.view;

import lombok.RequiredArgsConstructor;
import net.fit.GameModel;
import net.fit.activities.NetworkManager;
import net.fit.proto.SnakesProto;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

@RequiredArgsConstructor
public class ButtonListener implements KeyListener {
    private final GameModel model;
    private final NetworkManager networkManager;
    private SnakesProto.GameMessage.SteerMsg.Builder steerBuilder = SnakesProto.GameMessage.SteerMsg.newBuilder();
    private SnakesProto.GameMessage.Builder messageBuilder = SnakesProto.GameMessage.newBuilder();

    @Override
    public void keyPressed(KeyEvent e) {
        keyTyped(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (model.getRole() == SnakesProto.NodeRole.VIEWER)
            return;
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
            System.out.println("SENDING STEER TO " + model.getHost());
            networkManager.commit(messageBuilder
                    .setMsgSeq(networkManager.getSequenceNum())
                    .setSteer(steerBuilder.setDirection(direction)).build(), model.getHost());

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}
