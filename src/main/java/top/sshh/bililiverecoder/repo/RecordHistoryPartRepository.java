package top.sshh.bililiverecoder.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;

import java.util.List;

@Repository
public interface RecordHistoryPartRepository extends CrudRepository<RecordHistoryPart, Long> {

    RecordHistoryPart findByFilePath(String path);

    RecordHistoryPart findByHistoryIdAndTitle(Long historyId, String title);

    List<RecordHistoryPart> findByHistoryId(Long historyId);

    List<RecordHistoryPart> findByHistoryIdAndCidIsNotNullOrderByPageAsc(Long historyId);

    int countByHistoryIdAndRecordingIsTrue(Long historyId);

    int countByHistoryId(Long historyId);

    boolean existsByFilePath(String filePath);
}
