package top.sshh.bililiverecoder.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class LiveMsg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long partId;
    private String bvid;
    private Long cid;

    /**
     * 弹幕内容
     */
    private String context;

    /**
     * 十进制RGB888值
     * 默认为16777215（#FFFFFF）白色
     */
    private int color = 25;

    /**
     * 默认为25
     * 极小：12
     * 超小：16
     * 小：18
     * 标准：25
     * 大：36
     * 超大：45
     * 极大：64
     */
    private int fontsize = 25;

    /**
     * 0：普通池
     * 1：字幕池
     * 2：特殊池（代码/BAS弹幕）
     * 默认为0
     */
    private int pool = 0;

    /**
     * 1：普通弹幕
     * 4：底部弹幕
     * 5：顶部弹幕
     * 7：高级弹幕
     */
    private int mode = 1;

    /**
     * 发送时间戳
     */
    private Long sendTime;

    // 1 未发送，2已发送
    private boolean isSend;

    /**
     * 0：成功
     * -101：账号未登录
     * -102：账号被封停
     * -111：csrf校验失败
     * -400：请求错误
     * -404：无此项
     * 36700：系统升级中
     * 36701：弹幕包含被禁止的内容
     * 36702：弹幕长度大于100
     * 36703：发送频率过快
     * 36704：禁止向未审核的视频发送弹幕
     * 36705：您的等级不足，不能发送弹幕
     * 36706：您的等级不足，不能发送顶端弹幕
     * 36707：您的等级不足，不能发送底端弹幕
     * 36708：您的等级不足，不能发送彩色弹幕
     * 36709：您的等级不足，不能发送高级弹幕
     * 36710：您的权限不足，不能发送这种样式的弹幕
     * 36711：该视频禁止发送弹幕
     * 36712：level 1用户发送弹幕的最大长度为20
     * 36713：稿件未付费
     * 36714：弹幕发送时间不合法
     * 36715：当日操作数量超过上限
     */
    private int code = -1;


    /**
     * type: 1
     * oid: 4498330
     * msg: [0,0,"1-1",10,"暗夜sc：111111",0,1,50,50,500,0,1,"SimSun",0]
     * progress: 1010
     * color: 16776960
     * fontsize: 40
     * pool: 0
     * mode: 7
     * rnd: 2
     * plat: 1
     * aid: 2878892
     * polaris_appid: 100
     * polaris_platfrom: 5
     * spmid: 333.788.0.0
     * from_spmid:
     * csrf: d6424937b906413a5e7f788936be09b2
     */
}
