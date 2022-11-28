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
    private String historyId;
    //本地数据库用户id
    private Long uploadUserId;

    private boolean upload;

    private String title;

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
