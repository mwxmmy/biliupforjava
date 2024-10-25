package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

@Data
public class DescV2Dto {
    private long biz_id;
    private String raw_text;
    private int type = 1;
}
