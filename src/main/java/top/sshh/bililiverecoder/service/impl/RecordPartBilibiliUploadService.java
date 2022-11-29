package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.RecordPartUploadService;
import top.sshh.bililiverecoder.util.BiliApi;
import top.sshh.bililiverecoder.util.TaskUtil;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class RecordPartBilibiliUploadService implements RecordPartUploadService {

    @Autowired
    private BiliUserRepository biliUserRepository;
    @Autowired
    private RecordHistoryPartRepository partRepository;
    @Autowired
    private RecordHistoryRepository historyRepository;
    @Autowired
    private RecordRoomRepository roomRepository;

    public void asyncUpload(RecordHistoryPart part) {
        log.info("partId={},异步上传任务开始==>{}", part.getId(), part.getFilePath());
        this.upload(part);
    }

    @Override
    public void upload(RecordHistoryPart part) {
        Thread thread = TaskUtil.partUploadTask.get(part.getId());
        if (thread != null && thread != Thread.currentThread()) {
            log.info("当前线程为{} ,partId={}该文件正在被{}线程上传", Thread.currentThread(), part.getId(), thread.getName());
            return;
        }

        RecordRoom room = roomRepository.findByRoomId(part.getRoomId());
        if (room != null) {
            if (room.getTid() == null) {
                //没有设置分区，直接取消上传
                return;
            }

            Optional<RecordHistory> historyOptional = historyRepository.findById(part.getHistoryId());
            if (!historyOptional.isPresent()) {
                log.error("分片上传失败，history不存在==>{}", JSON.toJSONString(part));
                return;
            }
            RecordHistory history = historyOptional.get();
            if (history.isUpload()) {
                if (room.getUploadUserId() == null) {
                    log.info("分片上传事件，没有设置上传用户，无法上传 ==>{}", JSON.toJSONString(room));
                    return;
                } else {
                    Optional<BiliBiliUser> userOptional = biliUserRepository.findById(room.getUploadUserId());
                    if (!userOptional.isPresent()) {
                        log.error("分片上传事件，上传用户不存在，无法上传 ==>{}", JSON.toJSONString(room));
                        return;
                    }
                    BiliBiliUser biliBiliUser = userOptional.get();
                    if (!biliBiliUser.isLogin()) {
                        log.error("分片上传事件，用户登录状态失效，无法上传，请重新登录 ==>{}", JSON.toJSONString(room));
                        return;
                    }
                    boolean expired = false;
                    // 检查是否已经过期，调用用户信息接口
                    try {
                        String myInfo = BiliApi.appMyInfo(biliBiliUser.getAccessToken());
                        String uname = JsonPath.read(myInfo, "data.name");
                        if (StringUtils.isBlank(uname)) {
                            expired = true;
                        }
                    } catch (Exception e) {
                        expired = true;
                    }
                    if (expired) {
                        biliBiliUser.setLogin(false);
                        biliBiliUser = biliUserRepository.save(biliBiliUser);
                        throw new RuntimeException("{}登录已过期，请重新登录! " + biliBiliUser.getUname());
                    }


                    // 上传任务入队列
                    TaskUtil.partUploadTask.put(part.getId(), Thread.currentThread());
                    // 登录验证结束
                    String preRes = BiliApi.preUpload(biliBiliUser.getAccessToken(), biliBiliUser.getUid(), "ugcfr/pc3");
                    JSONObject preResObj = JSON.parseObject(preRes);
                    String url = preResObj.getString("url");
                    String complete = preResObj.getString("complete");
                    String filename = preResObj.getString("filename");
                    // 分段上传
                    String filePath = part.getFilePath();
                    File uploadFile = new File(filePath);
                    long fileSize = uploadFile.length();
                    long chunkSize = 1024 * 1024 * 5;
                    long chunkNum = (long) Math.ceil((double) fileSize / chunkSize);
                    MessageDigest md5Digest = DigestUtils.getMd5Digest();
                    try (RandomAccessFile r = new RandomAccessFile(filePath, "r")) {
                        for (int i = 0; i < chunkNum; i++) {
                            int tryCount = 0;
                            Exception toThrow = null;
                            while (tryCount < 5) {
                                try {
                                    r.seek(i * chunkSize);
                                    byte[] bytes = new byte[(int) chunkSize];
                                    int read = r.read(bytes);
                                    if (read == -1) {
                                        break;
                                    }
                                    if (read != bytes.length)
                                        bytes = ArrayUtils.subarray(bytes, 0, read);
                                    md5Digest.update(bytes);
                                    String s = BiliApi.uploadChunk(url, filename, bytes, read,
                                            i + 1, (int) chunkNum);
                                    if (!s.contains("OK")) {
                                        throw new RuntimeException("上传返回异常");
                                    }
                                    log.info("[{}] 上传视频part {} 进度{}/{}, resp={}", room.getTitle(),
                                            filePath, i + 1, chunkNum, s);
                                    tryCount = 5;
                                    toThrow = null;
                                } catch (Exception e) {
                                    log.info("[{}] 上传视频part {} 进度{}/{}, exception={}", room.getTitle(),
                                            filePath, i + 1, chunkNum, ExceptionUtils.getStackTrace(e));
                                    toThrow = e;
                                }
                            }
                            if (toThrow != null) {
                                throw toThrow;
                            }

                        }
                    } catch (Exception e) {
                        TaskUtil.partUploadTask.remove(part.getId());
                        part.setUpload(false);
                        part = partRepository.save(part);
                        historyOptional = historyRepository.findById(history.getId());
                        if (historyOptional.isPresent()) {
                            history = historyOptional.get();
                            history.setUploadRetryCount(history.getUploadRetryCount() + 1);
                            history = historyRepository.save(history);
                        }
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                    String md5 = DatatypeConverter.printHexBinary(md5Digest.digest()).toLowerCase();
                    BiliApi.completeUpload(complete, (int) chunkNum, fileSize, md5,
                            uploadFile.getName(), "2.0.0.1054");
                    part.setFileName(filename);
                    part.setUpload(true);
                    part.setUpdateTime(LocalDateTime.now());
                    part = partRepository.save(part);
                    //如果配置上传删除，则删除文件
                    if (room.isDeleteFile()) {
                        uploadFile.deleteOnExit();
                    }
                    TaskUtil.partUploadTask.remove(part.getId());
                    log.info("partId={},文件上传成功==>{}", part.getId(), part.getFilePath());
                }
            }else {
                log.info("分片上传事件，文件不需要上传 ==>{}",JSON.toJSONString(part));
                return;
            }
        }

    }
}
