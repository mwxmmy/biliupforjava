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
public class Nameplate {

    /**
     * 勋章id	详细说明有待补充
     */
    private int nid;
    /**
     * 勋章名称
     */
    private String name;
    /**
     * 挂件图片url 正常
     */
    private String image;
    /**
     * 勋章图片url 小
     */
    private String image_small;
    /**
     * 勋章等级
     */
    private String level;
    /**
     * 勋章条件
     */
    private String condition;

}