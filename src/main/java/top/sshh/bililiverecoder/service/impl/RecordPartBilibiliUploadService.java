package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.RecordHistoryPart;
import top.sshh.bililiverecoder.entity.RecordRoom;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.repo.RecordHistoryPartRepository;
import top.sshh.bililiverecoder.repo.RecordRoomRepository;
import top.sshh.bililiverecoder.service.RecordPartUploadService;

import java.util.Optional;

@Slf4j
@Component
public class RecordPartBilibiliUploadService implements RecordPartUploadService {

    @Autowired
    private BiliUserRepository biliUserRepository;
    @Autowired
    private RecordHistoryPartRepository partRepository;
    @Autowired
    private RecordRoomRepository roomRepository;

    @Override
    public void upload(RecordHistoryPart part) {
        RecordRoom room = roomRepository.findByRoomId(part.getRoomId());
        if(room != null){
            if(room.isUpload()){
                if(room.getUploadUserId() == null){
                    log.info("分片上传事件，没有设置上传用户，无法上传 ==>{}", JSON.toJSONString(room));
                    return;
                }else {
                    Optional<BiliBiliUser> userOptional = biliUserRepository.findById(room.getUploadUserId());
                    if(!userOptional.isPresent()){
                        log.error("分片上传事件，上传用户不存在，无法上传 ==>{}", JSON.toJSONString(room));
                        return;
                    }
                    BiliBiliUser biliBiliUser = userOptional.get();
                    if(!biliBiliUser.isLogin()){
                        log.error("分片上传事件，用户登录状态失效，无法上传，请重新登录 ==>{}", JSON.toJSONString(room));
                        return;
                    }



                }
            }else {
                log.info("分片上传事件，文件不需要上传 ==>{}",JSON.toJSONString(part));
                return;
            }
        }

    }
}
