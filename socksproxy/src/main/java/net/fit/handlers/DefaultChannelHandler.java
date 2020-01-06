package net.fit.handlers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.fit.ChannelContext;
import net.fit.PairingService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class DefaultChannelHandler implements Consumer<SelectionKey> {
    private final PairingService pairingService;
    private final SocksHandler socksHandler;

    @SneakyThrows
    private void readFromChannel(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        ByteBuffer byteBuffer = ByteBuffer.allocate(8196);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int read;
        try {
            read = socketChannel.read(byteBuffer);
        }
        catch (IOException e) {
            System.err.println("Channel closed by force in regular read....");
            socketChannel.close();
            context.getDataToSend().clear();
            context.setWantClose(true);
            context.getPairedContext().addData(ByteBuffer.allocate(1));
            context.getPairedContext().enableWrite();
            context.getPairedContext().notifySourceKey();
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
            context.getPairedContext().addData(ByteBuffer.allocate(1));
            context.disableRead();
        } else {
            System.out.println("Read data:\n" + new String(byteBuffer.array(), 0, byteBuffer.position()));
            boolean hasMoreSpace = context.getPairedContext().addData(byteBuffer);
            if (!hasMoreSpace) {
                context.disableRead();
            }
        }
        context.getPairedContext().enableWrite();
        context.getPairedContext().notifySourceKey();
    }


    @SneakyThrows
    private void writeToChannel(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        ByteBuffer byteBuffer = context.getDataToSend().get(0);
        context.getDataToSend().remove(0);
        System.out.println("Sending data:\n" + new String(byteBuffer.array(), 0, byteBuffer.position()));
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
            byteBuffer.compact();
            byteBuffer.clear();
            if (!context.getPairedContext().isWantClose()) {
                context.getPairedContext().enableRead();
                context.getPairedContext().notifySourceKey();
            }
            if (context.getDataToSend().size() == 0)
                context.disableWrite();
            if (context.getDataToSend().size() == 0 && context.getPairedContext().isWantClose())
                socketChannel.shutdownOutput();

        } catch (IOException e) {
            System.err.println("Channel closed by force in regular write....");
            socketChannel.close();
            context.getDataToSend().clear();
            context.setWantClose(true);
            context.getPairedContext().addData(ByteBuffer.allocate(1));
            context.getPairedContext().enableWrite();
            context.getPairedContext().notifySourceKey();
            selectionKey.cancel();
        }
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
