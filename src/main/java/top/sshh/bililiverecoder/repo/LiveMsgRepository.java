package top.sshh.bililiverecoder.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.LiveMsg;

@Repository
public interface LiveMsgRepository extends CrudRepository<LiveMsg, Long> {

}
