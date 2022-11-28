package top.sshh.bililiverecoder.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordEventDTO;
import top.sshh.bililiverecoder.entity.RecordEventData;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.service.RecordEventService;
import top.sshh.bililiverecoder.service.RecordPartUploadService;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class RecordEventFileClosedService implements RecordEventService {

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
        // 正常逻辑
        Optional<RecordHistoryPart> partOptional = historyPartRepository.findById(eventData.getRelativePath());
        if(!partOptional.isPresent()){
            return;
        }

        RecordHistoryPart part = partOptional.get();
        part.setRecording(false);
        part.setFileSize(eventData.getFileSize());
        part.setEndTime(LocalDateTime.now());
        part.setUpdateTime(LocalDateTime.now());
        historyPartRepository.save(part);
        Optional<RecordHistory> historyOptional = historyRepository.findById(part.getHistoryId());
        if(historyOptional.isPresent()){
            RecordHistory history = historyOptional.get();
            history.setFileSize(history.getFileSize()+ part.getFileSize());
            history.setUpdateTime(LocalDateTime.now());
            historyRepository.save(history);
        }
        // 文件上传操作
        //开始上传该视频分片，异步上传任务。
        uploadService.upload(part);

        //TODO 解析弹幕入库
    }
}
