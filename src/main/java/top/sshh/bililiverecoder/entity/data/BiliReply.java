package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

@Data
public class BiliReply {
    String type;
    String oid;
    String message;
    String rpid;
    String action;
}
