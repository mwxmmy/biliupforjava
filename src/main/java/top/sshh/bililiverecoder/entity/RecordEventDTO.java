package top.sshh.bililiverecoder.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
public class RecordEventDTO implements Serializable {

    // b站录播姬
    @JsonProperty("EventType")
    private String EventType;
    @JsonProperty("EventTimestamp")
    private Date EventTimestamp;
    @JsonProperty("EventId")
    private String EventId;
    @JsonProperty("EventData")
    private RecordEventData EventData;


    // blrec
    private String id;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;
    private String type;
    private BlrecData data;

}
