package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VideoEditUploadDto {
    private Integer build = 1088;
    private Integer copyright = 2;
    private String cover = "";
    private String desc = "";
    private List<DescV2Dto> desc_v2 = null;
    private Integer no_reprint = 0;
    private Integer recreate = -1;
    private Integer act_reserve_create = 0;
    private Integer open_elec = 1;
    private Integer topic_grey = 1;
    private Integer web_os = 1;
    private boolean handle_staff = false;
    private Integer interactive = 0;
    private Integer is_360 = -1;
    private String source = "";
    private String tag = "";
    private int desc_format_id = 0;
    private long aid;
    private Integer tid = 27;
    private String title = "";
    private String dynamic = "";
    private List<DescV2Dto> dynamic_v2 = null;
    private String csrf;
    private List<SingleVideoDto> videos = new ArrayList<>();
}
