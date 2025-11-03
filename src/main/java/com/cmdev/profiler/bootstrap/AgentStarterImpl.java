package com.cmdev.profiler.bootstrap;

import com.cmdev.profiler.instrument.PerformanceTracer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class AgentStarterImpl implements AgentStarter {

    private final Instrumentation instrumentation;

    public AgentStarterImpl(
            Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    @Override
    public void start() {

        try {
            new AgentBuilder.Default()
                    //.with(AgentBuilder.Listener.StreamWriting.toSystemError())
                    .type(ClassFilterUtils::isIncludedClass)
                    .transform((builder, typeDescription, classLoader, module, domain) -> {
                        try {
                            if (isMyApplication(classLoader)) {
                                return builder.method(
                                                ElementMatchers.isDeclaredBy(typeDescription)
                                                        .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                                        )
                                        .intercept(Advice.to(PerformanceTracer.class, ClassFileLocator.ForClassLoader.of(classLoader)));
                            }
                        } catch (Throwable e) {
                            System.err.println("[CMDev] Failed to instrument method!: " + e.getMessage());

                        }
                        return builder;
                    }).installOn(instrumentation);
        } catch (Throwable e) {
            System.err.println("[CMDev] Failed to start agent!: " + e.getMessage());
        }
    }

    private static boolean isMyApplication(ClassLoader classLoader) {
        // Only trace application classes and libraries.
        String allowedContext = System.getProperty("cmdev.profiler.context");
        if (allowedContext == null) return true;
        return classLoader != null
                && classLoader.getName() != null
                && classLoader.getName().contains(allowedContext);
    }
}
