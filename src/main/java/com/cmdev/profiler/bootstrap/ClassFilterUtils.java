package com.cmdev.profiler.bootstrap;

import net.bytebuddy.description.type.TypeDescription;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ClassFilterUtils {

    // Loads filters from an external properties file specified by the system property 'cmdev.profiler.filters.path'.
    private static final Properties FILTERS = new Properties();

    static {
        String customPath = System.getProperty("cmdev.profiler.filters.path");
        if (customPath == null || customPath.isEmpty()) {
            System.err.println("[CMDev] You must specify the filters.properties file path using -Dcmdev.profiler.filters.path=/path/to/filters.properties");
        } else {
            try (InputStream in = new java.io.FileInputStream(customPath)) {
                FILTERS.load(in);
            } catch (IOException e) {
                System.err.println("[CMDev] Unable to load filters.properties from custom path: " + e.getMessage());
            }
        }
    }

    // Utility method to get a list from a property key, splitting by comma and trimming values.
    private static List<String> getList(String key) {
        String value = FILTERS.getProperty(key, "");
        if (value.isEmpty()) return Collections.emptyList();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    // Lists of included and excluded packages/classes loaded from configuration.
    private static final List<String> INCLUDED_PACKAGES = getList("included.packages");
    private static final List<String> EXCLUDED_PACKAGES = getList("excluded.packages");
    private static final List<String> EXCLUDED_CLASSES = getList("excluded.classes");

    // Checks if a class should be included for instrumentation based on filters.
    public static boolean isIncludedClass(TypeDescription clazz) {
        if (clazz.isPublic()) {
            if (clazz.isInterface()) {
                return false;
            }
            for (String excludedPackage : EXCLUDED_PACKAGES) {
                if (clazz.getPackage() != null && clazz.getPackage().getName().contains(excludedPackage)) {
                    return false;
                }
            }
            for (String excludedClass : EXCLUDED_CLASSES) {
                if (clazz.getName().toUpperCase().contains(excludedClass)) {
                    return false;
                }
            }
            for (String includedPackage : INCLUDED_PACKAGES) {
                if (clazz.getPackage() != null && clazz.getPackage().getName().contains(includedPackage)) {
                    return true;
                }
            }
        }
        return false;
    }
}
