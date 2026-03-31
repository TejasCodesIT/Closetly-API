package com.closetly.closetly_backend;

		import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClosetlyBackendApplication {

	public static void main(String[] args) {



        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashed = encoder.encode("Password@123");
        System.out.println("@@@@@@@@@@@@@@@@@@@@@"
		+hashed);
    
		SpringApplication.run(ClosetlyBackendApplication.class, args);
	}

}
