package top.sshh.bililiverecoder.service.impl;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.LiveMsg;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.data.BiliDmResponse;
import top.sshh.bililiverecoder.repo.LiveMsgRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.util.BiliApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Transactional
@Component
public class LiveMsgService {


    private static final String ZH_CHAR_REGX = "^[\u4e00-\u9fa5]{1}$";

    private static final List<String> EXCLUSION_DM = new ArrayList<>();

    static {
        EXCLUSION_DM.add("天选之人");
        EXCLUSION_DM.add("老板大气");
        EXCLUSION_DM.add("红包");
        EXCLUSION_DM.add("关注");
        EXCLUSION_DM.add("傻逼");
        EXCLUSION_DM.add("妈");
        EXCLUSION_DM.add("草");
        EXCLUSION_DM.add("垃圾");
        EXCLUSION_DM.add("狗");
        EXCLUSION_DM.add("蛆");
        EXCLUSION_DM.add("脑浆");
        EXCLUSION_DM.add("退役");
        EXCLUSION_DM.add("好卡");
        EXCLUSION_DM.add("死");
        EXCLUSION_DM.add("艹");
        EXCLUSION_DM.add("举报");
        EXCLUSION_DM.add("弹幕");
        EXCLUSION_DM.add("画质");
        EXCLUSION_DM.add("傻子");
        EXCLUSION_DM.add("卡卡");
        EXCLUSION_DM.add("好卡");
        EXCLUSION_DM.add("不卡");
        EXCLUSION_DM.add("卡了");
        EXCLUSION_DM.add("nm");
        EXCLUSION_DM.add("NM");
        EXCLUSION_DM.add("nM");
        EXCLUSION_DM.add("Nm");
        EXCLUSION_DM.add("难看");
        EXCLUSION_DM.add("没意思");
        EXCLUSION_DM.add("尼");
        EXCLUSION_DM.add("玛");
        EXCLUSION_DM.add("哈哈哈");
        EXCLUSION_DM.add("恶心");
        EXCLUSION_DM.add("屎");
        EXCLUSION_DM.add("sb");
        EXCLUSION_DM.add("sB");
        EXCLUSION_DM.add("SB");
        EXCLUSION_DM.add("Sb");
    }

    @Autowired
    private JdbcService jdbcService;

    @Autowired
    private LiveMsgRepository liveMsgRepository;
    @Autowired
    private RecordHistoryRepository recordHistoryRepository;

    public int sendMsg(BiliBiliUser user, LiveMsg liveMsg) {
        BiliDmResponse response = BiliApi.sendVideoDm(user, liveMsg);
        int code = response.getCode();
        if (code != 0) {
            log.error("{}发送弹幕错误，code==>{}",user.getUname(), code);
            if(code == 36701 || code == 36702 || code == 36714){
                liveMsgRepository.delete(liveMsg);
            }
        }else {
            liveMsg.setCode(code);
            liveMsgRepository.save(liveMsg);
        }
        return code;
    }

    @Async
    public void processing(RecordHistoryPart part) {
        Optional<RecordHistory> historyOptional = recordHistoryRepository.findById(part.getHistoryId());
        String bvid = "";
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            bvid = history.getBvId();
        }
        if (StringUtils.isBlank(bvid) || part.getCid() == null || part.getCid() < 1) {
            return;
        }
        int partCount = liveMsgRepository.countByPartId(part.getId());
        //弹幕多就不重新加载了
        if (partCount > 100) {
            return;
        } else {
            liveMsgRepository.deleteByPartId(part.getId());
        }
        String filePath = part.getFilePath();
        filePath = filePath.replaceAll(".flv", ".xml");
        File file = new File(filePath);
        boolean exists = file.exists();
        if (exists) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(file);

                //2.创建org.dom4j.io包中的SAXReader对象
                SAXReader saxReader = new SAXReader();

                Document document = null;

                document = saxReader.read(stream);

                //4.拿到根元素
                Element rootElement = document.getRootElement();

                List<LiveMsg> liveMsgs = new ArrayList<>();


                // sc弹幕处理
                List<Node> scNodes = rootElement.selectNodes("/i/sc");
                for (Node node : scNodes) {
                    DefaultElement element = (DefaultElement) node;
                    String time = element.attribute("ts").getValue();
                    long sendTime = (long) (Float.parseFloat(time) * 1000);
                    String userName = element.attribute("user").getValue();
                    String price = element.attribute("price").getValue();
                    String text = element.getText();
                    LiveMsg msg = new LiveMsg();
                    msg.setPartId(part.getId());
                    msg.setBvid(bvid);
                    msg.setCid(part.getCid());
                    msg.setSendTime(sendTime);
                    msg.setMode(5);
                    msg.setPool(1);
                    msg.setFontsize(64);
                    msg.setColor(16776960);
                    StringBuilder builder = new StringBuilder();
                    builder.append(userName).append("发送了").append(price).append("元留言：").append(text);
                    if (builder.length() > 100) {
                        text = builder.substring(0, 99);
                    } else {
                        text = builder.toString();
                    }
                    msg.setContext(text);
                    liveMsgs.add(msg);
                }

