package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import javax.sql.DataSource;
import java.util.HashMap;

import static org.springframework.security.authorization.AuthenticatedAuthorizationManager.authenticated;

@EnableMultiFactorAuthentication(authorities = {
        FactorGrantedAuthority.PASSWORD_AUTHORITY,
        FactorGrantedAuthority.OTT_AUTHORITY
})
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
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
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return http -> http
                .webAuthn(a -> a
                        .rpName("bootiful")
                        .rpId("localhost")
                        .allowedOrigins("http://localhost:9090")
                )
                .oneTimeTokenLogin(ott -> ott
                        .tokenGenerationSuccessHandler((request, response, oneTimeToken) -> {
                            response.getWriter().println("you've got console mail!");
                            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                            IO.println("please go to http://localhost:" + request.getServerPort() +
                                    "/login/ott?token=" + oneTimeToken.getTokenValue());
                        })
                )
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/userinfo", "/oauth2/token").access(authenticated())
                );
    }
/*
    @Bean
    InMemoryUserDetailsManager userDetailsManager(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("josh").password(passwordEncoder.encode("pw")).roles("USER").build(),
                User.withUsername("rob").password(passwordEncoder.encode("pw")).roles("USER").build()
        );
    }
*/

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
