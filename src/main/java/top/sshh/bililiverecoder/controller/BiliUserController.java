package top.sshh.bililiverecoder.controller;

import com.alibaba.fastjson.JSON;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sshh.bililiverecoder.entity.BiliBiliUser;
import top.sshh.bililiverecoder.entity.data.BiliSessionDto;
import top.sshh.bililiverecoder.repo.BiliUserRepository;
import top.sshh.bililiverecoder.util.BiliApi;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/biliUser")
public class BiliUserController {

    @Autowired
    BiliUserRepository biliUserRepository;

    @GetMapping("/login")
    public void loginUser(HttpServletResponse response) throws Exception {

        BiliApi.BiliResponseDto<BiliApi.GenerateQRDto> s = BiliApi.generateQRUrlTV();
        if (s.getCode() != 0) {
            throw new RuntimeException("生成二维码异常，请检查日志");
        }
        BitMatrix bm = new QRCodeWriter().encode(s.getData().getUrl(),
                BarcodeFormat.QR_CODE, 256, 256);
        BufferedImage bi = MatrixToImageWriter.toBufferedImage(bm);
        ImageIO.write(bi, "jpg", response.getOutputStream());
        // 偷懒直接new一个Thread
        // new thread to check login status
        new Thread(() -> {
            for (int i = 0; i < 6; i++) {
                try {
                    String loginResp = BiliApi.loginOnTV(s.getData().getAuth_code());
                    Integer code = JsonPath.read(loginResp, "code");
                    if (code == 0) {
                        BiliSessionDto dto = JSON.parseObject(loginResp).getObject("data", BiliSessionDto.class);
                        BiliBiliUser biliUser = biliUserRepository.findByUid(dto.getMid());
                        if (biliUser == null) {
                            biliUser = new BiliBiliUser();
                        }
                        String userInfo = BiliApi.appMyInfo(dto.getAccessToken());
                        biliUser.setUname(JsonPath.read(userInfo, "data.name"));
                        log.info("{} 登录成功!!!", biliUser.getUname());
                        biliUser.setUid(dto.getMid());
                        biliUser.setAccessToken(dto.getAccessToken());
                        biliUser.setRefreshToken(dto.getRefreshToken());
                        biliUser.setUpdateTime(LocalDateTime.now());
                        biliUserRepository.save(biliUser);
                    }
                    Thread.sleep(10000);
                } catch (InterruptedException | IOException e) {
                    break;
                }
            }
        }).start();
    }

    @GetMapping("/list")
    public List<BiliBiliUser> listBillUser() {
        List<BiliBiliUser> list = new ArrayList<>();
        for (BiliBiliUser biliBiliUser : biliUserRepository.findAll()) {
            list.add(biliBiliUser);
        }
        return list;
    }
}
