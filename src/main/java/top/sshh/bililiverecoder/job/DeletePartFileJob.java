package top.sshh.bililiverecoder.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DeletePartFileJob {


    @Value("${record.work-path}")
    private String workPath;

    @Autowired
    RecordRoomRepository roomRepository;

    @Autowired
    RecordHistoryPartRepository partRepository;

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void deleteFileProcess() {
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
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void moveFileProcess() {
        List<RecordRoom> roomList = roomRepository.findByDeleteType(8);
        for (RecordRoom room : roomList) {
            LocalDateTime deleteTime = LocalDateTime.now().minusDays(room.getDeleteDay());
            List<RecordHistoryPart> partList = partRepository.findByRoomIdAndFileDeleteIsFalseAndEndTimeIsBefore(room.getRoomId(),deleteTime);
            if (partList.size() > 0) {
                log.info("定时移动文件任务，主播名称{}，待移动文件数量{}", room.getUname(), partList.size());
            }
            for (RecordHistoryPart part : partList) {
                String filePath = part.getFilePath();
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                String startDirPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
                String toDirPath = room.getMoveDir() + filePath.substring(0, filePath.lastIndexOf('/') + 1).replace(workPath, "");
                File toDir = new File(toDirPath);
                if (!toDir.exists()) {
                    toDir.mkdirs();
                }
                File startDir = new File(startDirPath);
                File[] files = startDir.listFiles((file, s) -> s.startsWith(fileName));
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        if(! filePath.startsWith(workPath)){
                            part.setFileDelete(true);
                            part = partRepository.save(part);
                            continue;
                        }
                        if(room.getDeleteType() == 8){
                            try {
                                Files.move(Paths.get(file.getPath()), Paths.get(toDirPath + file.getName()),
                                        StandardCopyOption.REPLACE_EXISTING);
                                log.error("{}=>文件移动成功！！！", file.getName());
                            } catch (Exception e) {
                                log.error("{}=>文件移动失败！！！", file.getName());
                            }
                        }

                    }
                }
                
                part.setFilePath(toDirPath + filePath.substring(filePath.lastIndexOf("/") + 1));
                part.setFileDelete(true);
                part = partRepository.save(part);
            }
        }
    }
}
