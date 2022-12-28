package top.sshh.bililiverecoder.service.impl;

import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventData;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.RecordEventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Component
public class RecordEventRecordEndService implements RecordEventService {

    @Value("${record.wx-push-token}")
    private String wxToken;

    private static final String WX_MSG_FORMAT= """
            收到主播%s开录制结束
            房间名: %s
            父分区: %s
            子分区: %s
            时间: %s
            若十分钟内未收到录制开始事件，
            则在上传完成后发布视频。
            """;
    @Autowired
    private RecordHistoryRepository historyRepository;
    @Autowired
    private RecordRoomRepository roomRepository;

    @Autowired
    private RecordBiliPublishService recordBiliPublishService;


    @Override
    public void processing(RecordEventDTO event) {
        RecordEventData eventData = event.getEventData();
        log.info("录制结束事件==>{}=={}", eventData.getRoomId(), eventData.getTitle());
        try {
            Thread.sleep(2000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
        RecordRoom room = roomRepository.findByRoomId(eventData.getRoomId());
        Optional<RecordHistory> historyOptional = historyRepository.findById(room.getHistoryId());
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            history.setSessionId(eventData.getSessionId());
            history.setEndTime(LocalDateTime.now());
            history.setRecording(false);
            history.setStreaming(false);
            historyRepository.save(history);
            room.setRecording(false);
            room.setStreaming(false);
            room.setSessionId(null);
            roomRepository.save(room);
        }
        String wxuid = room.getWxuid();
        String pushMsgTags = room.getPushMsgTags();
        if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("录制结束")){
            Message message = new Message();
            message.setAppToken(wxToken);
            message.setContentType(Message.CONTENT_TYPE_TEXT);
            message.setContent(WX_MSG_FORMAT.formatted(room.getUname(),room.getTitle(),
                    eventData.getAreaNameParent(),eventData.getAreaNameChild(),LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒"))));
            message.setUid(wxuid);
            WxPusher.send(message);
        }
//        recordBiliPublishService.publishRecordHistory(history);
    }
}
