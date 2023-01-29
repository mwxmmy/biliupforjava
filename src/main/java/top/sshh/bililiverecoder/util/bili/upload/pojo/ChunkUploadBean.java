package top.sshh.bililiverecoder.util.bili.upload.pojo;

import lombok.Data;

@Data
public class ChunkUploadBean {


    private int OK;

    private String upload_id;

    private String key;

    private String bucket;

    private String ctx;

    private String checksum;
}
