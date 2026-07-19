package com.dong.dongpicturebackendpictureservice.infrastructure.config;

import com.dong.dongpicturebackendpictureservice.infrastructure.algorithm.HeavyKeeper;
import com.dong.dongpicturebackendpictureservice.infrastructure.algorithm.TopK;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HotKeyDetectorConfig {

    @Bean
    public TopK hotKeyDetector() {
        return new HeavyKeeper(100, 100000, 5, 0.92, 10);
    }
}
