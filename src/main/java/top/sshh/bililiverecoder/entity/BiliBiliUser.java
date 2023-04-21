package top.sshh.bililiverecoder.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class BiliBiliUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long uid;

    private String uname;

    private String accessToken;
    private String refreshToken;

    private String cookies;

    private LocalDateTime updateTime;

    /**
     * 是否登录
     */
    private boolean login;

    /**
     * 是否启用弹幕
     */
    private boolean enable;
}
