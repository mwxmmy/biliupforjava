package top.sshh.bililiverecoder.util.bili.upload.pojo;

import lombok.Data;

@Data
public class LineUploadBean {


    private int OK;

    private String upload_id;

    private String key;

    private String bucket;

    public String getFileName(){
        return key.substring(1,key.indexOf('.'));
    }
}
