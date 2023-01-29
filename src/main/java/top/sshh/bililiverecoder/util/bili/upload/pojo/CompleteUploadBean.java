package top.sshh.bililiverecoder.util.bili.upload.pojo;

import lombok.Data;

/**
 * @author mwxmmy
 */
@Data
public class CompleteUploadBean {


    private Integer OK;

    private Integer code;

    private String message;

    private Integer ttl;

    private String location;

    private String key;

    private String bucket;

    private String hash;

    private Long eTag;
}
