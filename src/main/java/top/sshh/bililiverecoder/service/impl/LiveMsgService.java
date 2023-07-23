package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.entity.data.BiliDmResponse;
import top.sshh.bililiverecoder.entity.data.BiliVideoInfoResponse;
import top.sshh.bililiverecoder.repo.LiveMsgRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
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


    @Autowired
    private JdbcService jdbcService;

    @Autowired
    private LiveMsgRepository liveMsgRepository;
    @Autowired
    private RecordHistoryRepository recordHistoryRepository;
    @Autowired
    private RecordRoomRepository roomRepository;

    @Autowired
    RecordHistoryPartRepository partRepository;

    public int sendMsg(BiliBiliUser user, LiveMsg liveMsg) {
        BiliDmResponse response = BiliApi.sendVideoDm(user, liveMsg);
        int code = response.getCode();
        if (code != 0) {
            log.error("{}发送弹幕错误，code==>{}", user.getUname(), code);
            if (code == 36701 || code == 36702 || code == 36714) {
                liveMsgRepository.delete(liveMsg);
            }
            if(code == 36704){
                String bvid = liveMsg.getBvid();
                this.syncVideoState(bvid);
                return code;
            }
        }
        liveMsg.setCode(code);
        liveMsgRepository.save(liveMsg);
        return code;
    }

    public static boolean checkUtf8Size(String testStr) {
        for (int i = 0; i < testStr.length(); i++) {
            int c = testStr.codePointAt(i);
            if (c < 0x0000 || c > 0xffff) {
                return true;
            }
        }
        return false;
    }

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
        RecordRoom room = roomRepository.findByRoomId(part.getRoomId());
        String dmKeywordBlacklist = room.getDmKeywordBlacklist();
        String[] EXCLUSION_DM;
        if (StringUtils.isNotBlank(dmKeywordBlacklist)) {
            EXCLUSION_DM = dmKeywordBlacklist.split("\n");
        } else {
            EXCLUSION_DM = new String[0];
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
                BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1000000, 0.01);
                for (Node node : nodes) {
                    DefaultElement element = (DefaultElement) node;

                    String text = element.getText().trim().replace("\n", ",").replace("\r", ",").toLowerCase();
                    text = StringUtils.deleteWhitespace(text);
                    //过滤utf8字符大小为4的
                    if (checkUtf8Size(text)) {
                        continue;
                    }
                    //排除垃圾弹幕
                    boolean isContinue = false;
                    for (String s : EXCLUSION_DM) {
                        if (text.contains(s.toLowerCase())) {
                            isContinue = true;
                            break;
                        }
                    }
                    if (isContinue) {
                        continue;
                    }
                    if (element.attribute("raw") != null) {
                        String raw = element.attribute("raw").getValue();
                        JSONArray array = JSON.parseArray(raw);

                        // 判断是否抽奖弹幕
                        boolean lottery = (Integer)((JSONArray)array.get(0)).get(9) != 0;
                        if(lottery){
                            continue;
                        }

                        JSONArray dmFanMedalObjects = (JSONArray) array.get(3);
                        // 0-不做处理，1-必须佩戴粉丝勋章。2-必须佩戴主播的粉丝勋章
                        if (room.getDmFanMedal() == 1) {
                            if (dmFanMedalObjects.size() == 0) {
                                continue;
                            }
                        } else if (room.getDmFanMedal() == 2) {
                            if (dmFanMedalObjects.size() == 0) {
                                continue;
                            }
                            String roomId = dmFanMedalObjects.get(3).toString();
                            if (!part.getRoomId().equals(roomId)) {
                                continue;
                            }
                        }
                        Integer ulLive = (Integer) ((JSONArray) array.get(4)).get(0);
                        //排除低级用户
                        if (ulLive < room.getDmUlLevel()) {
                            if (dmFanMedalObjects.size() == 0) {
                                continue;
                            }
                        }
                    }

                    String value = element.attribute("p").getValue();
                    String[] values = value.split(",");
                    long sendTime = (long) (Float.parseFloat(values[0]) * 1000) - 10000L;
                    if (sendTime < 0) {
                        continue;
                    }
                    int fontsize = Integer.parseInt(values[2]);
                    int color = Integer.parseInt(values[3]);
                    //弹幕重复过滤
                    if (room.getDmDistinct() != null && room.getDmDistinct()) {
                        if (!bloomFilter.put(text)) {
                            continue;
                        }
                    }
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
    
    
    public void syncVideoState(String bvid) {
        RecordHistory history = recordHistoryRepository.findByBvId(bvid);
        BiliVideoInfoResponse videoInfoResponse = BiliApi.getVideoInfo(history.getBvId());
        int code = videoInfoResponse.getCode();
        BiliVideoInfoResponse.BiliVideoInfo videoInfoResponseData = videoInfoResponse.getData();
        if (code != 0 || videoInfoResponseData.getState() != 0) {
            history.setCode(-1);
            history = recordHistoryRepository.save(history);
        }
    }
}
