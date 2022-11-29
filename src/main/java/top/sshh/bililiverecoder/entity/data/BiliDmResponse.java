package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

@Data
public class BiliDmResponse {
    private int code;
    private String message;
    private BiliDm data;

    @Data
    public class BiliDm {
        private String dmid;
        private String dmid_str;
    }
}
