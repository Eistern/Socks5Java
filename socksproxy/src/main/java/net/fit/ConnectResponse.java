package net.fit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ConnectResponse {
    @RequiredArgsConstructor
    public enum Status {
        SUCCEEDED((byte) 0x00),
        SERVER_FAILURE((byte) 0x01),
        NOT_ALLOWED((byte) 0x02),
        NETWORK_UNREACHABLE((byte) 0x03),
        HOST_UNREACHABLE((byte) 0x04),
        CONNECTION_REFUSED((byte) 0x05),
        TTL_EXPIRED((byte) 0x06),
        COMMAND_NOT_SUPPORTED((byte) 0x07),
        ADDR_TYPE_NOT_SUPPORTED((byte) 0x08);

        @Getter
        private final byte code;
        private static Status valueOfCode(byte code) {
            for ( Status status : values()) {
                if (status.code == code)
                    return status;
            }
            return null;
        }
    }

    public static byte[] generateResponse(Status status, ConnectRequest.AddressType addressType, InetAddress address, short port) {
        ByteBuffer portConvert = ByteBuffer.allocate(2);
        portConvert.order(ByteOrder.BIG_ENDIAN);
        portConvert.putShort(port);
        byte[] portCode = portConvert.array();
        byte[] addrCode = address.getAddress();

        int responseLength = 6 + addrCode.length;
        if (addressType == ConnectRequest.AddressType.DOMAIN_NAME)
            responseLength++;
        ByteBuffer byteBuffer = ByteBuffer.allocate(responseLength);
        byteBuffer.put((byte) 0x05);
        byteBuffer.put(status.getCode());
        byteBuffer.put((byte) 0x00);
        byteBuffer.put(addressType.getCode());
        if (addressType == ConnectRequest.AddressType.DOMAIN_NAME)
            byteBuffer.put((byte) addrCode.length);
        byteBuffer.put(addrCode);
        byteBuffer.put(portCode);
        return byteBuffer.array();
    }
}
