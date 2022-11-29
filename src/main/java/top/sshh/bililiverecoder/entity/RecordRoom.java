package top.sshh.bililiverecoder.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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

    private String tags = "直播回放";

    // 发布到哪个分区
    private Integer tid = 171;


    private int copyright = 1;

    private String descTemplate = "直播录像 \n" +
            "${uname}直播间：https://live.bilibili.com/${roomId}";

    private String partTitleTemplate = "${MM月dd日HH点mm分}";

    /**
     * 上传完成是否删除文件
     */
    private boolean deleteFile;

    private String sessionId;

    private boolean recording;

    private boolean streaming;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;


}
