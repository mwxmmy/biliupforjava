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

    private String bvId;

    private String title;

    private String eventId;

    private String sessionId;

    private String filePath;

    private long fileSize;

    private boolean recording;
    private boolean streaming;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private LocalDateTime updateTime;
}
