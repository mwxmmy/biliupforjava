package top.sshh.bililiverecoder.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"eventId", "filePath"}))
public class RecordHistoryPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomId;
    private Long historyId;
    /**
     * 视频oid
     */
    private Long cid;

    /**
     * 直播标题
     */
    private String liveTitle;

    private String title;

    private String areaName;

    private String filePath;

    private int page;

    private float duration;

    /**
     * 投稿服务器返回的文件名
     */
    private String fileName;

    private long fileSize;

    private String eventId;

    private String sessionId;


    private boolean recording;

    private boolean upload;

    @Column(name = "file_delete", columnDefinition = "bit default 0")
    private boolean fileDelete;

    private int uploadRetryCount;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime updateTime;
}
