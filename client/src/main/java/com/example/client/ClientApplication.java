package com.example.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.RequiredFactor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.ImportHttpServices;

import java.time.Duration;

@ImportHttpServices(Client.class)
@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    OAuth2RestClientHttpServiceGroupConfigurer securityConfigurer(
            OAuth2AuthorizedClientManager manager) {
        return OAuth2RestClientHttpServiceGroupConfigurer.from(manager);
    }

    @Bean
    RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}

@Controller
@ResponseBody
class ClientController {

    private final Client client;

    ClientController(Client client) {
        this.client = client;
    }

    @GetMapping("/")
    Message hello(
//            @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient auth2AuthorizedClient
    ) {
        return this.client.getMessage();
    }

}


// refactor from manual RestClient + RegisteredOAuth2AuthorizedClient to declarative interfaces
// using OAuth2RestClientHttpServiceGroupConfigurer + @ClientRegistrationId("clientId")

@ClientRegistrationId("spring")
interface Client {

    @GetExchange("http://localhost:8080")
    Message getMessage();
}

record Message(String message) {
}