package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

import java.util.List;

@Data
public class BiliVideoPartInfoResponse {
    private int code;
    private String message;
    private BiliVideoInfo data;


    @Data
    public static class BiliVideoInfo {
        private List<Video> videos;
    }

    @Data
    public static class Video {
        private long aid;
        private String bvid;
        private String title;
        private String filename;
        private long cid;
        private long ctime;
        private int failCode;
        private int xcodeState;
        private String failDesc;
        private int page;
        /**
         * 标题
         */
        private String part;
        private int duration;
    }
}
