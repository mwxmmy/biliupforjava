package top.sshh.bililiverecoder.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;

@Repository
public interface RecordHistoryPartRepository extends CrudRepository<RecordHistoryPart, String> {

    RecordHistoryPart findByFilePath(String path);
}
