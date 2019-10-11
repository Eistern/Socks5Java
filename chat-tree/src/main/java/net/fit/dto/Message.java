package net.fit.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message implements Serializable, TreeData {
    private final UUID identifier;
    private final String sourceHost;
    @EqualsAndHashCode.Exclude private final String message;
    @EqualsAndHashCode.Exclude @JsonIgnore private final Date timeSent;
    public String getPrintingRep() {
        return sourceHost + ": " + message;
    }
}
