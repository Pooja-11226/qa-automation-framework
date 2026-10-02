package com.sdet.api.tests.support;

/** TestNG group names. Run a group with: mvn test -Dgroups=smoke */
public final class TestGroups {
    public static final String SMOKE = "smoke";
    public static final String REGRESSION = "regression";
    public static final String NEGATIVE = "negative";
    public static final String KNOWN_DEFECT = "known-defect";

    private TestGroups() {
    }
}
