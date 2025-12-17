package com.example.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

@SpringBootApplication
public class ServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
	}

}

@Controller
@ResponseBody
class GreetingsController {

	@GetMapping("/user")
	Map<String, String> user(Principal principal) {
		return this.response(principal);
	}

	@GetMapping("/admin")
	Map<String, String> admin(Principal principal) {
		return this.response(principal);
	}

	private Map<String, String> response(Principal authentication) {
		return Map.of("message", "hello, " + authentication.getName());
	}
}