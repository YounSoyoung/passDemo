package com.example.demo.nice.controller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "nice")
public class NiceProperties {

    /**
     * 이것은 나이스 아이디 모듈 사용을 위한 사이트 코드입니다.
     */
    @Getter
    @Setter
    private String siteCode;

    @Getter
    @Setter
    private String password;
}
