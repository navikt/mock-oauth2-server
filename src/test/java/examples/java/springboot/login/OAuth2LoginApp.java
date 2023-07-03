package examples.java.springboot.login;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.security.config.Customizer.withDefaults;

@SpringBootApplication
public class OAuth2LoginApp {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OAuth2LoginApp.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    @RestController
    @RequestMapping("/api")
    class ApiController {

        @GetMapping(value = "/ping", produces = MediaType.TEXT_HTML_VALUE)
        Mono<String> ping(@AuthenticationPrincipal OAuth2AuthenticationToken token) {
            return Mono.just("hello " + token.getPrincipal().getAttribute("sub"));
        }
    }

    @EnableWebFluxSecurity
    static
    class SecurityConfiguration {

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
            return http
                    .authorizeExchange(exchanges -> exchanges
                            .anyExchange().authenticated()
                    ).oauth2Login(withDefaults()).build();
        }

        @Bean
        WebClient client() {
            return WebClient.builder().build();
        }
    }
}
