package top.sshh.bililiverecoder.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.RecordEventService;
import top.sshh.bililiverecoder.service.UploadServiceFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Component
public class RecordEventFileClosedService implements RecordEventService {

    @Value("${record.work-path}")
    private String workPath;

    @Autowired
    private BiliUserRepository biliUserRepository;

    @Autowired
    private RecordRoomRepository roomRepository;

    @Autowired
    private RecordHistoryRepository historyRepository;

    @Autowired
    private RecordHistoryPartRepository historyPartRepository;

    @Autowired
    private UploadServiceFactory uploadServiceFactory;


    @Override
    public void processing(RecordEventDTO event) {
        RecordEventData eventData = event.getEventData();
        log.info("分p录制结束事件==>{}", eventData.getRelativePath());
        RecordRoom room = roomRepository.findByRoomId(eventData.getRoomId());
        Optional<RecordHistory> historyOptional = historyRepository.findById(room.getHistoryId());
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            // 正常逻辑
            RecordHistoryPart part = historyPartRepository.findByFilePath(workPath + File.separator + eventData.getRelativePath());
            if (part == null) {
                log.info("文件分片不存在==>{}", eventData.getRelativePath());
                part = new RecordHistoryPart();
                part.setStartTime(LocalDateTime.now().minusSeconds((long) eventData.getDuration()));
                part.setEventId(event.getEventId());
                part.setTitle(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM月dd日HH点mm分ss秒")));
                part.setAreaName(eventData.getAreaNameChild());
                part.setRoomId(history.getRoomId());
                part.setHistoryId(history.getId());
                part.setFilePath(workPath + File.separator + eventData.getRelativePath());
                part.setFileSize(0L);
                part.setSessionId(eventData.getSessionId());
                part.setRecording(eventData.isRecording());
                part.setStartTime(LocalDateTime.now());
                part.setEndTime(LocalDateTime.now());
            }
            long fileSize = eventData.getFileSize();
            part.setRecording(false);
            part.setFileSize(fileSize);
            part.setDuration(eventData.getDuration());
            part.setEndTime(LocalDateTime.now());
            part.setAreaName(eventData.getAreaNameChild());
            part.setUpdateTime(LocalDateTime.now());
            part = historyPartRepository.save(part);

            history.setFileSize(history.getFileSize() + part.getFileSize());
            history.setTitle(eventData.getTitle());
            history.setSessionId(eventData.getSessionId());
            history.setRecording(eventData.isRecording());
            history.setStreaming(eventData.isStreaming());
            history.setUpdateTime(LocalDateTime.now());
            history.setEndTime(LocalDateTime.now());
            history = historyRepository.save(history);

            // 文件上传操作
            //开始上传该视频分片，异步上传任务。
            // 小于设定文件大小和时长不上传
            if (fileSize > 1024 * 1024 * room.getFileSizeLimit() && part.getDuration() > room.getDurationLimit()) {
                uploadServiceFactory.getUploadService(room.getLine()).asyncUpload(part);
            } else {
                log.error("文件大小小于设置的忽略大小或时长，删除。");
                historyPartRepository.delete(part);
            }
        } else {
            log.error("分p录制结束事件，录制历史不存在。");
            RecordHistoryPart part = new RecordHistoryPart();
            part.setStartTime(LocalDateTime.now().minusSeconds((long) eventData.getDuration()));
            part.setEventId(event.getEventId());
            part.setTitle(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM月dd日HH点mm分ss秒")));
            part.setAreaName(eventData.getAreaNameChild());
            part.setRoomId(eventData.getRoomId());
            part.setFilePath(workPath + File.separator + eventData.getRelativePath());
            part.setFileSize(0L);
            part.setSessionId(eventData.getSessionId());
            part.setRecording(eventData.isRecording());
            part.setStartTime(LocalDateTime.now());
            part.setEndTime(LocalDateTime.now());
            historyPartRepository.save(part);
        }

    }
}
