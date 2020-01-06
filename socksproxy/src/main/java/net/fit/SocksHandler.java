package net.fit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class SocksHandler implements Consumer<SelectionKey> {
    private ByteBuffer byteBuffer = ByteBuffer.allocate(512);
    {
        System.out.println("Set order");
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    private final Selector selector;
    private final PairingService pairingService;


    @SneakyThrows
    private void readAuthRequest(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        byteBuffer.clear();
        int read;
        try {
            read = socketChannel.read(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (read == -1) {
            System.err.println("Connection was closed during authorization...." + socketChannel);
            socketChannel.close();
            selectionKey.cancel();
            return;
        }
        byte[] requestBytes = Arrays.copyOf(byteBuffer.array(), read);
        System.out.println("Got request: " + Arrays.toString(requestBytes));
        AuthenticationRequest authenticationRequest = AuthenticationRequest.parseFromBytes(requestBytes);
        context.setAuthenticationRequest(authenticationRequest);
        context.disableRead();
        context.enableWrite();
    }


    @SneakyThrows
    private void writeAuthResponse(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        AuthenticationRequest.AuthenticationMethod method;
        if (context.getAuthenticationRequest().getMethodsList().contains(AuthenticationRequest.AuthenticationMethod.NO_AUTH)) {
            method = AuthenticationRequest.AuthenticationMethod.NO_AUTH;
        }
        else {
            method = AuthenticationRequest.AuthenticationMethod.NO_ACCEPTABLE;
        }
        byte[] responseBytes = AuthenticationResponse.generateResponse(method);
        System.out.println("Writing response: " + Arrays.toString(responseBytes));
        byteBuffer.clear();
        byteBuffer.put(responseBytes);
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            System.err.println("Client disconnected (write auth response)");
            selectionKey.cancel();
            socketChannel.close();
            return;
        }
        context.setAuthorized(true);
        context.disableWrite();
        context.enableRead();
    }

    @SneakyThrows
    private void readConnectRequest(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        byteBuffer.clear();

        int read;
        try {
            read = socketChannel.read(byteBuffer);
        } catch (IOException e) {
            System.err.println("Client disconnected (while reading connect request)");
            selectionKey.cancel();
            socketChannel.close();
            return;
        }
        if (read == -1) {
            System.err.println("Client disconnected during connect request");
            selectionKey.cancel();
            socketChannel.close();
            return;
        }
        byte[] requestBytes = Arrays.copyOf(byteBuffer.array(), read);
        System.out.println("Got request: " + Arrays.toString(requestBytes));
        ConnectRequest connectRequest = ConnectRequest.parseFromBytes(requestBytes);
        context.setConnectRequest(connectRequest);
        context.disableRead();
        context.enableWrite();
    }


    @SneakyThrows
    private void writeConnectResponse(SelectionKey selectionKey) {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        byteBuffer.clear();
        ConnectRequest request = context.getConnectRequest();
        ConnectResponse.Status status;
        if (request.getCommand() != ConnectRequest.Command.CONNECT) {
            status = ConnectResponse.Status.COMMAND_NOT_SUPPORTED;
        }
        else if (request.getAddressType() == ConnectRequest.AddressType.IP_V6) {
            status = ConnectResponse.Status.ADDR_TYPE_NOT_SUPPORTED;
        }
        else {
            status = ConnectResponse.Status.SUCCEEDED;
        }
        byte[] responseBytes = ConnectResponse.generateResponse(status, ConnectRequest.AddressType.IP_V4, request.getAddress(), request.getPort());
        System.out.println("Writing response: " + Arrays.toString(responseBytes));
        byteBuffer.put(responseBytes);
        byteBuffer.flip();
        try {
            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            System.err.println("Client disconnected (writing connect response)");
            selectionKey.cancel();
            socketChannel.close();
            return;
        }
        context.disableWrite();

        registerServer(selectionKey, request.getAddress(), request.getPort());
    }

    @SneakyThrows
    private void registerServer(SelectionKey clientKey, InetAddress address, int port) {
        SocketChannel serverChannel = SocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.connect(new InetSocketAddress(address, port));
        SelectionKey serverKey = serverChannel.register(selector, SelectionKey.OP_CONNECT);

        ChannelContext serverContext = new ChannelContext(serverKey);
        serverContext.setAuthorized(true);
        serverContext.disableRead();
        serverContext.enableConnect();
        serverKey.attach(serverContext);
        pairingService.addPair(serverKey, clientKey);
    }

    @Override
    public void accept(SelectionKey selectionKey) {
        ChannelContext context = (ChannelContext) selectionKey.attachment();
        if (selectionKey.isValid() && selectionKey.isReadable()) {
            if (!context.isAuthorized()) {
                readAuthRequest(selectionKey);
            }
            else if (!context.isPaired()) {
                readConnectRequest(selectionKey);
            }
        }
        if (selectionKey.isValid() && selectionKey.isWritable()) {
            if (!context.isAuthorized()) {
                writeAuthResponse(selectionKey);
            }
            else if (!context.isPaired()) {
                writeConnectResponse(selectionKey);
            }
        }
    }
}
