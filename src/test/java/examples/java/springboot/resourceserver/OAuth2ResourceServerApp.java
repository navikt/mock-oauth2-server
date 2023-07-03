package examples.java.springboot.resourceserver;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class OAuth2ResourceServerApp {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OAuth2ResourceServerApp.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @RestController
    @RequestMapping("/api")
    class ApiController {

        @GetMapping("/ping")
        Mono<String> ping(@AuthenticationPrincipal JwtAuthenticationToken jwtAuthenticationToken) {
            return Mono.just("hello " + jwtAuthenticationToken.getToken().getSubject());
        }
    }

    @EnableWebFluxSecurity
    class SecurityConfiguration {

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
            return http
                    .authorizeExchange(exchanges -> exchanges
                            .anyExchange().authenticated()
                    ).oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(Customizer.withDefaults())
                    ).build();
        }

        @Bean
        WebClient client() {
            return WebClient.builder().build();
        }
    }
}

