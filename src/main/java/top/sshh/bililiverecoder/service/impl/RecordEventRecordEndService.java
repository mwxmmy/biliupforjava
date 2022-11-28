package top.sshh.bililiverecoder.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventData;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.RecordEventService;

@Slf4j
@Component
public class RecordEventRecordEndService implements RecordEventService {

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
        String sessionId = eventData.getSessionId();
        RecordHistory history = historyRepository.findBySessionId(sessionId);

        history.setSessionId(eventData.getSessionId());
        history.setRecording(eventData.isRecording());
        history.setStreaming(eventData.isStreaming());
        historyRepository.save(history);
        RecordRoom room = roomRepository.findByRoomId(eventData.getRoomId());
        room.setRecording(eventData.isRecording());
        room.setStreaming(eventData.isStreaming());
        room.setHistoryId(null);
        room.setSessionId(null);
        roomRepository.save(room);
        recordBiliPublishService.publishRecordHistory(history);
    }
}
