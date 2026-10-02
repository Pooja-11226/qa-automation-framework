package com.sdet.api.listener;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Logs each test's lifecycle and puts the test name into the logging MDC, so every HTTP log line printed
 * while tests run in parallel can be traced back to the test that issued it.
 */
public class TestLoggingListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(TestLoggingListener.class);
    private static final String MDC_TEST_KEY = "test";

    @Override
    public void onTestStart(ITestResult result) {
        MDC.put(MDC_TEST_KEY, displayName(result));
        LOG.info("STARTED {}", displayName(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOG.info("PASSED  {} ({} ms)", displayName(result), durationMs(result));
        MDC.remove(MDC_TEST_KEY);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable failure = result.getThrowable();
        LOG.error("FAILED  {} ({} ms): {}", displayName(result), durationMs(result),
                failure == null ? "no exception recorded" : failure.getMessage());
        MDC.remove(MDC_TEST_KEY);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.warn("SKIPPED {}", displayName(result));
        MDC.remove(MDC_TEST_KEY);
    }

    private static String displayName(ITestResult result) {
        String name = result.getTestClass().getRealClass().getSimpleName() + "." + result.getMethod().getMethodName();
        Object[] parameters = result.getParameters();
        return parameters == null || parameters.length == 0 ? name : name + Arrays.toString(parameters);
    }

    private static long durationMs(ITestResult result) {
        return result.getEndMillis() - result.getStartMillis();
    }
}
