package br.com.felipebrandao.vidya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class VidyaApplication {

    public static void main(String[] args) {
        SpringApplication.run(VidyaApplication.class, args);
    }

}
