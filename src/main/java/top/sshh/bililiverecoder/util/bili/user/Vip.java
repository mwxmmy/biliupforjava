/**
 * Copyright 2022 json.cn
 */
package top.sshh.bililiverecoder.util.bili.user;

import lombok.Data;

/**
 * Auto-generated: 2022-07-22 9:39:59
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class Vip {

    /**
     * 会员类型
     * 0：无
     * 1：月大会员
     * 2：年度及以上大会员
     */
    private int type;
    /**
     * 评论区用户会员类型
     * 0：无
     * 1：月大会员
     * 2：年度及以上大会员
     */
    private int vipType;
    /**
     * 会员状态
     * 0：无
     * 1：有
     */
    private int status;
    /**
     * 评论区用户会员状态
     * 0：无
     * 1：有
     */
    private int vipStatus;
    /**
     * 会员过期时间	Unix时间戳(毫秒)
     */
    private long due_date;
    private int vip_pay_type;
    private int theme_type;
    /**
     * 会员标签
     */
    private Label label;
    /**
     * 是否显示会员图标
     * 0：不显示
     * 1：显示
     */
    private int avatar_subscript;
    /**
     * 会员昵称颜色	颜色码
     */
    private String nickname_color;
    private int role;
    /**
     * 大会员角标地址
     */
    private String avatar_subscript_url;
    private int tv_vip_status;
    private int tv_vip_pay_type;


}