package top.sshh.bililiverecoder.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class RecordRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String roomId;
    private String uname;
    private Long historyId;
    //本地数据库用户id
    private Long uploadUserId;

    private boolean upload = true;

    private String title;
    private String titleTemplate = "【直播回放】【${uname}】${title} ${yyyy年MM月dd日HH点mm分}";

    private long fileSizeLimit = 0L;

    private int durationLimit = 60;

    private String tags = "直播回放";

    // 发布到哪个分区
    private Integer tid = 171;


    private int copyright = 1;

    private String coverUrl;

    /**
     * 微信uid
     */
    private String wxuid;

    /**
     * 允许发送推送的tag
     */
    private String pushMsgTags="开始直播,录制结束,分P上传,视频投稿,高级弹幕,视频评论";

    private String descTemplate = "直播录像 \n" +
            "${uname}直播间：https://live.bilibili.com/${roomId}";

    private String partTitleTemplate = "P{index}-${areaName}-${MM月dd日HH点mm分}";

    /**
     * 上传完成是否删除文件
     * 0-不删除
     * 1-上传完成删除
     * 2-发布成功删除
     */
    private int deleteType = 0;

    private String sessionId;

    private boolean recording;

    private boolean streaming;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
