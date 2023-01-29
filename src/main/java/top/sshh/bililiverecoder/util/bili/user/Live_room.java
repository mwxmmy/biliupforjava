/**
 * Copyright 2022 json.cn
 */
package top.sshh.bililiverecoder.util.bili.user;

import lombok.Data;

/**
 * Auto-generated: 2022-07-22 9:39:59
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class Live_room {

    /**
     * 直播间状态
     * 0：无房间
     * 1：有房间
     */
    private int roomStatus;
    /**
     * 直播状态
     * 0：未开播
     * 1：直播中
     */
    @Deprecated
    private int liveStatus;
    /**
     * 直播状态
     * 0：未开播
     * 1：直播中
     */
    private int live_status;


    /**
     * 直播间网页 url
     */
    private String url;
    /**
     * 直播间标题
     */
    private String title;
    /**
     * 直播间封面 url
     */
    private String cover;
    /**
     * 直播间 id(短号)
     */
    private long roomid;
    /**
     * 轮播状态
     * 0：未轮播
     * 1：轮播
     */
    private int roundStatus;
    private int broadcast_type;
    private Watched_show watched_show;
}