package net.fit;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChannelContext {
    @Getter(value = AccessLevel.PRIVATE) private final SelectionKey sourceKey;
    @ToString.Exclude private ChannelContext pairedContext = null;
    private boolean isAuthorized = false;
    private boolean isPaired = false;
    private boolean wantClose = false;
    private AuthenticationRequest authenticationRequest = null;
    private ConnectRequest connectRequest = null;
    private int interestOps = SelectionKey.OP_READ;
    private List<ByteBuffer> dataToSend = new ArrayList<>();

    public void enableConnect() {
        interestOps = interestOps | SelectionKey.OP_CONNECT;
    }

    public void disableConnect() {
        interestOps = interestOps & ~SelectionKey.OP_CONNECT;
    }

    public void enableRead() {
        interestOps = interestOps | SelectionKey.OP_READ;
    }

    public void disableRead() {
        interestOps = interestOps & ~SelectionKey.OP_READ;
    }

    public void enableWrite() {
        interestOps = interestOps | SelectionKey.OP_WRITE;
    }

    public void disableWrite() {
        interestOps = interestOps & ~SelectionKey.OP_WRITE;
    }

    public boolean addData(ByteBuffer data) {
        dataToSend.add(data);
        return dataToSend.size() < 255;
    }

    public void notifySourceKey() {
        if (sourceKey.isValid()) {
            sourceKey.interestOps(interestOps);
        }
    }
}
