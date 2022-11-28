package top.sshh.bililiverecoder.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
public class RecordHistoryPart {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String roomId;
    private Long historyId;
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

    private String eventId;

    private String sessionId;


    private boolean recording;

    private boolean upload;

    private int uploadRetryCount;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime updateTime;
}
