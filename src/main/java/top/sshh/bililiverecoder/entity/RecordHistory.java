package top.sshh.bililiverecoder.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
public class RecordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String roomId;

    @Transient
    private String roomName;

    private String avId;

    private String bvId;

    private String title;

    private String eventId;

    private String sessionId;

    private String filePath;

    private long fileSize;

    private boolean recording;
    private boolean streaming;

    // 是否上传
    private boolean upload;

    // 是否发布成功
    private boolean publish;

    //是否已发布评论
    private boolean sendReply;

    private int code = -1;

    private int uploadRetryCount = 0;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime updateTime;


    @Transient
    private int partCount;

    @Transient
    private int recordPartCount;

    @Transient
    private int msgCount;

    @Transient
    private int successMsgCount;


    @Transient
    private LocalDateTime from;
    @Transient
    private LocalDateTime to;
}
