package top.sshh.bililiverecoder.service;

import org.springframework.scheduling.annotation.Async;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;

public interface RecordPartUploadService {

    @Async
    void upload(RecordHistoryPart part);
}
