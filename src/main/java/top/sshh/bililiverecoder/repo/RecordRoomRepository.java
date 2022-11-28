package top.sshh.bililiverecoder.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.RecordRoom;

@Repository
public interface RecordRoomRepository extends CrudRepository<RecordRoom, Long> {

    RecordRoom findByRoomId(String roomId);
}
