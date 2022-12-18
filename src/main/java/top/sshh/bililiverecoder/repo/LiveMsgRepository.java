package top.sshh.bililiverecoder.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.LiveMsg;

import java.util.List;

@Repository
public interface LiveMsgRepository extends CrudRepository<LiveMsg, Long> {

    List<LiveMsg> findByPartIdAndCode(Long partId, int code);

    int countByPartId(Long partId);

    int countByBvid(String bvId);

    int countByBvidAndCode(String bvId, Integer code);

    void deleteByPartId(Long partId);

    List<LiveMsg> queryByBvid(String bvid);
}
