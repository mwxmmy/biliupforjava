package top.sshh.bililiverecoder.entity.data;

import lombok.Data;

import java.util.List;


@Data
public class BiliUserCard {
    /**
     * 用户ID
     */
    private long mid;

    /**
     * 用户名
     */
    private String name;

    /**
     * 是否通过审核
     */
    private boolean approve;

    /**
     * 性别
     */
    private String sex;

    /**
     * 排名
     */
    private int rank;

    /**
     * 头像链接
     */
    private String face;

    /**
     * 硬币数量
     */
    private int coins;

    /**
     * 显示排名
     */
    private String DisplayRank;

    /**
     * 注册时间戳
     */
    private long regtime;

    /**
     * 用户空间状态
     */
    private int spacesta;

    /**
     * 地址
     */
    private String place;

    /**
     * 生日
     */
    private String birthday;

    /**
     * 个人签名
     */
    private String sign;

    /**
     * 个人描述
     */
    private String description;

    /**
     * 文章数量
     */
    private int article;

    /**
     * 粉丝数量
     */
    private int fans;

    /**
     * 好友数量
     */
    private int friend;

    /**
     * 关注数量
     */
    private int attention;

    /**
     * 关注列表
     */
    private List<Long> attentions;

    /**
     * 等级信息
     */
    private LevelInfo level_info;

    @Data
    public static class LevelInfo {
        /**
         * 下一等级所需经验值
         */
        private int next_exp;

        /**
         * 当前等级
         */
        private int current_level;

        /**
         * 当前等级最小经验值
         */
        private int current_min;

        /**
         * 当前经验值
         */
        private int current_exp;
    }
}
