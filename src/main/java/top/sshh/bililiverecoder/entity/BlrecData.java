package top.sshh.bililiverecoder.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BlrecData {
    @JsonProperty("room_id")
    private String roomId;
    private String path;
    @JsonProperty("user_info")
    private BlrecUserInfo userInfo;
    @JsonProperty("room_info")
    private BlrecRoomInfo roomInfo;
}