package top.sshh.bililiverecoder.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecordHistoryDTO {

    private Long id;

    private String roomId;

    private String roomName;

    private String bvId;

    private String title;

    private String eventId;

    private String sessionId;

    private String filePath;

    private long fileSize;

    private Boolean recording;
    private Boolean streaming;

    // 是否上传
    private Boolean upload;

    // 是否发布成功
    private Boolean publish;

    private int code = -1;

    private int uploadRetryCount = 0;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime updateTime;


    private int partCount;

    private int recordPartCount;

    private int msgCount;

    private int successMsgCount;


    private LocalDateTime from;

    private LocalDateTime to;

    private int current = 1;
    private int pageSize = 5;
    private int total;
}
