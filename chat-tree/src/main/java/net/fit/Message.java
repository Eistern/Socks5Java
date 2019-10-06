package net.fit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
class Message {
    private final UUID identifier;
    private final String message;
}
