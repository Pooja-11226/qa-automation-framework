package com.sdet.api.tests;

import static com.sdet.api.tests.support.TestGroups.NEGATIVE;
import static com.sdet.api.tests.support.TestGroups.REGRESSION;
import static com.sdet.api.tests.support.TestGroups.SMOKE;
import static org.assertj.core.api.Assertions.assertThat;

import com.sdet.api.client.HttpStatus;
import com.sdet.api.config.ConfigManager;
import com.sdet.api.model.AuthRequest;
import com.sdet.api.model.AuthResponse;
import com.sdet.api.spec.ResponseSpecFactory;
import com.sdet.api.tests.support.Schemas;
import io.qameta.allure.Feature;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Feature("Authentication")
public class AuthTest extends BaseApiTest {

    private static final String BAD_CREDENTIALS_REASON = "Bad credentials";

    @Test(groups = {SMOKE, REGRESSION}, description = "Valid credentials return a non-empty token")
    public void validCredentialsReturnToken() {
        AuthResponse body = authClient.createToken(new AuthRequest(config.username(), config.password()))
                .then()
                .spec(ResponseSpecFactory.jsonMatchingSchema(HttpStatus.OK, Schemas.AUTH_TOKEN))
                .extract()
                .as(AuthResponse.class);

        assertThat(body.token()).as("token").isNotBlank();
        assertThat(body.reason()).as("failure reason").isNull();
    }

    @DataProvider(name = "invalidCredentials")
    public Object[][] invalidCredentials() {
        ConfigManager cfg = ConfigManager.getInstance();
        return new Object[][] {
            {"wrong password", new AuthRequest(cfg.username(), "wrong-password")},
            {"unknown username", new AuthRequest("unknown-user", cfg.password())},
            {"username in a different case", new AuthRequest(cfg.username().toUpperCase(), cfg.password())},
            {"empty username and password", new AuthRequest("", "")},
            {"missing username and password", new AuthRequest(null, null)},
        };
    }

    /**
     * Restful-Booker reports failed logins with HTTP 200 and a "reason" body (see KD-API-07 for the 401
     * expectation). The security-relevant contract asserted here is that no token is ever issued.
     */
    @Test(dataProvider = "invalidCredentials", groups = {NEGATIVE, REGRESSION},
            description = "Invalid credentials never issue a token")
    public void invalidCredentialsDoNotIssueToken(String scenario, AuthRequest credentials) {
        AuthResponse body = authClient.createToken(credentials)
                .then()
                .spec(ResponseSpecFactory.json(HttpStatus.OK))
                .extract()
                .as(AuthResponse.class);

        assertThat(body.token()).as("token for %s", scenario).isNull();
        assertThat(body.reason()).as("failure reason for %s", scenario).isEqualTo(BAD_CREDENTIALS_REASON);
    }
}
