package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventData;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.RecordEventService;

import java.time.LocalDateTime;

@Slf4j
@Component
public class RecordEventRecordStartedService implements RecordEventService {

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
        if(room == null){
            log.error("房间不存在，重新创建房间保存数据库");
            room = new RecordRoom();
            room.setRoomId(eventData.getRoomId());
            room.setCreateTime(LocalDateTime.now());
            room = roomRepository.save(room);
        }
        room.setUname(eventData.getName());
        room.setTitle(eventData.getTitle());
        room.setSessionId(eventData.getSessionId());
        room.setRecording(eventData.isRecording());
        room.setStreaming(eventData.isStreaming());
        RecordHistory history = historyRepository.findBySessionId(eventData.getSessionId());
        if(history == null){
            history = new RecordHistory();
            history.setRoomId(room.getRoomId());
            history.setStartTime(LocalDateTime.now());
            history.setTitle(eventData.getTitle());
            history.setUpload(room.isUpload());
        }else {
            log.error("开始录制异常，不应该存在的情况，sessionId已存在，使用上一次创建的history ==> {}", JSON.toJSONString(history));
        }
        history.setEventId(event.getEventId());
        history.setSessionId(eventData.getSessionId());
        history.setRecording(eventData.isRecording());
        history.setStreaming(eventData.isStreaming());
        historyRepository.save(history);
        room.setHistoryId(history.getId());
        roomRepository.save(room);
        log.info("录制开始事件处理完成");
    }
}
