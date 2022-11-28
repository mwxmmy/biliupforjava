package top.sshh.bililiverecoder.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public
class RecordEventData implements Serializable {
    @JsonProperty("RoomId")
    private String RoomId;
    @JsonProperty("ShortId")
    private long ShortId;
    @JsonProperty("Name")
    private String Name;
    @JsonProperty("Title")
    private String Title;
    @JsonProperty("RelativePath")
    private String RelativePath;
    @JsonProperty("FileSize")
    private long FileSize;
    @JsonProperty("Duration")
    private float Duration;
    @JsonProperty("FileOpenTime")
    private Date FileOpenTime;
    @JsonProperty("FileCloseTime")
    private Date FileCloseTime;
    @JsonProperty("SessionId")
    private String SessionId;
    @JsonProperty("AreaNameParent")
    private String AreaNameParent;
    @JsonProperty("AreaNameChild")
    private String AreaNameChild;
    @JsonProperty("Recording")
    private boolean Recording;
    @JsonProperty("Streaming")
    private boolean Streaming;
    @JsonProperty("DanmakuConnected")
    private boolean DanmakuConnected;

}
