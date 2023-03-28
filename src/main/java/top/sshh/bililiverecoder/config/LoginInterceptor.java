package top.sshh.bililiverecoder.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Base64;


public class LoginInterceptor implements HandlerInterceptor {

    public LoginInterceptor(String userName,String password) {
        if(StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)){
            Base64.Encoder encoder = Base64.getEncoder();
            this.authString = "Basic " + encoder.encodeToString((userName+":"+password).getBytes());
        }else {
            this.authString = "";
        }
    }

    private final String authString;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if(StringUtils.isBlank(authString)){
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if(this.authString.equals(authorization)){
            return true;
        }
        response.setHeader("WWW-Authenticate", "Basic realm=\"Restricted\"");
        response.setStatus(401);
        return false;
    }
}