                // sc弹幕处理
                List<Node> guardNodes = rootElement.selectNodes("/i/guard");
                for (Node node : guardNodes) {
                    DefaultElement element = (DefaultElement) node;
                    String time = element.attribute("ts").getValue();
                    long sendTime = (long) (Float.parseFloat(time) * 1000);
                    String userName = element.attribute("user").getValue();
                    String level = element.attribute("level").getValue();
                    String count = element.attribute("count").getValue();
                    LiveMsg msg = new LiveMsg();
                    msg.setPartId(part.getId());
                    msg.setBvid(bvid);
                    msg.setCid(part.getCid());
                    msg.setSendTime(sendTime);
                    msg.setMode(5);
                    msg.setPool(1);
                    msg.setColor(16776960);
                    StringBuilder builder = new StringBuilder();
                    builder.append(userName).append("开通了");
                    if (Integer.parseInt(count) > 1) {
                        builder.append(count).append("个月");
                    }
                    if ("1".equals(level)) {
                        msg.setFontsize(64);
                        builder.append("19998/月的总督");
                    } else if ("2".equals(level)) {
                        msg.setFontsize(64);
                        builder.append("1998/月的提督");
                    } else if ("3".equals(level)) {
                        msg.setFontsize(64);
                        builder.append("舰长");
                    } else {
                        builder.append("舰长");
                    }
                    String text;
                    if (builder.length() > 100) {
                        text = builder.substring(0, 99);
                    } else {
                        text = builder.toString();
                    }
                    msg.setContext(text);
                    liveMsgs.add(msg);
                }

                // 普通弹幕处理
                List<Node> nodes = rootElement.selectNodes("/i/d");
                //限制每两秒钟最多一条弹幕
                long time = 0;
                BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1000000, 0.01);
                for (Node node : nodes) {
                    DefaultElement element = (DefaultElement) node;
                    String userName = element.attribute("user").getValue();
                    //排除低级用户
                    if (userName.startsWith("bili") || userName.endsWith("bili")) {
                        continue;
                    }
                    String text = element.getText().trim().replace("\n", ",").replace("\r", ",");
                    //短文字弹幕没有意义
                    if (this.zhCharCount(text) < 3) {
                        continue;
                    }
                    //排除垃圾弹幕
                    boolean isContinue = false;
                    for (String s : EXCLUSION_DM) {
                        if (text.contains(s)) {
                            isContinue = true;
                            break;
                        }
                    }
                    if (isContinue) {
                        continue;
                    }

                    String value = element.attribute("p").getValue();
                    String[] values = value.split(",");
                    long sendTime = (long) (Float.parseFloat(values[0]) * 1000) - 10000L;
                    if (sendTime < 0) {
                        continue;
                    }
                    int fontsize = Integer.parseInt(values[2]);
                    int color = Integer.parseInt(values[3]);
                    //白色弹幕需要调整间隔
                    if (color == 1677215) {
                        //如果显示时间超过当前时间，调整当前时间
                        if (sendTime > time + 4000) {
                            time = (int) sendTime;
                        } else {
                            continue;
                        }
                    }
                    if (bloomFilter.put(text)) {
                        LiveMsg msg = new LiveMsg();
                        msg.setPartId(part.getId());
                        msg.setBvid(bvid);
                        msg.setCid(part.getCid());
                        msg.setSendTime(sendTime);
                        msg.setFontsize(fontsize);
                        msg.setMode(1);
                        msg.setPool(0);
                        msg.setColor(color);
                        msg.setContext(text);
                        liveMsgs.add(msg);
                        if (liveMsgs.size() > 500) {
                            jdbcService.saveLiveMsgList(liveMsgs);
                            log.info("{} 弹幕解析入库成功，一共入库{}条。", filePath, liveMsgs.size());
                            liveMsgs.clear();
                        }
                    }
                }
                jdbcService.saveLiveMsgList(liveMsgs);
                log.info("{} 弹幕解析入库成功，一共入库{}条。", filePath, liveMsgs.size());
            } catch (Exception e) {
                log.info("{} 弹幕解析入库失败", filePath, e);
                e.printStackTrace();
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int zhCharCount(String s) {
        int count = 0;
        char[] chars = s.toCharArray();
        for (char c : chars) {
            if (String.valueOf(c).matches(ZH_CHAR_REGX)) {
                count++;
            }
        }
        return count;
    }
}
