package top.sshh.bililiverecoder.util.bili.upload.pojo;

import lombok.Data;

@Data
public class EditorSpaceBean {


    private Integer code;

    private Data data;

    private String message;

    private Integer ttl;

    @lombok.Data
    public static class Data {
        private Long used;
        private Long total;
    }
}
