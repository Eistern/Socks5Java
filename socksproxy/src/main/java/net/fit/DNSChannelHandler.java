package net.fit;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class DNSChannelHandler implements Consumer<SelectionKey> {
    private final SelectionKey dnsKey;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(8196);
    private Map<String, SelectionKey> keysRequests = new HashMap<>();
    private List<Message> pendingRequests = new ArrayList<>();

    public void addMessage(Message nextMessage) {
        pendingRequests.add(nextMessage);
        int interest = dnsKey.interestOps();
        dnsKey.interestOps(interest | SelectionKey.OP_WRITE);
    }

    @SneakyThrows
    @Override
    public void accept(SelectionKey selectionKey) {
        if (!dnsKey.equals(selectionKey)) {
            System.err.println("Accepted wrong channel (DNS)");
            return;
        }
        DatagramChannel dnsChannel = (DatagramChannel) dnsKey.channel();
        if (selectionKey.isReadable()) {
            byteBuffer.clear();
            dnsChannel.read(byteBuffer);
            Message receivedMessage = new Message(byteBuffer);
            Record[] answers = receivedMessage.getSectionArray(Section.ANSWER);
            System.out.println(Arrays.toString(answers));
        }
        if (selectionKey.isWritable()) {
            byteBuffer.clear();
            Message nextMessage = pendingRequests.get(0);
            byte[] message = nextMessage.toWire();
            byteBuffer.put(message);
            dnsChannel.write(byteBuffer);
            int interest = selectionKey.interestOps();
            selectionKey.interestOps(interest | SelectionKey.OP_WRITE);
        }
    }
}
