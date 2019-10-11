package net.fit.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
public class TreePacket implements Serializable {
    public enum PacketType {
        MESSAGE, CONNECT_NODE, DISCONNECT_NODE, UPDATE_REPLACER
    }
    private final PacketType packetType;
    private final TreeData data;
}
