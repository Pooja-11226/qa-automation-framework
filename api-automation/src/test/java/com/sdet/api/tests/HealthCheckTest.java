package com.sdet.api.tests;

import static com.sdet.api.tests.support.TestGroups.REGRESSION;
import static com.sdet.api.tests.support.TestGroups.SMOKE;

import com.sdet.api.client.HealthClient;
import com.sdet.api.client.HttpStatus;
import com.sdet.api.spec.ResponseSpecFactory;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

@Feature("Health check")
public class HealthCheckTest extends BaseApiTest {

    @Test(groups = {SMOKE, REGRESSION}, description = "GET /ping reports the service as up (API returns 201)")
    public void pingReportsServiceIsUp() {
        new HealthClient().ping().then().spec(ResponseSpecFactory.status(HttpStatus.CREATED));
    }
}
