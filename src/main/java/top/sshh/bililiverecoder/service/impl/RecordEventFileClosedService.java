package top.sshh.bililiverecoder.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
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

    @Autowired
    private LiveMsgService liveMsgService;


    @Override
    public void processing(RecordEventDTO event) {
        RecordEventData eventData = event.getEventData();
        String sessionId = eventData.getSessionId();
        String relativePath = eventData.getRelativePath();
        log.info("分p录制结束事件==>{}", relativePath);
        RecordRoom room = roomRepository.findByRoomId(eventData.getRoomId());
        Optional<RecordHistory> historyOptional = historyRepository.findById(room.getHistoryId());
        if ("blrec".equals(sessionId)) {
            relativePath = relativePath.replace(workPath, "");
        }
        String filePath = workPath + File.separator + relativePath;
        if (historyOptional.isPresent()) {
            RecordHistory history = historyOptional.get();
            // 正常逻辑
            RecordHistoryPart part = historyPartRepository.findByFilePath(filePath);
            if (part == null) {
                log.info("文件分片不存在==>{}", relativePath);
                part = new RecordHistoryPart();
                part.setStartTime(LocalDateTime.now().minusSeconds((long) eventData.getDuration()));
                part.setEventId(event.getEventId());
                part.setTitle(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM月dd日HH点mm分ss秒")));
                part.setAreaName(eventData.getAreaNameChild());
                part.setRoomId(history.getRoomId());
                part.setHistoryId(history.getId());
                part.setFilePath(filePath);
                part.setFileSize(0L);
                part.setSessionId(sessionId);
                part.setRecording(eventData.isRecording());
                part.setStartTime(LocalDateTime.now());
                part.setEndTime(LocalDateTime.now());
            }
            File vidleFile = new File(filePath);
            long fileSize = 0;
            if (vidleFile.exists()) {
                fileSize = vidleFile.length();
            } else {
                log.error("文件{}不存在，请考虑工作目录是否设置正确，或者docker 卷是否映射正确", filePath);
                fileSize = eventData.getFileSize();
            }
            LocalDateTime startTime = part.getStartTime();
            Duration duration = Duration.between(startTime, LocalDateTime.now());
            part.setRecording(false);
            part.setFileSize(fileSize);
            part.setDuration(eventData.getDuration() != 0.0f ? (int)eventData.getDuration() : duration.getSeconds());
            part.setEndTime(LocalDateTime.now());
            part.setAreaName(eventData.getAreaNameChild());
            part.setUpdateTime(LocalDateTime.now());
            part = historyPartRepository.save(part);

            history.setFileSize(history.getFileSize() + part.getFileSize());
            history.setTitle(eventData.getTitle());
            history.setSessionId(sessionId);
            history.setRecording(eventData.isRecording());
            history.setStreaming(eventData.isStreaming());
            history.setUpdateTime(LocalDateTime.now());
            history.setEndTime(LocalDateTime.now());
            history = historyRepository.save(history);
            if (!vidleFile.exists()) {
                return;
            }
            if (StringUtils.isNotBlank(room.getMoveDir()) && (room.getDeleteType() == 6 || room.getDeleteType() == 7)) {
                String fileName = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
                String startDirPath = filePath.substring(0, filePath.lastIndexOf('/') + 1);
                String toDirPath = room.getMoveDir() + filePath.substring(0, filePath.lastIndexOf('/') + 1).replace(workPath, "");
                File toDir = new File(toDirPath);
                if (!toDir.exists()) {
                    toDir.mkdirs();
                }
                File startDir = new File(startDirPath);
                File[] files = startDir.listFiles((file, s) -> s.startsWith(fileName));
                if (files != null) {
                    for (File file : files) {
                        if(! filePath.startsWith(workPath)){
                            part.setFileDelete(true);
                            part = historyPartRepository.save(part);
                            continue;
                        }
                        if(room.getDeleteType() == 6){
                            try {
                                Files.move(Paths.get(file.getPath()), Paths.get(toDirPath + file.getName()),
                                        StandardCopyOption.REPLACE_EXISTING);
                                log.error("{}=>文件移动成功！！！", file.getName());
                            } catch (Exception e) {
                                log.error("{}=>文件移动失败！！！", file.getName());
                            }
                        }else if(room.getDeleteType() == 7){
                            try {
                                Files.copy(Paths.get(file.getPath()), Paths.get(toDirPath + file.getName()),
                                        StandardCopyOption.REPLACE_EXISTING);
                                log.error("{}=>文件复制成功！！！", file.getName());
                            } catch (Exception e) {
                                log.error("{}=>文件复制失败！！！", file.getName());
                            }
                        }

                    }
                }

                
                part.setFilePath(toDirPath + filePath.substring(filePath.lastIndexOf("/") + 1));
                part.setFileDelete(true);
                part = historyPartRepository.save(part);
            }
            // 文件上传操作
            //开始上传该视频分片，异步上传任务。
            // 小于设定文件大小和时长不上传
            if (fileSize > 1024 * 1024 * room.getFileSizeLimit() && part.getDuration() > room.getDurationLimit()) {
                uploadServiceFactory.getUploadService(room.getLine()).asyncUpload(part);
            } else {
                log.error("文件大小{}小于{},或时长{}小于{}，删除。", fileSize, room.getFileSizeLimit(), part.getDuration(), room.getDurationLimit());
                historyPartRepository.delete(part);
                return;
            }
        } else {
            log.error("分p录制结束事件，录制历史不存在。");
            RecordHistoryPart part = new RecordHistoryPart();
            part.setStartTime(LocalDateTime.now().minusSeconds((long) eventData.getDuration()));
            part.setEventId(event.getEventId());
            part.setTitle(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM月dd日HH点mm分ss秒")));
            part.setAreaName(eventData.getAreaNameChild());
            part.setRoomId(eventData.getRoomId());
            part.setFilePath(filePath);
            part.setFileSize(0L);
            part.setSessionId(sessionId);
            part.setRecording(eventData.isRecording());
            part.setStartTime(LocalDateTime.now());
            part.setEndTime(LocalDateTime.now());
            historyPartRepository.save(part);
        }

    }
}
