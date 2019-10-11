package net.fit.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import net.fit.nodes.ConnectedNodes;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Message.class),
        @JsonSubTypes.Type(value = ConnectedNodes.class)
})
interface TreeData {
}
