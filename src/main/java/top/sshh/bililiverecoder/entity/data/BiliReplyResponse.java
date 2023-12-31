package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

@Data
public class BiliReplyResponse {
    private int code;
    private String message;
    private BiliReply data;

    @Data
    public static class Reply {
        private String rpid;
    }
}
