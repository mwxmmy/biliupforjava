package top.sshh.bililiverecoder.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.RecordHistory;

import java.util.List;

@Repository
public interface RecordHistoryRepository extends CrudRepository<RecordHistory, Long> {

    RecordHistory findBySessionId(String sessionId);

    List<RecordHistory> findByRoomIdAndRecordingAndUploadAndUploadRetryCountLessThan(Long roomId, boolean record, boolean upload, int count);

    List<RecordHistory> findByBvIdNotNullAndPublish(boolean publish);
}
