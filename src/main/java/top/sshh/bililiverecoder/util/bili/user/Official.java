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
public class Official {

    /**
     * 认证类型	0：无
     * 1 2 7 9：个人认证
     * 3 4 5 6：机构认证
     */
    private int role;
    /**
     * 认证信息	无为空
     */
    private String title;
    /**
     * 认证备注	无为空
     */
    private String desc;
    /**
     * 是否认证
     * -1：无
     * 0：认证
     */
    private int type;

}