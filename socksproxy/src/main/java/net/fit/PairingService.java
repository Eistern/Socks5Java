package net.fit;

import lombok.SneakyThrows;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map;

public class PairingService {
    private Map<SelectionKey, SelectionKey> keyPairs = new HashMap<>();

    public void addPair(SelectionKey key, SelectionKey value) {
        keyPairs.put(key, value);
    }

    @SneakyThrows
    public void abortPairing(SelectionKey key) {
        SelectionKey value = keyPairs.get(key);
        if (value == null) {
            System.err.println("Pairing service got invalid key: " + key);
            return;
        }
        key.channel().close();
        key.cancel();

        value.channel().close();
        value.cancel();
    }

    public void finishPairing(SelectionKey key) {
        SelectionKey value = keyPairs.get(key);
        if (value == null) {
            System.err.println("Pairing service got invalid key: " + key);
            return;
        }
        key.interestOps(SelectionKey.OP_READ);
        value.interestOps(SelectionKey.OP_READ);

        ChannelContext keyContext = (ChannelContext) key.attachment();
        ChannelContext valueContext = (ChannelContext) value.attachment();
        keyContext.setPairedContext(valueContext);
        keyContext.setPaired(true);
        valueContext.setPairedContext(keyContext);
        valueContext.setPaired(true);
    }
}
