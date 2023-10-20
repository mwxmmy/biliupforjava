package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.RecordEventService;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Component
public class RecordEventFileOpenService implements RecordEventService {

    @Value("${record.work-path}")
    private String workPath;

    @PostConstruct
    public void initWorkPath() {
        workPath = workPath.replace("\\", "/");
    }

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
        String relativePath = eventData.getRelativePath();
        log.info("分p开始录制事件==>{}", relativePath);
        String sessionId = eventData.getSessionId();
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String roomId = eventData.getRoomId();
        RecordRoom room = roomRepository.findByRoomId(eventData.getRoomId());
        if (room == null) {
            synchronized (roomId.intern()) {
                room = roomRepository.findByRoomId(eventData.getRoomId());
                if (room == null) {
                    log.error("录制异常，录制历史没有创建，录制房间也没有创建！！！可能webhook请求顺序错误");
                    room = new RecordRoom();
                    room.setRoomId(eventData.getRoomId());
                    room.setCreateTime(LocalDateTime.now());
                    if (eventData.getName() != null) {
                        room.setUname(eventData.getName());
                    }
                    room.setTitle(eventData.getTitle());
                    room.setHistoryId(-999L);
                    room = roomRepository.save(room);
                }
            }
        } else {
            room.setUname(eventData.getName());
            room.setTitle(eventData.getTitle());
            room.setSessionId(eventData.getSessionId());
            room.setRecording(eventData.isRecording());
            room.setStreaming(eventData.isStreaming());
            room = roomRepository.save(room);
        }
        Optional<RecordHistory> historyOptional = historyRepository.findById(room.getHistoryId());
        RecordHistory history;
        //异常情况判断
        if (!historyOptional.isPresent()) {
            log.error("录制异常，录制历史没有创建，可能webhook请求顺序错误");

            history = new RecordHistory();
            history.setEventId(event.getEventId());
            history.setRoomId(room.getRoomId());
            history.setStartTime(LocalDateTime.now());
            history.setEndTime(LocalDateTime.now());
            history.setTitle(eventData.getTitle());
            history.setSessionId(eventData.getSessionId());
            history.setRecording(eventData.isRecording());
            history.setStreaming(eventData.isStreaming());
            history = historyRepository.save(history);
            room.setHistoryId(history.getId());
            room = roomRepository.save(room);
        } else {
            history = historyOptional.get();
        }
        int partCount = historyPartRepository.countByHistoryId(history.getId());

        if(partCount>99){
            log.error("录制异常，该录制历史part数量已达到100，强制分次投稿");
            //更新唯一键,更新录制状态
            history.setEventId(history.getEventId()+1);
            history.setSessionId(history.getSessionId()+1);
            history.setRecording(false);
            history.setStreaming(false);
            history = historyRepository.save(history);
            //创建新的录制历史
            history = new RecordHistory();
            history.setEventId(event.getEventId());
            history.setRoomId(room.getRoomId());
            history.setStartTime(LocalDateTime.now());
            history.setEndTime(LocalDateTime.now());
            history.setTitle(eventData.getTitle());
            history.setSessionId(eventData.getSessionId());
            history.setRecording(eventData.isRecording());
            history.setStreaming(eventData.isStreaming());
            history = historyRepository.save(history);
        }
        if ("blrec".equals(sessionId)) {
            relativePath = relativePath.replace(workPath, "");
        }
        String filePath = workPath + File.separator + relativePath;
        // 正常逻辑
        boolean existsPart = historyPartRepository.existsByFilePath(filePath);
        if(existsPart){
            log.error("eventId 查询分p已存在，filePath==>{}", filePath);
            return;
        }
        RecordHistoryPart part = new RecordHistoryPart();
        part.setEventId(event.getEventId());
        part.setTitle(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM月dd日HH点mm分ss秒")));
        part.setAreaName(eventData.getAreaNameChild());
        part.setRoomId(history.getRoomId());
        part.setHistoryId(history.getId());
        part.setFilePath(filePath);
        part.setFileSize(0L);
        part.setSessionId(eventData.getSessionId());
        part.setRecording(eventData.isRecording());
        part.setStartTime(LocalDateTime.now());
        part.setEndTime(LocalDateTime.now());
        part = historyPartRepository.save(part);
        log.info("分p开始录制事件,成功保存数据库==>{}", JSON.toJSONString(part));
        history.setTitle(eventData.getTitle());
        history.setSessionId(eventData.getSessionId());
        history.setRecording(eventData.isRecording());
        history.setStreaming(eventData.isStreaming());
        history.setFilePath(workPath + File.separator + relativePath.substring(0, relativePath.lastIndexOf('/')));
        history.setEndTime(LocalDateTime.now());
        historyRepository.save(history);

    }
}
