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

    private static List<String> EXCLUSION_DM = new ArrayList<>();

    static {
        EXCLUSION_DM.add("天选之人");
        EXCLUSION_DM.add("老板大气");
        EXCLUSION_DM.add("红包");
    }

    @Autowired
    private JdbcService jdbcService;

    @Autowired
    private LiveMsgRepository liveMsgRepository;
    @Autowired
    private RecordHistoryRepository recordHistoryRepository;

    public int sendMsg(BiliBiliUser user, LiveMsg liveMsg) {
        BiliDmResponse response = BiliApi.sendVideoDm(user.getAccessToken(), liveMsg);
        int code = response.getCode();
        liveMsg.setCode(code);
        liveMsgRepository.save(liveMsg);
        if (code != 0) {
            log.error("发送弹幕错误，code==>{}", code);
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
                List<Node> nodes = rootElement.selectNodes("/i/d");
                //限制每秒钟不超过3条弹幕
                long time = 0;
                int limit = 0;
                BloomFilter<CharSequence> bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 1000000, 0.01);
                List<LiveMsg> liveMsgs = new ArrayList<>();
                for (Node node : nodes) {
                    DefaultElement element = (DefaultElement) node;
                    String text = element.getText();
                    //短弹幕没有意义
                    if (text.length() < 3) {
                        continue;
                    }
                    //排出垃圾弹幕
                    if (EXCLUSION_DM.contains(text)) {
                        return;
                    }
                    String value = element.attribute("p").getValue();
                    String[] values = value.split(",");
                    long sendTime = (long) (Float.parseFloat(values[0]) * 1000);
                    int fontsize = Integer.parseInt(values[2]);
                    int color = Integer.parseInt(values[3]);
                    //如果显示时间超过当前时间，调整当前时间
                    if (sendTime > time + 1000) {
                        time = (int) sendTime;
                        limit = 0;
                    } else {
                        limit++;
                    }
                    if (limit > 3) {
                        continue;
                    }
                    if (bloomFilter.mightContain(text)) {
                        LiveMsg msg = new LiveMsg();
                        msg.setPartId(part.getId());
                        msg.setBvid(bvid);
                        msg.setCid(part.getCid());
                        msg.setSendTime(sendTime);
                        msg.setFontsize(fontsize);
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
}
