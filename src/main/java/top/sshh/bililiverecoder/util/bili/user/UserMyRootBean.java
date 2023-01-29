/**
 * Copyright 2022 json.cn
 */
package top.sshh.bililiverecoder.util.bili.user;


import org.springframework.ui.context.Theme;

import java.util.Date;

/**
 * Auto-generated: 2022-07-22 22:35:23
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@lombok.Data
public class UserMyRootBean {

    private int code;
    private String message;
    private int ttl;
    private Data data;


    @lombok.Data
    public static class Data {

        /**
         * mid
         */
        private long mid;
        /**
         * 用户名
         */
        private String name;
        /**
         * 性别
         */
        private String sex;
        /**
         * 头像
         */
        private String face;
        /**
         * 是否为 nft 头像
         * 0不是nft头像
         * 1是 nft 头像
         */
        private int face_nft;
        private int face_nft_type;
        /**
         * 签名
         */
        private String sign;
        private int rank;
        /**
         * 当前等级	0-6级
         */
        private int level;
        private int jointime;
        /**
         * 节操	默认70
         */
        private int moral;
        /**
         * 封禁状态
         * 0：正常
         * 1：被封
         */
        private int silence;
        /**
         * 硬币数
         */
        private int coins;
        /**
         * 是否具有粉丝勋章
         * false：无
         * true：有
         */
        private boolean fans_badge;
        /**
         * 粉丝勋章信息
         */
        private Fans_medal fans_medal;
        /**
         * 认证信息
         */
        private Official official;
        /**
         * vip 信息
         */
        private Vip vip;
        /**
         * 头像框信息
         */
        private Pendant pendant;
        /**
         * 勋章信息
         */
        private Nameplate nameplate;
        private User_honour_info user_honour_info;
        /**
         * 是否关注此用户
         * true：已关注
         * false：未关注
         * 需要登录(Cookie)
         * 未登录恒为false
         */
        private boolean is_followed;
        /**
         * 主页头图链接
         */
        private String top_photo;
        private Theme theme;
        /**
         * 系统通知
         * 无内容则为空对象
         * 主要用于展示如用户争议、纪念账号等等
         */
        private Sys_notice sys_notice;
        /**
         * 直播间信息
         */
        private Live_room live_room;
        /**
         * 生日	MM-DD
         * 如设置隐私为空
         */
        private Date birthday;
        /**
         * 学校
         */
        private School school;
        private Profession profession;
        private String tags;
        private Series series;
        /**
         * 是否为硬核会员	0：否
         * 1：是
         */
        private int is_senior_member;
        private String mcn_info;


        /**
         * 已验证邮箱
         * 0：未验证
         * 1：已验证
         */
        private int email_status;
        /**
         * 已验证手机号
         * 0：未验证
         * 1：已验证
         */
        private int tel_status;
        private int identification;

        private int is_tourist;
        private int is_fake_account;
        private int pin_prompting;
        private int is_deleted;
        private int in_reg_audit;
        private boolean is_rip_user;
        private Level_exp level_exp;
        /**
         * 粉丝数
         */
        private int following;
        /**
         * 粉丝数
         */
        private int follower;
    }
}