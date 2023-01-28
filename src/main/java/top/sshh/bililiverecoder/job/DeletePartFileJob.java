package top.sshh.bililiverecoder.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DeletePartFileJob {


    @Autowired
    RecordRoomRepository roomRepository;

    @Autowired
    RecordHistoryPartRepository partRepository;

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void sndMsgProcess() {
        List<RecordRoom> roomList = roomRepository.findByDeleteType(3);
        for (RecordRoom room : roomList) {
            LocalDateTime deleteTime = LocalDateTime.now().minusDays(room.getDeleteDay());
            List<RecordHistoryPart> partList = partRepository.findByRoomIdAndFileDeleteIsFalseAndEndTimeIsBefore(room.getRoomId(),deleteTime);
            if (partList.size() > 0) {
                log.info("定时删除文件任务，主播名称{}，待删除文件数量{}", room.getUname(), partList.size());
            }
            for (RecordHistoryPart part : partList) {
                File file = new File(part.getFilePath());
                boolean delete = file.delete();
                if (delete) {
                    log.error("定时删除文件任务，主播名称{}，{}=>文件删除成功！！！", room.getUname(), part.getFilePath());
                } else {
                    log.error("定时删除文件任务，主播名称{}，{}=>文件删除失败！！！", room.getUname(), part.getFilePath());
                }
                part.setFileDelete(true);
                partRepository.save(part);
            }
        }
    }
}
