package top.sshh.bililiverecoder.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
@Entity
public class BiliBiliUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long uid;

    private String uname;

    private String accessToken;
    private String refreshToken;

    private LocalDateTime updateTime;

    /**
     * 是否登录
     */
    private boolean login;

    private boolean enable;
}
