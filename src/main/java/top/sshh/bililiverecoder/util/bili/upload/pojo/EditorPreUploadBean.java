package top.sshh.bililiverecoder.util.bili.upload.pojo;

import lombok.Data;

/**
 * @author mwxmmy
 */
@Data
public class EditorPreUploadBean {


    private Integer code;

    private Data data;

    private String message;

    private Integer ttl;

    @lombok.Data
    public static class Data {
        private String boss_path;
        private Long ctime;
        private String file_hash;
        private Long per_size;
        private Long mid;
        private Long mtime;
        private String resource_id;
        private Long size;
        private Integer state;
        private String title;
        private Long type;
        private String upload_id;
        private String[] upload_urls;
        private Integer video_height;
        private Integer video_width;
    }
}
