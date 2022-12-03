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

import java.time.LocalDateTime;
import java.util.Optional;

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

//        recordBiliPublishService.publishRecordHistory(history);
    }
}
