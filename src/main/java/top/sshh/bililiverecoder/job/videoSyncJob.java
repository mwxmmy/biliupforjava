package top.sshh.bililiverecoder.job;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.RecordHistory;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.entity.data.BiliVideoInfoResponse;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.impl.LiveMsgService;
import top.sshh.bililiverecoder.service.impl.RecordBiliPublishService;
import top.sshh.bililiverecoder.util.BiliApi;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class videoSyncJob {

    @Autowired
    RecordBiliPublishService publishService;

    @Autowired
    RecordRoomRepository roomRepository;

    @Autowired
    RecordHistoryRepository historyRepository;

    @Autowired
    RecordHistoryPartRepository partRepository;

    @Autowired
    private LiveMsgService liveMsgService;


    // 定时查询直播历史，每十分钟验证一下是否发布成功
    @Scheduled(fixedDelay = 600000, initialDelay = 5000)
    public void syncVideo() {
        //查询出所有需要同步的录播记录
        log.info("同步视频分p cid 开始");
        for (RecordHistory next : historyRepository.findByBvIdNotNullAndPublishIsTrueAndCodeLessThan(0)) {
            BiliVideoInfoResponse videoInfoResponse = BiliApi.getVideoInfo(next.getBvId());
            next.setCode(videoInfoResponse.getCode());
            if (videoInfoResponse.getData() != null) {
                next.setAvId(videoInfoResponse.getData().getAid());
                next.setBvId(videoInfoResponse.getData().getBvid());
            }
            next = historyRepository.save(next);
            if (videoInfoResponse.getCode() == 0) {
                RecordRoom recordRoom = roomRepository.findByRoomId(next.getRoomId());
                List<BiliVideoInfoResponse.BiliVideoInfoPart> pages = videoInfoResponse.getData().getPages();
                for (BiliVideoInfoResponse.BiliVideoInfoPart page : pages) {
                    RecordHistoryPart part = partRepository.findByHistoryIdAndTitle(next.getId(), page.getPart());
                    if (part != null) {
                        part.setCid(page.getCid());
                        part.setPage(page.getPage());
                        part.setDuration(page.getDuration());
                        part = partRepository.save(part);

                        //如果配置成发布完成后删除则删除文件
                        if(recordRoom != null && recordRoom.getDeleteType() == 2){
                            File file = new File(part.getFilePath());
                            boolean delete = file.delete();
                            if(delete){
                                log.error("{}=>文件删除成功！！！", part.getFilePath());
                            }else {
                                log.error("{}=>文件删除失败！！！", part.getFilePath());
                            }
                        }
                        //解析弹幕入库
                        liveMsgService.processing(part);
                        log.info("同步视频分p 成功==>{}", JSON.toJSONString(part));
                    }
                }

            }
        }

    }
}
