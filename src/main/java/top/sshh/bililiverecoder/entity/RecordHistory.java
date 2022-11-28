package top.sshh.bililiverecoder.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@Entity
public class RecordHistory {

    @Id
    private String id;

    private String roomId;

    private String bvId;

    private String title;

    private String sessionId;

    private String filePath;

    private long fileSize;

    private boolean recording;
    private boolean streaming;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime updateTime;
}
