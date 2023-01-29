package top.sshh.bililiverecoder.util.bili.upload.pojo;

import lombok.Data;

/**
 * @author mwxmmy
 */
@Data
public class PublishVideoBean {


    private int code;
    private String message;
    private int ttl;
    private Data data;


    @lombok.Data
    public static class Data {
        private String aid;
        private String bvid;
    }
}
