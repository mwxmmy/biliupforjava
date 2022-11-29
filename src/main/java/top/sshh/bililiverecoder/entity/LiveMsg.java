package top.sshh.bililiverecoder.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@Entity
public class LiveMsg {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String partId;
    private String bvid;
    private Long cid;

    private String username;

    private String context;

    // 1 未发送，2已发送
    private boolean isSend;

    private int code;

    /**
     * 发送时间戳
     */
    private Long sendTime;

}
