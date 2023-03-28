package top.sshh.bililiverecoder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer{

    @Value("${record.userName}")
    private String userName;

    @Value("${record.password}")
    private String password;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LoginInterceptor loginInterceptor = new LoginInterceptor(userName,password);
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**").excludePathPatterns("/**/recordWebHook");
    }
}
