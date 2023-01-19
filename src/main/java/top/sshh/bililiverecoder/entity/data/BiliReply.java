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
     * 根评论rpid
     * 二级评论以上使用
     */
    String root;

    /**
     * 父评论rpid
     * 二级评论同根评论id
     * 大于二级评论为要回复的评论id
     */
    String parent;
    /**
     * 操作代码
     * 默认为0
     * 0：取消置顶
     * 1：设为置顶
     */
    String action;
}
