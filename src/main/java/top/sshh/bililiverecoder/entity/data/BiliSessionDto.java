package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

@Data
public class BiliSessionDto {
    private String accessToken;
    private String refreshToken;
    private Long mid;
    private long createTime;

}
