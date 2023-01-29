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
public class Label {

    private String path;
    /**
     * 会员类型文案
     */
    private String text;
    /**
     * 会员标签
     * vip：大会员
     * annual_vip：年度大会员
     * ten_annual_vip：十年大会员
     * hundred_annual_vip：百年大会员
     */
    private String label_theme;
    /**
     * 会员标签
     */
    private String text_color;
    private int bg_style;
    private String bg_color;
    private String border_color;
    private boolean use_img_label;
    private String img_label_uri_hans;
    private String img_label_uri_hant;
    private String img_label_uri_hans_static;
    private String img_label_uri_hant_static;
}