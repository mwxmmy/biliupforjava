package top.sshh.bililiverecoder.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class BlrecRoomInfo {
    private String uid;
    @JsonProperty("room_id")
    private String roomId;
    @JsonProperty("short_room_id")
    private long sortRoomId;
    @JsonProperty("area_id")
    private Integer areaId;
    @JsonProperty("parent_area_id")
    private Integer parentAreaId;

    @JsonProperty("area_name")
    private String areaName;
    @JsonProperty("parent_area_name")
    private String parentAreaName;
    @JsonProperty("live_status")
    private int liveStatus;
    @JsonProperty("live_start_time")
    private Date liveStartTime;
    private int online;
    private String title;
    private String cover;
    private String tags;
    private String description;
}
