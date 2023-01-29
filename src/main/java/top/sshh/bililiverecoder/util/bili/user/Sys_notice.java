/**
 * Copyright 2022 json.cn
 */
package top.sshh.bililiverecoder.util.bili.user;

import lombok.Data;

/**
 * Auto-generated: 2022-07-22 9:39:59
 *
 * @author zimo
 */
@Data
public class Sys_notice {
    /**
     * 系统提示类型id
     * 8：争议账号
     * 11：合约争议
     * 20：纪念账号
     * 22：合约诉讼
     * 24：合约争议
     * 25：严重指控
     */
    private int id;
    /**
     * 提示文案
     */
    private String content;
    /**
     * 提示信息页面url
     */
    private String url;
    /**
     * 提示图标url
     */
    private String icon;
    /**
     * 提示文字颜色
     */
    private String text_color;
    /**
     * 提示背景颜色
     */
    private String bg_color;

}