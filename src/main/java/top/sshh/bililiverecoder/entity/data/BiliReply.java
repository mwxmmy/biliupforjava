package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

/**
 * @author mwxmmy
 */
@Data
public class BiliReply {
    /**
     * 评论区类型代码
     */
    String type;
    /**
     * 目标评论区id
     */
    String oid;
    /**
     * 发送评论内容
     */
    String message;
    /**
     * 评论rpid
     */
    String rpid;
    /**
     * 操作代码
     * 默认为0
     * 0：取消置顶
     * 1：设为置顶
     */
    String action;
}
