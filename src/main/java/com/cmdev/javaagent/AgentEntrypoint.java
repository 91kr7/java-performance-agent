package com.cmdev.javaagent;

import com.cmdev.net.ManagementHttpServer;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

@SuppressWarnings("SystemOut")
public final class AgentEntrypoint {

    public static void premain(String agentArgs, Instrumentation inst) {
        startAgent(inst, agentArgs);
    }

    private static void startAgent(
            Instrumentation inst, String agentArgs) {
        try {
            System.out.println("[CMDev] Starting CmDev Profiling Agent...");
            new ManagementHttpServer().run();
            File javaagentFile = installBootstrapJar(inst);
            AgentInitializer.initialize(inst, javaagentFile);
        } catch (Throwable ex) {
            // Do not rethrow. No log manager is available here, so just print to stderr.
            System.err.println("[CMDev] Failed to start CmDev Profiling Agent: " + ex.getMessage());
        }
    }

    private static synchronized File installBootstrapJar(Instrumentation inst)
            throws IOException, URISyntaxException {
        // We do not use AgentEntrypoint.class.getProtectionDomain().getCodeSource() to get the agent
        // location because getProtectionDomain performs a permission check with the security manager.
        ClassLoader classLoader = AgentEntrypoint.class.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        URL url =
                classLoader.getResource(AgentEntrypoint.class.getName().replace('.', '/') + ".class");
        if (url == null || !"jar".equals(url.getProtocol())) {
            throw new IllegalStateException("could not get agent jar location from url " + url);
        }
        String resourcePath = url.toURI().getSchemeSpecificPart();
        int protocolSeparatorIndex = resourcePath.indexOf(":");
        int resourceSeparatorIndex = resourcePath.indexOf("!/");
        if (protocolSeparatorIndex == -1 || resourceSeparatorIndex == -1) {
            throw new IllegalStateException("could not get agent location from url " + url);
        }
        String agentPath = resourcePath.substring(protocolSeparatorIndex + 1, resourceSeparatorIndex);
        File javaagentFile = new File(agentPath);

        if (!javaagentFile.isFile()) {
            throw new IllegalStateException(
                    "agent jar location doesn't appear to be a file: " + javaagentFile.getAbsolutePath());
        }

        JarFile agentJar = new JarFile(javaagentFile, false);
        verifyJarManifestMainClassIsThis(javaagentFile, agentJar);
        inst.appendToBootstrapClassLoaderSearch(agentJar);
        return javaagentFile;
    }

    private static void verifyJarManifestMainClassIsThis(File jarFile, JarFile agentJar)
            throws IOException {
        Manifest manifest = agentJar.getManifest();
        if (manifest.getMainAttributes().getValue("Premain-Class") == null) {
            throw new IllegalStateException(
                    "The agent was not installed, because the agent was found in '"
                            + jarFile
                            + "', which doesn't contain a Premain-Class manifest attribute. Make sure that you"
                            + " haven't included the agent jar file inside of an application uber jar.");
        }
    }

    public static void main(String... args) {
        try {
            System.out.println(AgentEntrypoint.class.getPackage().getImplementationVersion());
        } catch (RuntimeException e) {
            System.out.println("[CMDev] Failed to parse agent version");
            e.printStackTrace();
        }
    }

    private AgentEntrypoint() {
    }
}
