package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventData;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
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
        long fileSize = eventData.getFileSize();
        part.setRecording(false);
        part.setFileSize(fileSize);
        part.setEndTime(LocalDateTime.now());
        part.setUpdateTime(LocalDateTime.now());
        part = historyPartRepository.save(part);
        Optional<RecordHistory> historyOptional = historyRepository.findById(part.getHistoryId());
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            history.setFileSize(history.getFileSize() + part.getFileSize());
            history.setUpdateTime(LocalDateTime.now());
            history.setEndTime(LocalDateTime.now());
            history = historyRepository.save(history);
        } else {
            log.error("history不存在 part==>{}", JSON.toJSONString(part));
        }
        // 文件上传操作
        //开始上传该视频分片，异步上传任务。
        // 小于200M不上传
        if (fileSize > 1024 * 1024 * 200) {
            uploadService.asyncUpload(part);
        } else {
//            historyPartRepository.delete(part);
        }

    }
}
