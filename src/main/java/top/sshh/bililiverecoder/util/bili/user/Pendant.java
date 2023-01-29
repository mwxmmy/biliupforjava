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
public class Pendant {

    /**
     * 头像框id
     */
    private int pid;
    /**
     * 头像框名称
     */
    private String name;
    /**
     * 头像框图片url
     */
    private String image;
    private int expire;
    private String image_enhance;
    private String image_enhance_frame;


    /**
     * 评论区的用户头像框 id
     */
    private int id;
    private String jump_url;
    private String type;
}