package top.sshh.bililiverecoder.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JdbcService {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public <T> void saveLiveMsgList(List<T> list) {
        // 这里注意VALUES要用实体的变量，而不是字段的Column值
        String sql = "INSERT INTO live_msg(bvid, cid, code, context, is_send, part_id, send_time, color, fontsize, mode, pool) " +
                "VALUES (:bvid, :cid,  :code, :context, false, :partId, :sendTime, :color, :fontsize, :mode, :pool)";
        updateBatchCore(sql, list);
    }

    /**
     * 一定要在jdbc url 加&rewriteBatchedStatements=true才能生效
     *
     * @param sql  自定义sql语句，类似于 "INSERT INTO chen_user(name,age) VALUES (:name,:age)"
     * @param list
     * @param <T>
     */
    public <T> void updateBatchCore(String sql, List<T> list) {
        SqlParameterSource[] beanSources = SqlParameterSourceUtils.createBatch(list.toArray());
        namedParameterJdbcTemplate.batchUpdate(sql, beanSources);
    }

}
