package top.sshh.bililiverecoder.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class RecordEventDTO implements Serializable {

    @JsonProperty("EventType")
    private String EventType;
    @JsonProperty("EventTimestamp")
    private Date EventTimestamp;
    @JsonProperty("EventId")
    private String EventId;
    @JsonProperty("EventData")
    private RecordEventData EventData;
}
