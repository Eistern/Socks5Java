package net.fit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor
@AllArgsConstructor
class ChatNode implements Serializable, TreeData {
    private final InetSocketAddress address;

    @EqualsAndHashCode.Exclude @JsonIgnore private ChatNode replacer;
    @EqualsAndHashCode.Exclude @JsonIgnore private List<Message> unconfirmedMessages = new ArrayList<>();

    public void addMessage(Message message) {
        unconfirmedMessages.add(message);
    }
    public void messageSent(Message message) {
        unconfirmedMessages.remove(message);
    }
}
