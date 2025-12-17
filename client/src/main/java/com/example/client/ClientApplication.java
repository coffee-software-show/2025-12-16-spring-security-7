package com.example.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

import java.util.Map;


@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

   /*
    @Bean
    Customizer<HttpSecurity> httpServerCustomizer() {
        var tenMinutes = Duration.ofMinutes(15);
        var mfa = AuthorizationManagerFactories.multiFactor()
                .requireFactors(f -> f
                        .requireFactor(RequiredFactor.Builder::passwordAuthority)
                        .requireFactor(RequiredFactor.Builder::ottAuthority)
                )
                .build();

        return http -> http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/admin").access(mfa.hasRole("ADMIN"))
                        .requestMatchers("/user").authenticated()
                );
    }
    */

    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}

@Controller
@ResponseBody
class ClientController {

    private final RestClient http;

    ClientController(RestClient http) {
        this.http = http;
    }

    @GetMapping("/")
    Map<String, String> hello(@RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client) {
        var at = client.getAccessToken();
        return this.http
                .get()
                .uri("http://localhost:8080")
                .headers(headers -> headers.setBearerAuth(at.getTokenValue()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

}