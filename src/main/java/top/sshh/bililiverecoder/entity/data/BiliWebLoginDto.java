package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

@Data
public class BiliWebLoginDto {
    private Integer code;
    private String message;
    private Data data;
    private String cookie;

    @lombok.Data
    public static class Data {
        private String url;
        private String refresh_token;
        private long timestamp;
        //0：扫码登录成功
        // 86038：二维码已失效
        // 86090：二维码已扫码未确认
        // 86101：未扫码
        private Integer code;
        private String message;
    }
}
