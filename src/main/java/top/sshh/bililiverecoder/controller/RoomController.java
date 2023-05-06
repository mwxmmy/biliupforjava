package top.sshh.bililiverecoder.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.sshh.bililiverecoder.entity.*;
import top.sshh.bililiverecoder.repo.*;
import top.sshh.bililiverecoder.util.BiliApi;
import top.sshh.bililiverecoder.util.UploadEnums;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/room")
public class RoomController {
    @Autowired
    private RecordRoomRepository roomRepository;

    @Autowired
    private BiliUserRepository userRepository;

    @Autowired
    private RecordHistoryRepository historyRepository;

    @Autowired
    private RecordHistoryPartRepository partRepository;


    @PostMapping
    public List<RecordRoom> list() {
        Iterator<RecordRoom> roomIterator = roomRepository.findAll().iterator();
        List<RecordRoom> list = new ArrayList<>();
        roomIterator.forEachRemaining(list::add);
        return list;
    }


    @PostMapping("/exportConfig")
    public void exportConfig(@RequestBody ExportConfigParams params, HttpServletResponse response) throws IOException {
        Map<String,Object> map = new HashMap<>();
        if(params.isExportRoom()){
            List<RecordRoom> roomList = this.list();
            map.put("roomList",roomList);
        }
        if(params.isExportUser()){
            List<BiliBiliUser> userList = new ArrayList<>();
            Iterator<BiliBiliUser> userIterator = userRepository.findAll().iterator();
            userIterator.forEachRemaining(userList::add);
            map.put("userList",userList);
        }
        if(params.isExportHistory()){
            List<RecordHistory> historyList = new ArrayList<>();
            Iterator<RecordHistory> historyIterator = historyRepository.findAll().iterator();
            historyIterator.forEachRemaining(historyList::add);
            map.put("historyList",historyList);
            List<RecordHistoryPart> partList = new ArrayList<>();
            Iterator<RecordHistoryPart> partIterator = partRepository.findAll().iterator();
            partIterator.forEachRemaining(partList::add);
            map.put("partList",partList);
        }
        String jsonString = JSON.toJSONString(map);
        String timeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH点mm分"));
        // 构造响应头，指定文件名，并将文件名进行URL编码
        String encodedFilename = URLEncoder.encode("biliupForJavaConfig_"+timeString+".json", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "attachment; filename="+encodedFilename);
        // 将JSON字符串写入到响应输出流中
        OutputStream out = response.getOutputStream();
        out.write(jsonString.getBytes());
        out.flush();
        out.close();
    }

