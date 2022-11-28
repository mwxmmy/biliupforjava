package top.sshh.bililiverecoder.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@Entity
public class RecordHistoryPart {

    @Id
    private String id;

    private String roomId;
    private String historyId;
    /**
     * 视频oid
     */
    private Long oid;

    private String title;

    private String filePath;

    /**
     * 投稿服务器返回的文件名
     */
    private String fileName;

    private long fileSize;

    private String sessionId;


    private boolean recording;

    // 1-正在上传，2-上传成功
    private int uploadStatus;

    private int uploadRetryCount;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime updateTime;
}
