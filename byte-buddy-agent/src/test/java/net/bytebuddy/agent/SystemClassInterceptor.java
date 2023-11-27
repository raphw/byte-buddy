package net.bytebuddy.agent;

public class SystemClassInterceptor {
    public static final String ENV_VAR_NAME = "test-env-var-name";
    public static final String ENV_VAR_VALUE = "test-env-var-value";

    public static String getenv(String envVarName) {
        if (envVarName.equals(ENV_VAR_NAME)) {
            return ENV_VAR_VALUE;
        } else {
            return System.getenv().get(envVarName);
        }
    }
}
