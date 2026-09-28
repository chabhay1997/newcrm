package com.evtl.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.evtl.crm", "com.evtl.crm.employeeinfo", "config", "controller", "model", "repository", "service"})
@EntityScan(basePackages = {"model", "com.evtl.crm.employeeinfo"})
@EnableJpaRepositories(basePackages = {"repository", "com.evtl.crm.employeeinfo"})
@EnableScheduling
public class EvtlCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(EvtlCrmApplication.class, args);
    }
}
