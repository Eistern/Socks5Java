package net.fit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class DefaultChannelHandler implements Consumer<SelectionKey> {
    private final PairingService pairingService;
    private final SocksHandler socksHandler;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(8196);

    @SneakyThrows
    private void readFromChannel(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        byteBuffer.clear();
        int read;
        try {
            read = socketChannel.read(byteBuffer);
        }
        catch (IOException e) {
            System.err.println("Channel closed by force in regular read....");
            socketChannel.close();
            selectionKey.cancel();
            return;
        }
        if (read == -1) {
            socketChannel.shutdownInput();
            context.setWantClose(true);
            if (context.getPairedContext() == null) {
                socketChannel.close();
                selectionKey.cancel();
                return;
            }
            context.getPairedContext().addData(new byte[]{});
            context.disableRead();
        } else {
            byte[] data = Arrays.copyOf(byteBuffer.array(), read);
            System.out.println("Read data: " + new String(data));
            boolean hasMoreSpace = context.getPairedContext().addData(data);
            if (!hasMoreSpace) {
                context.disableRead();
                System.out.println("---------No more space, shutting down read....-------");
            }
        }
        context.getPairedContext().enableWrite();
        context.getPairedContext().notifySourceKey();
    }


    @SneakyThrows
    private void writeToChannel(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        byteBuffer.clear();
        byte[] nextBlock = context.getDataToSend().get(0);
        context.getDataToSend().remove(0);
        System.out.println("Sending data: " + new String(nextBlock));
        byteBuffer.put(nextBlock);
        byteBuffer.flip();
        byteBuffer.rewind();
        try {
            socketChannel.write(byteBuffer);
            byteBuffer.compact();
        } catch (IOException e) {
            System.err.println("Channel closed by force in regular write....");
            socketChannel.close();
            selectionKey.cancel();
            return;
        }

        if (!context.getPairedContext().isWantClose()) {
            context.getPairedContext().enableRead();
            context.getPairedContext().notifySourceKey();
        }
        if (context.getDataToSend().size() == 0)
            context.disableWrite();
        if (context.getDataToSend().size() == 0 && context.getPairedContext().isWantClose())
            socketChannel.shutdownOutput();
    }

    @SneakyThrows
    private void connectChannel(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        boolean success = socketChannel.finishConnect();
        if (success)
            pairingService.finishPairing(selectionKey);
        else
            pairingService.abortPairing(selectionKey);
        context.disableConnect();
        context.enableRead();
    }

    @SneakyThrows
    @Override
    public void accept(SelectionKey selectionKey) {
        ChannelContext context = (ChannelContext) selectionKey.attachment();

        if (selectionKey.isValid() && selectionKey.isConnectable()) {
            connectChannel(selectionKey);
        }
        if (selectionKey.isValid() && selectionKey.isWritable()) {
            if (context.isPaired()) {
                writeToChannel(selectionKey);
            }
            else {
                socksHandler.accept(selectionKey);
            }
        }
        if (selectionKey.isValid() && selectionKey.isReadable()) {
            if (context.isPaired()) {
                readFromChannel(selectionKey);
            } else {
                socksHandler.accept(selectionKey);
            }
        }

        if (selectionKey.isValid()) {
            //noinspection MagicConstant
            selectionKey.interestOps(context.getInterestOps());
            if (context.isPaired()) {
                if (context.getDataToSend().size() == 0 && context.getPairedContext().isWantClose() && context.isWantClose()) {
                    System.out.println("Cancelling key: " + selectionKey);
                    selectionKey.cancel();
                    selectionKey.channel().close();
                }
            }
        }
    }
}
