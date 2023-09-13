package top.sshh.bililiverecoder.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.data.BiliSessionDto;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.util.BiliApi;

import java.time.LocalDateTime;

@Slf4j
@Component
public class BiliBiliUserService {


    @Autowired
    private BiliUserRepository userRepository;

    public boolean refreshToken(BiliBiliUser user) {
        String response = BiliApi.refreshToken(user);

        Integer code = JsonPath.read(response, "code");
        if (code == 0){
            BiliSessionDto dto = JSON.parseObject(JSON.toJSONString(JsonPath.read(response, "data.token_info")),BiliSessionDto.class);
            JSONArray cookies = JSON.parseArray(JsonPath.read(response, "data.cookie_info.cookies").toString());
            StringBuilder cookieString = new StringBuilder();
            for (Object object : cookies) {
                JSONObject cookie = (JSONObject)object;
                cookieString.append(cookie.get("name").toString());
                cookieString.append(":");
                cookieString.append(cookie.get("value").toString());
                cookieString.append("; ");
            }

            user.setCookies(cookieString.toString());
            log.info("{} 刷新token成功!!!", user.getUname());
            user.setUid(dto.getMid());
            user.setAccessToken(dto.getAccessToken());
            user.setRefreshToken(dto.getRefreshToken());
            try{
                String userInfo = BiliApi.appMyInfo(user);
                user.setUname(JsonPath.read(userInfo, "data.uname"));
            }catch (Exception e){
                log.error("刷新token 获取用户名称失败==>{}",user.getUname());
            }
            user.setLogin(true);
            user.setUpdateTime(LocalDateTime.now());
            userRepository.save(user);
            return true;
        }else {
            try {
                String userInfo = BiliApi.appMyInfo(user);
                user.setUname(JsonPath.read(userInfo, "data.uname"));
                user.setLogin(true);
                user.setUpdateTime(LocalDateTime.now());
                userRepository.save(user);
                log.error("{} 刷新token失败!!!，账号仍然可用==>{}", user.getUname(), response);
            } catch (Exception e) {
                log.error("刷新token失败 获取用户名称失败==>{}", user.getUname());
            }
            user.setLogin(false);
            user.setEnable(false);
            user.setUpdateTime(LocalDateTime.now());
            userRepository.save(user);
            log.error("{} 刷新token失败!!!==>{}", user.getUname(),response);
            return false;
        }

    }
}
