package top.sshh.bililiverecoder.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import top.sshh.bililiverecoder.entity.LiveMsg;

import java.util.List;

@Repository
public interface LiveMsgRepository extends CrudRepository<LiveMsg, Long> {

    List<LiveMsg> findByPartIdAndCode(Long partId, int code);
    List<LiveMsg> findByPoolAndCodeAndPartIdInOrderBySendTimeAsc(int pool, int code, List<Long> partIds);
    Page<LiveMsg> findByPoolAndCodeAndPartIdInOrderBySendTimeAsc(int pool, int code, List<Long> partIds, Pageable page);

    /**
     * 查询字幕池弹幕
     * @param partId
     * @param pool
     * @return
     */
    List<LiveMsg> findByPartIdAndPoolAndCidNotNullOrderBySendTimeAsc(Long partId, int pool);

    int countByPartId(Long partId);

    int countByBvid(String bvId);

    int countByBvidAndCode(String bvId, Integer code);

    void deleteByPartId(Long partId);

    List<LiveMsg> queryByBvid(String bvid);

    List<LiveMsg> queryByCid(Long cid);

    List<LiveMsg> queryByPartId(Long partId);

    @Query(value = "SELECT " +
            "   FLOOR(send_time / 60000) AS time, " +
            "   COUNT(*) AS num " +
            "FROM live_msg " +
            "WHERE part_id = ?1 " +
            "GROUP BY FLOOR(send_time / 60000)",
            nativeQuery = true)
    List<Object[]> getMsgCountByMinute(Long partId);

    @Query(value = "SELECT " +
            "   FLOOR(send_time / 1000) AS time, " +
            "   COUNT(*) AS num " +
            "FROM live_msg " +
            "WHERE part_id = ?1 " +
            "GROUP BY FLOOR(send_time / 1000)",
            nativeQuery = true)
    List<Object[]> getMsgCountBySecond(Long partId);
}
