package net.fit;

import lombok.SneakyThrows;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Set;

public class Main {
    @SneakyThrows
    public static void main(String[] args) {
        if (args.length < 1)
            return;
        int port = Integer.parseInt(args[0]);
        //GOT PORT FROM PARAMETERS

        SystemProperties.load("application.properties");
        //ADDED SYSTEM PROPERTIES FOR DNSJAVA

        Selector selector = Selector.open();
        //CREATED SELECTOR

        ServerSocketChannel listeningChannel = ServerSocketChannel.open();
        listeningChannel.bind(new InetSocketAddress(port));
        listeningChannel.configureBlocking(false);
        SelectionKey key = listeningChannel.register(selector, SelectionKey.OP_ACCEPT);
        //REGISTERED ServerSocketChannel

        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();

        }
    }
}
