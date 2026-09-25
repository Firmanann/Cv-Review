package com.mann.cvreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.mann.cvreview.util.config.ParsingConfig;

@SpringBootApplication
@EnableConfigurationProperties(ParsingConfig.class)
public class CvreviewApplication {

	public static void main(String[] args) {
		SpringApplication.run(CvreviewApplication.class, args);
	}
}
