package top.sshh.bililiverecoder.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.BiliBiliUser;

@Repository
public interface BiliUserRepository extends CrudRepository<BiliBiliUser, Long> {

    BiliBiliUser findByUid(long uid);
}
