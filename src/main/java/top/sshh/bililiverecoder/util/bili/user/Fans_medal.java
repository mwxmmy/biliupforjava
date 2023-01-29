/**
 * Copyright 2022 json.cn
 */
package top.sshh.bililiverecoder.util.bili.user;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * Auto-generated: 2022-07-22 9:39:59
 *
 * @author json.cn (i@json.cn)
 * @website http://www.json.cn/java2pojo/
 */
@Data
public class Fans_medal {

    /**
     * 是否展示
     */
    private boolean show;
    /**
     * 是否佩戴了粉丝勋章
     */
    private boolean wear;
    /**
     * 粉丝勋章信息(元数据未能写pojo)
     * 字段	                类型	            内容	                    备注
     * uid	                num	            此用户mid
     * target_id	        num	            粉丝勋章所属UP的mid
     * medal_id	            num	            粉丝勋章id
     * level	            num	            粉丝勋章等级
     * medal_name	        str	            粉丝勋章名称
     * medal_color	        num	            颜色
     * intimacy	            num	            当前亲密度
     * next_intimacy	    num	            下一等级所需亲密度
     * day_limit	        num	            每日亲密度获取上限
     * medal_color_start	num
     * medal_color_end	    num
     * medal_color_border	num
     * is_lighted	        num
     * light_status	        num
     * wearing_status	    num	            当前是否佩戴	                0：未佩戴  1：已佩戴
     * score	            num
     */
    private JSONObject medal;
}