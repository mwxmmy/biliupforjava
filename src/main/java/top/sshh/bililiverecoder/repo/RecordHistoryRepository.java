package top.sshh.bililiverecoder.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.RecordHistory;

@Repository
public interface RecordHistoryRepository extends CrudRepository<RecordHistory, Long> {

    RecordHistory findBySessionId(String sessionId);
}
