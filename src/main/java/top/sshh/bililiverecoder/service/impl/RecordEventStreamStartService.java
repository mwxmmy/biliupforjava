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
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.RecordEventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RecordEventStreamStartService implements RecordEventService {

    @Value("${record.wx-push-token}")
    private String wxToken;

    private static final String WX_MSG_FORMAT= """
            主播%s开播了
            房间名: %s
            父分区: %s
            子分区: %s
            时间: %s
            """;

    @Autowired
    private BiliUserRepository biliUserRepository;

    @Autowired
    private RecordRoomRepository roomRepository;

    @Autowired
    private RecordHistoryRepository historyRepository;

    @Autowired
    private RecordHistoryPartRepository historyPartRepository;

    @Autowired
    private LiveMsgRepository liveMsgRepository;


    @Override
    public void processing(RecordEventDTO event) {
        RecordEventData eventData = event.getEventData();
        String roomId = eventData.getRoomId();
        RecordRoom room = roomRepository.findByRoomId(eventData.getRoomId());
        if (room == null) {
            synchronized (roomId.intern()) {
                room = roomRepository.findByRoomId(eventData.getRoomId());
                if (room == null) {
                    log.error("房间不存在，重新创建房间保存数据库");
                    room = new RecordRoom();
                    room.setRoomId(eventData.getRoomId());
                    room.setCreateTime(LocalDateTime.now());
                    room.setTitle(eventData.getTitle());
                    room = roomRepository.save(room);
                }
            }
        }
        room.setUname(eventData.getName());
        room.setTitle(eventData.getTitle());
        room.setSessionId(eventData.getSessionId());
        room.setRecording(eventData.isRecording());
        room.setStreaming(eventData.isStreaming());
        room = roomRepository.save(room);
        String wxuid = room.getWxuid();
        String pushMsgTags = room.getPushMsgTags();
        if(StringUtils.isNotBlank(wxuid)&&StringUtils.isNotBlank(pushMsgTags)&&pushMsgTags.contains("开始直播")){
            Message message = new Message();
            message.setAppToken(wxToken);
            message.setContentType(Message.CONTENT_TYPE_TEXT);
            message.setContent(WX_MSG_FORMAT.formatted(room.getUname(),room.getTitle(),
                    eventData.getAreaNameParent(),eventData.getAreaNameChild(),LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分ss秒"))));
            message.setUid(wxuid);
            WxPusher.send(message);
        }
    }
}
