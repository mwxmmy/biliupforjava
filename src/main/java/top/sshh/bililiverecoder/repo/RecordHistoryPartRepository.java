package top.sshh.bililiverecoder.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecordHistoryPartRepository extends CrudRepository<RecordHistoryPart, Long> {

    RecordHistoryPart findByFilePath(String path);

    RecordHistoryPart findByHistoryIdAndTitle(Long historyId, String title);

    List<RecordHistoryPart> findByHistoryIdOrderByStartTimeAsc(Long historyId);

    List<RecordHistoryPart> findByIdIn(List<Long> ids);

    List<RecordHistoryPart> findByRoomIdAndFileDeleteIsFalseAndEndTimeIsBefore(String roomId, LocalDateTime deleteTime);

    List<RecordHistoryPart> findByHistoryIdAndCidIsNotNullOrderByPageAsc(Long historyId);

    int countByHistoryIdAndRecordingIsTrue(Long historyId);

    int countByHistoryId(Long historyId);

    int countByHistoryIdAndFileNameNotNull(Long historyId);

    @Query("select ifnull(sum(duration),0) from RecordHistoryPart where historyId = ?1")
    float sumHistoryDurationByHistoryId(Long historyId);

    boolean existsByFilePath(String filePath);
}
