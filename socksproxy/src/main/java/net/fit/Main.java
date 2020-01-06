package net.fit;

import lombok.SneakyThrows;
import net.fit.handlers.DNSChannelHandler;
import net.fit.handlers.DefaultChannelHandler;
import net.fit.handlers.ListeningChannelHandler;
import net.fit.handlers.SocksHandler;

import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
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
        SelectionKey listeningKey = listeningChannel.register(selector, SelectionKey.OP_ACCEPT);
        //REGISTERED ServerSocketChannel

        DatagramChannel dnsChannel = DatagramChannel.open();
        dnsChannel.connect(new InetSocketAddress(System.getProperty("dns.server"), 53));
        dnsChannel.configureBlocking(false);
        SelectionKey dnsKey = dnsChannel.register(selector, 0);
        //REGISTERED DnsChannel

        ListeningChannelHandler listeningChannelHandler = new ListeningChannelHandler(selector);
        DNSChannelHandler dnsChannelHandler = new DNSChannelHandler(dnsKey);
        PairingService pairingService = new PairingService();
        SocksHandler socksHandler = new SocksHandler(selector, pairingService);
        DefaultChannelHandler defaultChannelHandler = new DefaultChannelHandler(pairingService, socksHandler);

        while (true) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (Iterator<SelectionKey> iterator = selectionKeys.iterator(); iterator.hasNext(); ) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();

                if (selectionKey.equals(listeningKey) && selectionKey.isAcceptable()) {
                    listeningChannelHandler.accept(selectionKey);
                } else if (selectionKey.equals(dnsKey)) {
                    dnsChannelHandler.accept(selectionKey);
                } else {
                    defaultChannelHandler.accept(selectionKey);
                }
            }
        }
    }
}