    @PostMapping("/uploadConfig")
    public void uploadConfig(@RequestParam("file") MultipartFile file) throws IOException {
        // 获取上传的文件内容
        byte[] bytes = file.getBytes();
        // 将文件内容转换为JSON字符串
        String json = new String(bytes);

        // 将JSON字符串转换为Map对象
        Map<String,Object> configMap = JSON.parseObject(json, new TypeReference<>() {
        });
        List<RecordRoom> roomList = JSON.parseObject(JSON.toJSONString(configMap.get("roomList")), new TypeReference<>() {});
        List<BiliBiliUser> userList = JSON.parseObject(JSON.toJSONString(configMap.get("userList")), new TypeReference<>() {});
        List<RecordHistory> historyList = JSON.parseObject(JSON.toJSONString(configMap.get("historyList")), new TypeReference<>() {});
        List<RecordHistoryPart> partList = JSON.parseObject(JSON.toJSONString(configMap.get("partList")), new TypeReference<>() {});


        Map<Long,Long> userIdConverMap = new HashMap<>();
        if(userList != null && userList.size()>0){
            for (BiliBiliUser user : userList) {
                Long id = user.getId();
                user.setId(null);
                BiliBiliUser dbUser = userRepository.findByUid(user.getUid());
                if(dbUser != null){
                    user.setId(dbUser.getId());
                }
                userRepository.save(user);
                userIdConverMap.put(id,user.getId());
            }
            System.out.println("导入用户配置成功，一共"+userList.size()+"条");
        }
        if(roomList != null && roomList.size()>0){
            for (RecordRoom room : roomList) {
                room.setId(null);
                room.setUploadUserId(userIdConverMap.get(room.getUploadUserId()));
                RecordRoom dbRoom = roomRepository.findByRoomId(room.getRoomId());
                if(dbRoom != null){
                    room.setId(dbRoom.getId());
                }
                roomRepository.save(room);
            }
            System.out.println("导入房间配置成功，一共"+roomList.size()+"条");
        }
        Map<Long,Long> historyIdConverMap = new HashMap<>();
        if(historyList != null && historyList.size()>0){
            for (RecordHistory history : historyList) {
                Long oldId = history.getId();
                history.setId(null);
                RecordHistory dbHistory = historyRepository.findBySessionId(history.getSessionId());
                if(dbHistory != null){
                    history.setId(dbHistory.getId());
                }
                historyRepository.save(history);
                historyIdConverMap.put(oldId,history.getId());
            }
            System.out.println("导入录制历史信息成功，一共"+historyList.size()+"条");
        }
        if(partList != null && partList.size()>0){
            for (RecordHistoryPart part : partList) {
                part.setId(null);
                RecordHistoryPart dbPart = partRepository.findByFilePath(part.getFilePath());
                if(dbPart != null){
                    part.setId(dbPart.getId());
                }
                part.setHistoryId(historyIdConverMap.get(part.getHistoryId()));
                partRepository.save(part);
            }
            System.out.println("导入分P数据成功，一共"+partList.size()+"条");
        }
        // 在控制台输出转换后的Map对象
        System.out.println("导入全部配置文件成功!");
    }

    @PostMapping("/update")
    public boolean update(@RequestBody RecordRoom room) {
        Optional<RecordRoom> roomOptional = roomRepository.findById(room.getId());
        if (roomOptional.isPresent()) {
            RecordRoom dbRoom = roomOptional.get();
            dbRoom.setTid(room.getTid());
            dbRoom.setTags(room.getTags());
            dbRoom.setUpload(room.isUpload());
            dbRoom.setUploadUserId(room.getUploadUserId());
            dbRoom.setTitleTemplate(room.getTitleTemplate());
            dbRoom.setPartTitleTemplate(room.getPartTitleTemplate());
            dbRoom.setDescTemplate(room.getDescTemplate());
            dbRoom.setDynamicTemplate(room.getDynamicTemplate());
            dbRoom.setCopyright(room.getCopyright());
            dbRoom.setLine(room.getLine());
            dbRoom.setCoverUrl(room.getCoverUrl());
            dbRoom.setWxuid(room.getWxuid());
            dbRoom.setPushMsgTags(room.getPushMsgTags());
            dbRoom.setFileSizeLimit(room.getFileSizeLimit());
            dbRoom.setDurationLimit(room.getDurationLimit());
            dbRoom.setDeleteType(room.getDeleteType());
            dbRoom.setDeleteDay(room.getDeleteDay());
            dbRoom.setMoveDir(room.getMoveDir());
            dbRoom.setSendDm(room.getSendDm());
            roomRepository.save(dbRoom);
            return true;
        }
        return false;
    }

    @PostMapping("/editLiveMsgSetting")
    public boolean editLiveMsgSetting(@RequestBody RecordRoom room) {
        Optional<RecordRoom> roomOptional = roomRepository.findById(room.getId());
        if (roomOptional.isPresent()) {
            RecordRoom dbRoom = roomOptional.get();
            dbRoom.setDmDistinct(room.getDmDistinct());
            dbRoom.setDmFanMedal(room.getDmFanMedal());
            dbRoom.setDmUlLevel(room.getDmUlLevel());
            dbRoom.setDmKeywordBlacklist(room.getDmKeywordBlacklist());
            roomRepository.save(dbRoom);
            return true;
        }
        return false;
    }

