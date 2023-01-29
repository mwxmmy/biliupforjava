/**
 * Copyright 2022 json.cn
 */
package top.sshh.bililiverecoder.util.bili.user;

import lombok.Data;

/**
 * Auto-generated: 2022-07-22 22:35:23
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class Level_exp {

    /**
     * 当前等级	0-6级
     */
    private int current_level;
    /**
     * 指当前等级从多少经验值开始
     */
    private int current_min;
    /**
     * 当前账户的经验值
     */
    private int current_exp;
    /**
     * 下一个等级所需的经验值**(不是还需要多少)**
     */
    private int next_exp;
}