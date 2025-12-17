package com.example.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.AuthorizationManagerFactory;
import org.springframework.security.authorization.DefaultAuthorizationManagerFactory;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.authorization.AuthenticatedAuthorizationManager.authenticated;


/**
 * 1.0 EnableMFA with two authorities
 * 2.0 but if u wanna deviate, u have two options.
 * 2.1 let's say u want the default to be NON-MFA. say, only /admin.
 * in this case enable MFA with an empty annotation. then configure a AMF as a variable passed to access(AMF).
 * 2.2 u might want MFA for everything but might want to override and make it even more demanding in certain spots.
 * U can use the annotation and then for certain access endpoints, u can pass an AMF with, say, a duration.
 * 3.0 remember that the AMF can also be used for all sorts of other cool usecases. u can talk to some external service via the AMF.
 */

/*@EnableMultiFactorAuthentication(authorities = {
        FactorGrantedAuthority.PASSWORD_AUTHORITY,
        FactorGrantedAuthority.OTT_AUTHORITY
})*/
@EnableMultiFactorAuthentication(authorities = {})
@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @Bean
    DefaultAuthorizationManagerFactory<Object> myAMF() {
        var builder = AuthorizationManagerFactories
                .multiFactor()
                .requireFactors(FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.OTT_AUTHORITY);
        return builder.build();
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
    Customizer<HttpSecurity> httpSecurityCustomizer(AuthorizationManagerFactory<Object> implicit) {

        var whatever = AuthorizationManagerFactories
                .multiFactor()
//                .requireFactors( b -> b
//                        .requireFactor( bb -> bb.ottAuthority().validDuration(Duration.ofMinutes(1)).build())
//                        .requireFactor( bb -> bb.passwordAuthority().build())
//                )
//                .requireFactor(factor -> factor.ottAuthority( otta -> otta.duration()))
                .requireFactor(fact -> fact.passwordAuthority().build())
                .build();
        var admin = AuthorizationManagerFactories
                .multiFactor()
                .requireFactors(FactorGrantedAuthority.OTT_AUTHORITY, FactorGrantedAuthority.PASSWORD_AUTHORITY)
                .build();
        var everybodyElse = new DefaultAuthorizationManagerFactory<>();

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
                                .requestMatchers("/user").access(everybodyElse.authenticated())
                                .requestMatchers("/admin").authenticated() // equivalent to access(implicitAMFBean.authenticated()), so no need for admin MFA anymore!
//                                .requestMatchers("/admin").access(implicit.authenticated()) // equivalent to access(implicitAMFBean.authenticated()), so no need for admin MFA anymore!
//                        .requestMatchers("/admin").access(admin.authenticated())
                );
    }

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        var users = new JdbcUserDetailsManager(dataSource);
        users.setEnableUpdatePassword(true);
        return users;
    }

}

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
}
