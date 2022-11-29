package top.sshh.bililiverecoder.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventData;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.RecordEventService;

import java.time.LocalDateTime;

@Slf4j
@Component
public class RecordEventStreamStartService implements RecordEventService {

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

        RecordRoom room = roomRepository.findByRoomId(eventData.getRoomId());
        if (room == null) {
            log.error("房间不存在，重新创建房间保存数据库");
            room = new RecordRoom();
            room.setRoomId(eventData.getRoomId());
            room.setCreateTime(LocalDateTime.now());
            room.setUname(eventData.getName());
            room.setTitle(eventData.getTitle());
            room = roomRepository.save(room);
        }
        room.setTitle(eventData.getTitle());
        room.setSessionId(eventData.getSessionId());
        room.setRecording(eventData.isRecording());
        room.setStreaming(eventData.isStreaming());
        room = roomRepository.save(room);
    }
}
