package top.sshh.bililiverecoder.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.sshh.bili.login.Login;
import top.sshh.bili.login.login.LoginImpl;
import top.sshh.bili.user.pojo.my.UserMyRootBean;
import top.sshh.bili.user.userinfo.UserMy;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.repo.BiliUserRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/biliUser")
public class BiliUserController {

    @Autowired
    BiliUserRepository biliUserRepository;

    @GetMapping("/login")
    public String loginUser() throws Exception {

        Login login = new LoginImpl();
        BufferedImage img = login.webLogin((cookie)->{
            UserMy userMy = new UserMy(cookie);
            try {

                UserMyRootBean userMyPojo = userMy.getPojo();
                int code = userMyPojo.getCode();
                if (code == 0) {
                    UserMyRootBean.Data dto = userMyPojo.getData();
                    BiliBiliUser biliUser = biliUserRepository.findByUid(dto.getMid());
                    if (biliUser == null) {
                        biliUser = new BiliBiliUser();
                    }
                    biliUser.setCookies(cookie.toStringV2());
                    biliUser.setUid(dto.getMid());
                    // biliUser.setAccessToken(dto.getAccessToken());
                    // biliUser.setRefreshToken(dto.getRefreshToken());
                    biliUser.setLogin(true);
                    biliUser.setUpdateTime(LocalDateTime.now());
                    biliUser.setUname(dto.getName());
                    log.info("{} 登录成功!!!", biliUser.getUname());
                    biliUserRepository.save(biliUser);
                    return;
                }
            }catch (Exception e){
                log.error("登录获取用户信息失败");
            }

        });

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", stream);
        byte[] bytes = Base64.encodeBase64(stream.toByteArray());
        return new String(bytes);
    }

    @GetMapping("/list")
    public List<BiliBiliUser> listBillUser() {
        List<BiliBiliUser> list = new ArrayList<>();
        for (BiliBiliUser biliBiliUser : biliUserRepository.findAll()) {
            list.add(biliBiliUser);
        }
        return list;
    }

    @PostMapping("/update")
    public boolean updateBillUser(@RequestBody BiliBiliUser user) {
        Optional<BiliBiliUser> userOptional = biliUserRepository.findById(user.getId());
        if (userOptional.isPresent()) {
            BiliBiliUser dbUser = userOptional.get();
            dbUser.setEnable(user.isEnable());
            dbUser.setUpdateTime(LocalDateTime.now());
            biliUserRepository.save(dbUser);
        }
        return false;
    }

    @GetMapping("/delete/{id}")
    public Map<String, String> delete(@PathVariable("id") Long id) {
        Map<String, String> result = new HashMap<>();
        if (id == null) {
            result.put("type", "info");
            result.put("msg", "请输入用户id");
            return result;
        }

        Optional<BiliBiliUser> userOptional = biliUserRepository.findById(id);
        if (userOptional.isPresent()) {
            biliUserRepository.delete(userOptional.get());
            result.put("type", "success");
            result.put("msg", "用户删除成功");
            return result;
        } else {
            result.put("type", "warning");
            result.put("msg", "用户不存在");
            return result;
        }
    }
}
