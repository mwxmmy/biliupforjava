package top.sshh.bililiverecoder.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class RecordRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    private String tags = "直播回放,${uname},${areaName}";

    // 发布到哪个分区
    private Integer tid = 171;


    private int copyright = 1;

    private String coverUrl = "live";

    /**
     * 上传线路
     */
    private String line = "CS_BLDSA";

    /**
     * 微信uid
     */
    private String wxuid;

    /**
     * 允许发送推送的tag
     */
    private String pushMsgTags = "开始直播,录制结束,分P上传,视频投稿,高级弹幕,视频评论,云剪辑";


    @Column(length = 2100)
    private String descTemplate = """
            直播录像
            ${uname}直播间：https://live.bilibili.com/${roomId}
            """;

    @Column(length = 2100)
    private String dynamicTemplate = """
            直播录像
            ${uname}直播间：https://live.bilibili.com/${roomId}
            """;

    private String partTitleTemplate = "P${index}-${areaName}-${MM月dd日HH点mm分}";

    /**
     * 上传完成是否删除文件
     * 0-不删除
     * 1-上传完成删除
     * 2-审核通过后删除
     * 3-多少天后删除
     * 4-上传结束后移动到指定目录
     * 5-审核通过后移动到指定目录
     * 6-录制结束后移动到指定目录
     * 7-录制结束后复制到指定目录
     * 8-多少天后删除移动到指定目录
     * 9-投稿成功后删除
     * 10-投稿成功后移动到指定目录
     * 11-审核通过后复制到指定目录
     */
    private int deleteType = 0;

    @Column(name = "delete_day", columnDefinition = "int default 5")
    private int deleteDay= 5;

    private String moveDir;

    private String sessionId;

    private Boolean sendDm = false;

    /**
     * 弹幕去除重复
     */
    private Boolean dmDistinct = false;

    /**
     * 弹幕用户ul等级过滤
     */
    @Column(name = "dm_ul_level", columnDefinition = "int default 0")
    private int dmUlLevel = 0;

    /**
     * 弹幕粉丝勋章过滤
     */
    @Column(name = "dm_fan_medal", columnDefinition = "int default 0")
    private int dmFanMedal = 0;

    private boolean recording;

    private boolean streaming;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


    /**
     * 弹幕关键词过滤
     */
    @Column(length = 2000)
    private String dmKeywordBlacklist = """
            天选之人
            老板大气
            红包
            关注
            傻逼
            妈
            草
            垃圾
            狗
            蛆
            脑浆
            退役
            好卡
            死
            艹
            举报
            弹幕
            画质
            傻子
            卡卡
            好卡
            不卡
            卡了
            nm
            难看
            没意思
            尼
            玛
            哈哈哈
            恶心
            屎
            sb
            xjp
            jzm
            """;

}
