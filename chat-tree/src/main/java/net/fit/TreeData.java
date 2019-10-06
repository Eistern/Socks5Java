package net.fit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Message.class),
        @JsonSubTypes.Type(value = ConnectedNodes.class)
})
public interface TreeData {
}
