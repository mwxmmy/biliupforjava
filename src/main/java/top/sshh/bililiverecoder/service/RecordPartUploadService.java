package top.sshh.bililiverecoder.service;

import org.springframework.scheduling.annotation.Async;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;

public interface RecordPartUploadService {

    @Async("myAsyncPool")
    void asyncUpload(RecordHistoryPart part);

    void upload(RecordHistoryPart part);
}
