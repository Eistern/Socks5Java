package net.fit.handlers;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.fit.ChannelContext;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class DNSChannelHandler implements Consumer<SelectionKey> {
    private final SelectionKey dnsKey;
    private ByteBuffer byteBuffer = ByteBuffer.allocate(8196);
    private Map<Record, SelectionKey> keysRequests = new HashMap<>();
    private List<Message> pendingRequests = new ArrayList<>();

    @SneakyThrows
    void addMessage(String hostName, SelectionKey from) {
        String resultHostname;
        if (!hostName.endsWith(".")) {
            resultHostname = hostName + ".";
        } else {
            resultHostname = hostName;
        }

        Name host = Name.fromString(resultHostname);
        Record question = Record.newRecord(host, Type.A, DClass.IN);
        Message nextMessage = Message.newQuery(question);
        pendingRequests.add(nextMessage);
        keysRequests.put(question, from);
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
            Message receivedMessage = new Message(byteBuffer.array());
            Record receivedQuestion = receivedMessage.getQuestion();
            System.out.println("Received DNS response:\n" + receivedMessage);
            SelectionKey questionSource = keysRequests.get(receivedQuestion);
            if (questionSource == null) {
                System.out.println("Didn't find question:\n" + receivedQuestion + "\nIn:\n" + keysRequests);
                return;
            }
            keysRequests.remove(receivedQuestion);
            Record[] answers = receivedMessage.getSectionArray(Section.ANSWER);
            InetAddress address = null;
            for (Record answer : answers) {
                if (address == null && (answer instanceof ARecord || answer instanceof A6Record)) {
                    if (answer instanceof ARecord) {
                        ARecord aRecord = (ARecord) answer;
                        address = aRecord.getAddress();
                    } else {
                        A6Record a6Record = (A6Record) answer;
                        address = a6Record.getSuffix();
                    }
                }
            }
            if (address == null) {
                System.err.println("Got response without needed answer entry, closing channel....");
                questionSource.channel().close();
                questionSource.cancel();
            }
            else {
                ChannelContext context = (ChannelContext) questionSource.attachment();
                context.getConnectRequest().setAddress(address);
                context.enableWrite();
                context.notifySourceKey();
            }

            if (keysRequests.isEmpty()) {
                int interest = selectionKey.interestOps();
                selectionKey.interestOps(interest & ~SelectionKey.OP_READ);
            }
        }
        if (selectionKey.isWritable()) {
            byteBuffer.clear();
            Message nextMessage = pendingRequests.remove(0);
            byte[] message = nextMessage.toWire();
            System.out.println("Sending DNS request:\n" + nextMessage);
            byteBuffer.put(message);
            byteBuffer.flip();
            dnsChannel.write(byteBuffer);
            int interest = selectionKey.interestOps();
            selectionKey.interestOps(interest | SelectionKey.OP_READ);
            if (pendingRequests.isEmpty()) {
                interest = selectionKey.interestOps();
                selectionKey.interestOps(interest & ~SelectionKey.OP_WRITE);
            }
        }
    }
}
