package net.fit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor(force = true)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
public class ChatNode implements Serializable, TreeData {
    private final InetSocketAddress address;

    @EqualsAndHashCode.Exclude @JsonIgnore private ChatNode replacer;
    @EqualsAndHashCode.Exclude @JsonIgnore private List<Message> unconfirmedMessages = new ArrayList<>();
    @EqualsAndHashCode.Exclude @JsonIgnore private int trust = 0;

    public void addUnconfirmedMessage(Message message) {
        unconfirmedMessages.add(message);
    }
    public boolean messageSent(Message message) {
        if (unconfirmedMessages.contains(message)) {
            unconfirmedMessages.remove(message);
            pardon();
        }
        else
            return false;
        return true;
    }
    public void pardon() {
        trust = 0;
    }
    public void incrementTrust() {
        trust++;
    }
}
