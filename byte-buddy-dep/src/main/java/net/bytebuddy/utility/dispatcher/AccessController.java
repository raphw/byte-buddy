package net.bytebuddy.utility.dispatcher;

import net.bytebuddy.build.AccessControllerPlugin;

import java.security.PrivilegedAction;

public class AccessController {

    private static final boolean ACCESS_CONTROLLER;

    static {
        boolean accessController;
        try {
            Class.forName("java.security.AccessController", false, null);
            accessController = Boolean.parseBoolean(System.getProperty("net.bytebuddy.securitymanager", "true"));
        } catch (ClassNotFoundException ignored) {
            accessController = false;
        } catch (SecurityException ignored) {
            accessController = true;
        }
        ACCESS_CONTROLLER = accessController;
    }

    private AccessController() {
        throw new UnsupportedOperationException();
    }

    @AccessControllerPlugin.Enhance
    protected static <T> T doPrivileged(PrivilegedAction<T> action) {
        if (ACCESS_CONTROLLER) {
            return java.security.AccessController.doPrivileged(action);
        }
        return action.run();
    }
}
