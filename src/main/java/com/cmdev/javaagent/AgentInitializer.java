package com.cmdev.javaagent;

import com.cmdev.profiler.bootstrap.AgentStarter;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.security.PrivilegedExceptionAction;


public final class AgentInitializer {

    private static AgentStarter agentStarter = null;
    private static boolean isSecurityManagerSupportEnabled = false;
    private static volatile boolean agentStarted = false;

    public static void initialize(
            Instrumentation inst, File javaagentFile)
            throws Exception {

        if (AgentInitializer.class.getClassLoader() != null) {
            throw new IllegalStateException("agent initializer should be loaded in boot loader");
        }

        isSecurityManagerSupportEnabled = isSecurityManagerSupportEnabled();

        // This call uses a lambda instead of an anonymous class for privileged execution.
        execute(() -> {
            agentStarter = createAgentStarter(inst, javaagentFile);
            agentStarter.start();
            agentStarted = true;
            return null;
        });
    }

    private static AgentStarter createAgentStarter(Instrumentation instrumentation, File javaagentFile)
            throws Exception {
        Class<?> starterClass =
                Class.forName("com.cmdev.profiler.bootstrap.AgentStarterImpl");
        Constructor<?> constructor =
                starterClass.getDeclaredConstructor(Instrumentation.class);
        return (AgentStarter) constructor.newInstance(instrumentation);
    }

    private static void execute(PrivilegedExceptionAction<Void> action) throws Exception {
        if (isSecurityManagerSupportEnabled) {
            doPrivilegedExceptionAction(action);
        } else {
            action.run();
        }
    }

    private static boolean isSecurityManagerSupportEnabled() {
        return false;
    }

    @SuppressWarnings("removal") // AccessController is deprecated for removal
    private static <T> T doPrivilegedExceptionAction(PrivilegedExceptionAction<T> action)
            throws Exception {
        return java.security.AccessController.doPrivileged(action);
    }

    private AgentInitializer() {
    }
}
