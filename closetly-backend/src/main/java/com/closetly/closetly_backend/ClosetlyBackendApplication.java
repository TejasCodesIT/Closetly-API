package com.closetly.closetly_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

@SpringBootApplication
public class ClosetlyBackendApplication {

	public static void main(String[] args) {

		System.out.println(
				Encoders.BASE64.encode(Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded()));
		System.out.println("******************************************************************************");

		SpringApplication.run(ClosetlyBackendApplication.class, args);
	}

}
