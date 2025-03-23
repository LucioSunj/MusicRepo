package org.example.cpt202music;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("org.example.cpt202music.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class Cpt202MusicApplication {

    public static void main(String[] args) {
        SpringApplication.run(Cpt202MusicApplication.class, args);
    }

}
