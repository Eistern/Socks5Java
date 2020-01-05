package net.fit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ListeningChannelHandler implements Consumer<SelectionKey> {
    private final Selector selector;

    @SneakyThrows
    @Override
    public void accept(SelectionKey selectionKey) {
        if (!selectionKey.isAcceptable())
            return;
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (socketChannel == null) {
            System.err.println("Failed to accept");
            return;
        }
        socketChannel.configureBlocking(false);
        SelectionKey nextKey = socketChannel.register(selector, SelectionKey.OP_READ);
        ChannelContext context = new ChannelContext(nextKey);
        nextKey.attach(context);
    }
}
