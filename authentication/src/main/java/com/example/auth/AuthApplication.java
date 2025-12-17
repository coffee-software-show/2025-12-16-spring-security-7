package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

//@EnableMultiFactorAuthentication( authorities = {
//        FactorGrantedAuthority.OTT_AUTHORITY ,
//        FactorGrantedAuthority.PASSWORD_AUTHORITY
//})
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    private final AtomicInteger port = new AtomicInteger(0);

    @EventListener
    void on(WebServerInitializedEvent serverInitializedEvent) {
        this.port.set(serverInitializedEvent.getWebServer().getPort());
    }

    @Bean
    @SuppressWarnings("deprecation")
    DelegatingPasswordEncoder delegatingPasswordEncoder() {
        var encodingId = "password4j-argon2";
        var encoders = new HashMap<String, PasswordEncoder>();
        encoders.put(encodingId, new Argon2Password4jPasswordEncoder());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("noop", NoOpPasswordEncoder.getInstance());
        encoders.put("sha256", new StandardPasswordEncoder());
        return new DelegatingPasswordEncoder(encodingId, encoders);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .oauth2AuthorizationServer(a -> a.oidc(Customizer.withDefaults()))
                .webAuthn(a -> a
                        .rpName("bootiful")
                        .rpId("localhost")
                        .allowedOrigins("http://localhost:9090")
                )
                .oneTimeTokenLogin(ott -> ott
                        .tokenGenerationSuccessHandler((_, response, oneTimeToken) -> {

                            response.getWriter().println("you've got console mail!");
                            response.setContentType(MediaType.TEXT_PLAIN_VALUE);

                            IO.println("please go to http://localhost:" + this.port.get() +
                                    "/login/ott?token=" +
                                    oneTimeToken.getTokenValue());
                        })
                )
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .build();
    }

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        var users = new JdbcUserDetailsManager(dataSource);
        users.setEnableUpdatePassword(true);
        return users;
    }

}
/*

@Controller
@ResponseBody
class GreetingsController {

    @GetMapping("/user")
    Map<String, String> user(Principal principal) {
        return response(principal);
    }

    @GetMapping("/admin")
    Map<String, String> admin(Principal principal) {
        return response(principal);
    }

    private Map<String, String> response(Principal authentication) {
        return Map.of("message", "hello, " + authentication.getName());
    }
}*/
