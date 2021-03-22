package examples.java.springboot.resourceserver;

import examples.java.springboot.MockOAuth2ServerInitializer;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static examples.java.springboot.MockOAuth2ServerInitializer.MOCK_OAUTH_2_SERVER_BASE_URL;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = OAuth2ResourceServerApp.class,
        properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=${" + MOCK_OAUTH_2_SERVER_BASE_URL + "}/issuer1"
)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
public class OAuth2ResourceServerAppTest {
    @Autowired
    private WebTestClient webClient;
    @Autowired
    private MockOAuth2Server mockOAuth2Server;

    @Test
    @DisplayName("api should return 401 if no bearer token present")
    public void isUnauthorized() {
        webClient.get()
                .uri("/api/ping")
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    @DisplayName("api should return 200 when valid token is present")
    public void validTokenShouldReturn200Ok() {
        var token = mockOAuth2Server.issueToken("issuer1", "foo");
        webClient.get()
                .uri("/api/ping")
                .headers(headers -> headers.setBearerAuth(token.serialize()))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class).isEqualTo("hello foo");

    }
}
