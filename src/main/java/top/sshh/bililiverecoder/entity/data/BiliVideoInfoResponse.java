package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

import java.util.List;

@Data
public class BiliVideoInfoResponse {
    private int code;
    private String message;
    private BiliVideoInfo data;


    @Data
    public static class BiliVideoInfo {
        private String bvid;
        private String aid;
        private int videos;
        private int tid;
        private String pic;
        private int state;
        private int duration;
        private List<BiliVideoInfoPart> pages;
    }

    @Data
    public static class BiliVideoInfoPart {
        private long cid;
        private int page;
        /**
         * 标题
         */
        private String part;
        private int duration;
    }
}
