package examples.java.springboot.login;

import examples.java.springboot.MockOAuth2ServerInitializer;
import io.netty.handler.codec.http.cookie.Cookie;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.HashMap;
import java.util.Map;

import static examples.java.springboot.MockOAuth2ServerInitializer.MOCK_OAUTH_2_SERVER_BASE_URL;
import static examples.java.springboot.login.OAuth2LoginAppTest.PROVIDER;
import static examples.java.springboot.login.OAuth2LoginAppTest.PROVIDER_ID;
import static examples.java.springboot.login.OAuth2LoginAppTest.REGISTRATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = OAuth2LoginApp.class,
        //these can be set in application yaml if you desire
        properties = {
                REGISTRATION + PROVIDER_ID + ".client-id=testclient",
                REGISTRATION + PROVIDER_ID + ".client-secret=testsecret",
                REGISTRATION + PROVIDER_ID + ".authorization-grant-type=authorization_code",
                REGISTRATION + PROVIDER_ID + ".redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
                REGISTRATION + PROVIDER_ID + ".scope=openid",
                PROVIDER + PROVIDER_ID + ".authorization-uri=${" + MOCK_OAUTH_2_SERVER_BASE_URL + "}/issuer1/authorize",
                PROVIDER + PROVIDER_ID + ".token-uri=${" + MOCK_OAUTH_2_SERVER_BASE_URL + "}/issuer1/token",
                PROVIDER + PROVIDER_ID + ".jwk-set-uri=${" + MOCK_OAUTH_2_SERVER_BASE_URL + "}/issuer1/jwks"
        }
)
@ContextConfiguration(initializers = {MockOAuth2ServerInitializer.class})
public class OAuth2LoginAppTest {
    public static final String CLIENT = "spring.security.oauth2.client";
    public static final String PROVIDER = CLIENT + ".provider.";
    public static final String REGISTRATION = CLIENT + ".registration.";
    public static final String PROVIDER_ID = "myprovider";

    @LocalServerPort
    private int localPort;

    @Autowired
    private MockOAuth2Server mockOAuth2Server;

    @Test
    public void oidcUserFooShouldBeLoggedIn() {
        Map<String, Cookie> cookieManager = new HashMap<>();
        WebClient webClient = WebClient.builder()
                .clientConnector(followRedirectsWithCookies(cookieManager))
                .build();

        mockOAuth2Server.enqueueCallback(new DefaultOAuth2TokenCallback("issuer1", "foo"));

        String response = webClient
                .mutate()
                .baseUrl("http://localhost:" + localPort)
                .build()
                .get()
                .uri("/api/ping")
                .header("Accept", "text/html")
                .retrieve()
                .bodyToMono(String.class).block();

        assertEquals("hello foo", response);
    }

    private ClientHttpConnector followRedirectsWithCookies(Map<String, Cookie> cookieManager) {
        return new ReactorClientHttpConnector(
                HttpClient
                        .create()
                        .followRedirect((req, resp) -> {
                                    for (var entry : resp.cookies().entrySet()) {
                                        var cookie = entry.getValue().stream().findFirst().orElse(null);
                                        if (cookie != null && cookie.value() != null && !cookie.value().isBlank()) {
                                            cookieManager.put(entry.getKey().toString(), cookie);
                                        }
                                    }
                                    return resp.responseHeaders().contains("Location");
                                },
                                req -> {
                                    for (var cookie : cookieManager.entrySet()) {
                                        req.header("Cookie", cookie.getKey() + "=" + cookie.getValue().value());
                                    }
                                }
                        )
        );
    }
}
