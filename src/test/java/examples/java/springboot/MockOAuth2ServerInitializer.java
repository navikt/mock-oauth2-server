package examples.java.springboot;

import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.io.IOException;
import java.util.Map;

//neccessary in order to create and start the server before the ApplicationContext is initialized, due to
//the spring boot oauth2 resource server dependency invoking the server on application context creation.
public class MockOAuth2ServerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    public static final String MOCK_OAUTH_2_SERVER_BASE_URL = "mock-oauth2-server.baseUrl";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var server = registerMockOAuth2Server(applicationContext);
        var baseUrl = server.baseUrl().toString().replaceAll("/$", "");

        TestPropertyValues
                .of(Map.of(MOCK_OAUTH_2_SERVER_BASE_URL, baseUrl))
                .applyTo(applicationContext);
    }

    private MockOAuth2Server registerMockOAuth2Server(ConfigurableApplicationContext applicationContext) {
        try {
            var server = new MockOAuth2Server();
            server.start();
            ((GenericApplicationContext) applicationContext).registerBean(MockOAuth2Server.class, () -> server);
            return server;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