    @PostMapping("/add")
    public Map<String, String> add(@RequestBody RecordRoom add) {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isBlank(add.getRoomId())) {
            result.put("type", "info");
            result.put("msg", "请输入房间号");
            return result;
        }

        RecordRoom room = roomRepository.findByRoomId(add.getRoomId());
        if (room != null) {
            result.put("type", "warning");
            result.put("msg", "房间号已存在");
            return result;
        } else {
            room = new RecordRoom();
            room.setRoomId(add.getRoomId());
            roomRepository.save(room);
            result.put("type", "success");
            result.put("msg", "添加成功");
            return result;
        }
    }

    @GetMapping("/delete/{roomId}")
    public Map<String, String> add(@PathVariable("roomId") Long roomId) {
        Map<String, String> result = new HashMap<>();
        if (roomId == null) {
            result.put("type", "info");
            result.put("msg", "请输入房间号");
            return result;
        }

        try {
            Optional<RecordRoom> roomOptional = roomRepository.findById(roomId);
            if (roomOptional.isPresent()) {
                roomRepository.delete(roomOptional.get());
                result.put("type", "success");
                result.put("msg", "房间删除成功");
                return result;
            } else {
                result.put("type", "warning");
                result.put("msg", "房间不存在");
                return result;
            }
        } catch (Exception e) {
            result.put("type", "error");
            result.put("msg", "房间删除失败==>" + e.getMessage());
            return result;
        }
    }

    @PostMapping("/uploadCover")
    public Map<String, String> uploadCover(@RequestParam Long id, @RequestParam("file") MultipartFile file) {

        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入房间号");
            return result;
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                result.put("type", "warning");
                result.put("msg", "请上传图片文件");
                return result;
            }
            if (image.getWidth() < 1146 || image.getHeight() < 717) {
                result.put("type", "warning");
                result.put("msg", "上传图片分辨率不低于1146*717,当前分辨率为"+image.getWidth()+"*"+image.getHeight());
                return result;
            }
        } catch (IOException e) {
            result.put("type", "warning");
            result.put("msg", "封面上传失败：" + e.getMessage());
            return result;
        }

        Optional<RecordRoom> roomOptional = roomRepository.findById(id);
        if (roomOptional.isPresent()) {
            try {
                RecordRoom room = roomOptional.get();
                Long userId = room.getUploadUserId();
                if (userId == null) {
                    result.put("type", "warning");
                    result.put("msg", "房间未绑定上传用户");
                    return result;
                }
                Optional<BiliBiliUser> userOptional = userRepository.findById(userId);
                if (!userOptional.isPresent()) {
                    result.put("type", "warning");
                    result.put("msg", "房间未绑定上传用户");
                    return result;
                }
                BiliBiliUser user = userOptional.get();
                byte[] bytes = file.getBytes();
                String response = BiliApi.uploadCover(user, file.getName(), bytes);
                String url = JsonPath.read(response, "data.url");
                if (StringUtils.isNotBlank(url)) {
                    room.setCoverUrl(url);
                    roomRepository.save(room);
                    result.put("type", "success");
                    result.put("coverUrl", url);
                    result.put("msg", "封面上传成功");
                    return result;
                }

            } catch (IOException e) {
                result.put("type", "warning");
                result.put("msg", "封面上传失败：" + e.getMessage());
                return result;
            }
            result.put("type", "warning");
            result.put("msg", "封面上传失败");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "房间不存在");
            return result;
        }
    }

    @GetMapping("/lines")
    public UploadEnums[] lines() {
        return UploadEnums.values();
    }

    @GetMapping("/verification")
    public String verification(String template) {
        template = template.replace("${uname}", "主播名称")
                .replace("${title}", "直播标题")
                .replace("${roomId}", "房间号");
        if (template.contains("${")) {
            String date = template.substring(template.indexOf("${"), template.indexOf("}") + 1);
            String format = LocalDateTime.now().format(DateTimeFormatter.ofPattern(date.substring(2, date.length() - 1)));
            template = template.replace(date, format);
        }


        return template;
    }
}
