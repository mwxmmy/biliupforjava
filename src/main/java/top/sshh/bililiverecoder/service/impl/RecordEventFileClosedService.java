package top.sshh.bililiverecoder.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventData;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.RecordEventService;
import top.sshh.bililiverecoder.service.RecordPartUploadService;

import java.io.File;
import java.time.LocalDateTime;
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
    private RecordPartUploadService uploadService;

    @Autowired
    private LiveMsgRepository liveMsgRepository;


    @Override
    public void processing(RecordEventDTO event) {
        RecordEventData eventData = event.getEventData();
        log.info("分p录制结束事件==>{}", eventData.getRelativePath());
        // 正常逻辑
        RecordHistoryPart part = historyPartRepository.findByFilePath(workPath + File.separator + eventData.getRelativePath());
        if (part == null) {
            log.info("文件分片不存在==>{}", eventData.getRelativePath());
            return;
        }
        part.setRecording(false);
        part.setFileSize(eventData.getFileSize());
        part.setEndTime(LocalDateTime.now());
        part.setUpdateTime(LocalDateTime.now());
        historyPartRepository.save(part);
        Optional<RecordHistory> historyOptional = historyRepository.findById(part.getHistoryId());
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            history.setFileSize(history.getFileSize()+ part.getFileSize());
            history.setUpdateTime(LocalDateTime.now());
            historyRepository.save(history);
        }
        // 文件上传操作
        //开始上传该视频分片，异步上传任务。
        uploadService.asyncUpload(part);

        //TODO 解析弹幕入库
    }
}
