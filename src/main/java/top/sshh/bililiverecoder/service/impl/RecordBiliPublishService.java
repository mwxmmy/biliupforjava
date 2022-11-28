package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.entity.data.SingleVideoDto;
import top.sshh.bililiverecoder.entity.data.VideoUploadDto;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.RecordPartUploadService;
import top.sshh.bililiverecoder.util.BiliApi;
import top.sshh.bililiverecoder.util.TaskUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class RecordBiliPublishService {

    @Autowired
    private BiliUserRepository biliUserRepository;
    @Autowired
    private RecordHistoryPartRepository partRepository;
    @Autowired
    private RecordHistoryRepository historyRepository;
    @Autowired
    private RecordRoomRepository roomRepository;
    @Autowired
    private RecordPartUploadService uploadService;

    public boolean publishRecordHistory(RecordHistory history) {
        if (history.isRecording()) {
            return false;
        }

        Thread publishThread = TaskUtil.publishTask.get(history.getId());
        if (publishThread != null) {
            //正在发布，直接退出
            return false;
        }

        RecordRoom room = roomRepository.findByRoomId(history.getRoomId());
        log.info("发布视频事件开始：{}", room.getUname());

        if (room.getTid() == null) {
            //没有设置分区，直接取消上传
            return false;
        }
        List<RecordHistoryPart> uploadParts = partRepository.findByHistoryId(history.getId());
        for (RecordHistoryPart uploadPart : uploadParts) {
            //已经上传完成就跳过
            if (uploadPart.isUpload()) {
                continue;
            }
            Thread thread = TaskUtil.partUploadTask.get(uploadPart.getId());
            if (thread != null && thread != Thread.currentThread()) {
                boolean alive = thread.isAlive();
                if (alive) {
                    try {
                        //等待线程上传完成
                        log.info("partId={},{} ===>正在上传 ，等待上传完成在发布", uploadPart.getId(), uploadPart.getFilePath());
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                TaskUtil.partUploadTask.remove(uploadPart.getId());
            }
            //再次检查是否上传完成
            Optional<RecordHistoryPart> partOptional = partRepository.findById(uploadPart.getId());
            if (partOptional.isPresent()) {
                RecordHistoryPart part = partOptional.get();
                if (!part.isUpload()) {
                    uploadService.upload(uploadPart);
                }
            }
        }
        //重新加载上传列表
        uploadParts = partRepository.findByHistoryId(history.getId());
        long count = uploadParts.stream().filter(RecordHistoryPart::isUpload).count();
        if (count != uploadParts.size()) {
            //没有全部上传完成返回失败
            return false;
        }
        if (room.isUpload()) {
            if (room.getUploadUserId() == null) {
                log.info("视频发布事件，没有设置上传用户，无法发布 ==>{}", JSON.toJSONString(room));
                return false;
            } else {
                Optional<BiliBiliUser> userOptional = biliUserRepository.findById(room.getUploadUserId());
                if (!userOptional.isPresent()) {
                    log.error("视频发布事件，用户不存在，无法发布 ==>{}", JSON.toJSONString(room));
                    return false;
                }
                BiliBiliUser biliBiliUser = userOptional.get();
                if (!biliBiliUser.isLogin()) {
                    log.error("视频发布事件，用户登录状态失效，无法发布，请重新登录 ==>{}", JSON.toJSONString(room));
                    return false;
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
                    biliUserRepository.save(biliBiliUser);
                    throw new RuntimeException("{}登录已过期，请重新登录! " + biliBiliUser.getUname());
                }


                // 发布任务入队列
                TaskUtil.publishTask.put(history.getId(), Thread.currentThread());


                Map<String, Object> map = new HashMap<>();
                LocalDateTime startTime = history.getStartTime();
                map.put("date", startTime);

                String uname = room.getUname();
                map.put("${uname}", uname);
                String title = StringUtils.isNotBlank(history.getTitle()) ? history.getTitle() : "直播录像";
                map.put("${title}", title);
                map.put("${roomId}", room.getRoomId());
                List<SingleVideoDto> dtos = new ArrayList<>();
                for (RecordHistoryPart uploadPart : uploadParts) {
                    SingleVideoDto dto = new SingleVideoDto();
                    map.put("date", uploadPart.getStartTime());
                    dto.setTitle(this.template(room.getPartTitleTemplate(), map));
                    dto.setDesc("");
                    dto.setFilename(uploadPart.getFileName());
                    dtos.add(dto);
                }
                VideoUploadDto videoUploadDto = new VideoUploadDto();

                map.put("date", startTime);
                videoUploadDto.setTid(room.getTid());
                videoUploadDto.setTitle(this.template(room.getTitleTemplate(), map));
                videoUploadDto.setDesc(this.template(room.getDescTemplate(), map));
                videoUploadDto.setDynamic(this.template(room.getDescTemplate(), map));
                videoUploadDto.setVideos(dtos);
                videoUploadDto.setTag(room.getTags());
                try {
                    String uploadRes = BiliApi.publish(biliBiliUser.getAccessToken(), videoUploadDto);
                    String bvid = JSON.parseObject(uploadRes).getJSONObject("data").getString("bvid");
                    history.setBvId(bvid);
                    historyRepository.save(history);
                    log.info("发布={}=视频成功 == > {}", room.getUname(), JSON.toJSONString(history));
                } catch (Exception e) {
                    log.info("发布={}=视频失败 == > {}", room.getUname(), JSON.toJSONString(history), e);
                } finally {
                    TaskUtil.publishTask.remove(history.getId());
                }
            }
        }
        return true;
    }

    private String template(String template, Map<String, Object> map) {
        template = template.replace("${uname}", map.get("${uname}").toString())
                .replace("${title}", map.get("${title}").toString())
                .replace("${roomId}", map.get("${roomId}").toString());
        if (template.contains("${")) {
            LocalDateTime localDateTime = (LocalDateTime) map.get("date");
            String date = template.substring(template.indexOf("${"), template.indexOf("}") + 1);
            String format = localDateTime.format(DateTimeFormatter.ofPattern(date.substring(2, date.length() - 1)));
            template = template.replace(date, format);
        }
        return template;
    }
}
