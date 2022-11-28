package top.sshh.bililiverecoder.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.RecordEventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RecordEventFileOpenService implements RecordEventService {

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
        String sessionId = eventData.getSessionId();
        try {
            Thread.sleep(1000L);
        }catch (Exception e){
            e.printStackTrace();
        }
        RecordHistory history = historyRepository.findBySessionId(sessionId);
        //异常情况判断
        if(history == null){
            log.error("录制异常，录制历史没有创建，可能webhook请求顺序错误");
            RecordRoom room = roomRepository.findByRoomId(eventData.getRoomId());
            if(room == null){
                log.error("录制异常，录制历史没有创建，录制房间也没有创建！！！可能webhook请求顺序错误");
                room = new RecordRoom();
                room.setRoomId(eventData.getRoomId());
                room.setCreateTime(LocalDateTime.now());
                room.setUname(eventData.getName());
                room.setTitle(eventData.getTitle());
                roomRepository.save(room);
            }
            history = new RecordHistory();
            history.setId(event.getEventId());
            history.setRoomId(room.getRoomId());
            history.setStartTime(LocalDateTime.now());
            history.setTitle(eventData.getTitle());
            history.setSessionId(eventData.getSessionId());
            history.setRecording(eventData.isRecording());
            history.setStreaming(eventData.isStreaming());
            historyRepository.save(history);
        }
        // 正常逻辑
        boolean existsPart = historyPartRepository.existsById(event.getEventId());
        if(existsPart){
            return;
        }

        RecordHistoryPart part = new RecordHistoryPart();
        part.setId(event.getEventId());
        part.setTitle(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM月dd日HH点mm分ss秒")));
        part.setRoomId(history.getRoomId());
        part.setHistoryId(history.getId());
        part.setFilePath(eventData.getRelativePath());
        part.setSessionId(eventData.getSessionId());
        part.setRecording(eventData.isRecording());
        part.setStartTime(LocalDateTime.now());
        historyPartRepository.save(part);

        String relativePath = eventData.getRelativePath();
        history.setFilePath(relativePath.substring(0,relativePath.lastIndexOf('/')));
        historyRepository.save(history);

    }
}
