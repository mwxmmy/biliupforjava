package top.sshh.bililiverecoder.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.impl.RecordBiliPublishService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
public class publishJob {

    @Autowired
    RecordBiliPublishService publishService;

    @Autowired
    RecordRoomRepository roomRepository;

    @Autowired
    RecordHistoryRepository historyRepository;

    @Autowired
    RecordHistoryPartRepository partRepository;


    // 定时查询直播历史，如果下一次直播开始时间和上一次结束时间小于5min，视为同一次直播
    @Scheduled(fixedDelay = 60000, initialDelay = 5000)
    public void publish() {
        log.info("视频上传定时任务开始");
        //查询出所有需要上传的房间
        List<RecordRoom> roomList = roomRepository.findByUpload(true);

        List<RecordHistory> historyList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (RecordRoom room : roomList) {
            // 查询不在录制,下播十分钟后的需要上传的历史
            Iterator<RecordHistory> iterator = historyRepository.findByRoomIdAndRecordingIsFalseAndUploadIsTrueAndPublishIsFalseAndUploadRetryCountLessThanAndEndTimeBetweenOrderByEndTimeAsc(room.getRoomId(), 5, now.minusMonths(1L), now.minusMinutes(11L)).iterator();
            iterator.forEachRemaining(historyList::add);
        }
        log.info("视频发布定时任务 待发布视频数量 size=={}", historyList.size());

        for (RecordHistory history : historyList) {
            publishService.publishRecordHistory(history);
        }
    }
}
