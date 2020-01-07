package net.fit.handshake;

import lombok.*;
import net.fit.handlers.DNSChannelHandler;
import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.util.Arrays;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConnectRequest {
    @RequiredArgsConstructor
    public enum Command {
        CONNECT((byte) 0x01),
        BIND((byte) 0x02),
        UDP_ASSOCIATE((byte) 0x03);

        @Getter private final byte code;
        private static Command valueOfCode(byte code) {
            for (Command command : values()) {
                if (command.code == code)
                    return command;
            }
            return null;
        }
    }

    @RequiredArgsConstructor
    public enum AddressType {
        IP_V4((byte) 0x01),
        DOMAIN_NAME((byte) 0x03),
        IP_V6((byte) 0x04);

        @Getter private final byte code;
        private static AddressType valueOfCode(byte code) {
            for (AddressType type : values()) {
                if (type.code == code)
                    return type;
            }
            return null;
        }
    }

    private static final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
    private final Command command;
    private final AddressType addressType;
    private InetAddress address;
    private String hostName;
    private final short port;

    public static ConnectRequest parseFromBytes(byte[] input) throws UnknownHostException {
        if (input.length < 6)
            return null;
        if (input[0] != 0x05 || input[2] != 0x00)
            return null;

        Command requestCommand = Command.valueOfCode(input[1]);
        if (requestCommand == null)
            return null;

        AddressType addressType = AddressType.valueOfCode(input[3]);
        if (addressType == null)
            return null;

        byteBuffer.clear();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        int addrLen = 0, addrOffset = 4;
        switch (addressType) {
            case DOMAIN_NAME:
                addrLen = input[4];
                addrOffset = 5;
                break;
            case IP_V4:
                addrLen = 4;
                break;
            case IP_V6:
                addrLen = 16;
                break;
            default:
                break;
        }
        byte[] address = Arrays.copyOfRange(input, addrOffset, addrOffset + addrLen);
        InetAddress resultAddress = null;
        String hostname = null;
        if (addressType != AddressType.DOMAIN_NAME) {
             resultAddress = InetAddress.getByAddress(address);
        }
        else {
            System.out.println("--------------------------------ACTUALLY USING DNS-------------------------------------------");
            hostname = new String(address);
        }

        byte[] portBytes = Arrays.copyOfRange(input, input.length - 2, input.length);
        byteBuffer.put(portBytes);
        byteBuffer.rewind();
        short port = byteBuffer.getShort();

        return new ConnectRequest(requestCommand, addressType, resultAddress, hostname, port);
    }
}
