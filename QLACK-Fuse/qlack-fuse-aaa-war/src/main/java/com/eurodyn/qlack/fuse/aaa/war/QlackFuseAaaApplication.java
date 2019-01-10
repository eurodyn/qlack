package com.eurodyn.qlack.fuse.aaa.war;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"com.eurodyn.qlack.fuse.aaa.repository"})
@EntityScan(basePackages = {"com.eurodyn.qlack.fuse.aaa.model"})
@ComponentScan(basePackages = {"com.eurodyn.qlack.fuse.aaa.service",
    "com.eurodyn.qlack.fuse.aaa.mappers",
    "com.eurodyn.qlack.fuse.aaa.config",
    "com.eurodyn.qlack.fuse.aaa.ws",
    "com.eurodyn.qlack.fuse.aaa.war.config",
    "com.eurodyn.qlack.util.swagger.config"
})
public class QlackFuseAaaApplication {

    public static void main(String[] args) {
        SpringApplication.run(QlackFuseAaaApplication.class, args);
    }


}
