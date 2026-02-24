package com.cmdev.profiler.memory;

import sun.misc.Unsafe;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.*;

/**
 * Computes the memory footprint (in bytes) of a Java object, including the full
 * reachable object graph ("deep size").
 * <p>
 * It relies on {@link Instrumentation#getObjectSize(Object)} for the shallow size and
 * {@link Unsafe} to efficiently walk non-primitive fields, avoiding double
 * counting in the presence of shared or cyclic references.
 * <p>
 * <b>NOTE:</b> The calculated size represents the actual memory usage on the heap
 * and may differ significantly from the size of the object when serialized.
 * The in-memory representation includes object headers, padding, and memory pointers,
 * which are not part of the serialized state. The exact size is also dependent on
 * the JVM (32/64-bit) and its configuration (e.g., CompressedOops).
 */
public class ObjectSizeCalculator {

    private static ObjectSizeCalculator instance;
    private final Instrumentation instrumentation;
    private static final Unsafe unsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("[MemUsage] Unable to obtain Unsafe", e);
        }
    }

    private ObjectSizeCalculator(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
    }

    public static void premain(Instrumentation instrumentation) {
        if (instance != null) {
            throw new SecurityException("You cannot alter the ObjectSizeCalculator programmatically!");
        }
        instance = new ObjectSizeCalculator(instrumentation);
    }

    public static ObjectSizeCalculator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ObjectSizeCalculator is not initialized");
        }
        return instance;
    }

    /**
     * Structured version of the object profile, returning a DTO with:
     * <ul>
     *     <li>root class and shallow size</li>
     *     <li>total deep size</li>
     *     <li>per-field / per-element entries, each with size and an optional label</li>
     * </ul>
     * Labels follow the same conventions used previously in the textual report
     * (e.g. "[SHARED/RECURSIVE]" and "null").
     */
    public ObjectSizeProfile getObjectProfile(Object target, boolean detailed) {
        if (target == null) {
            return new ObjectSizeProfile(null, 0, 0, new LinkedHashMap<>());
        }

        Map<Object, Object> visited = new IdentityHashMap<>();
        long totalSize = 0;

        long rootShallow = instrumentation.getObjectSize(target);
        totalSize += rootShallow;
        visited.put(target, null);

        Class<?> clazz = target.getClass();
        ObjectSizeProfile.Builder builder = ObjectSizeProfile.builder()
                .rootClass(clazz)
                .rootShallowSize(rootShallow);

        if (clazz.isArray()) {
            if (!clazz.getComponentType().isPrimitive()) {
                int length = Array.getLength(target);
                for (int i = 0; i < length; i++) {
                    Object element = Array.get(target, i);
                    String label = "[" + i + "]";
                    if (element != null && !visited.containsKey(element)) {
                        long childSize = computeDeepSize(element, visited);
                        totalSize += childSize;
                        builder.child(label, childSize, null);
                    } else if (element == null) {
                        builder.child(label, 0L, "null");
                    } else {
                        builder.child(label, 0L, "[SHARED/RECURSIVE]");
                    }
                }
            }
        } else {
            while (clazz != null) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                        continue;
                    }
                    String name = qualifyFieldName(field);
                    try {
                        long offset = unsafe.objectFieldOffset(field);
                        Object value = unsafe.getObject(target, offset);

                        if (value != null) {
                            if (visited.containsKey(value)) {
                                builder.child(name, 0L, "[SHARED/RECURSIVE]");
                            } else {
                                long fieldSize = computeDeepSize(value, visited);
                                totalSize += fieldSize;
                                builder.child(name, fieldSize, null);
                            }
                        } else {
                            builder.child(name, 0L, "null");
                        }
                    } catch (Exception e) {
                        builder.child(name, 0L, "ERROR ([MemUsage] " + e.getMessage() + ")");
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }

        builder.totalDeepSize(totalSize);
        ObjectSizeProfile size = builder.build();
        if (!detailed) {
            size.getChildren().clear();
        }
        return size;
    }

    /**
     * Internal iterative logic (with an explicit stack) to compute the size of a
     * subgraph of objects, starting from a given root and following all
     * non-primitive references.
     */
    private long computeDeepSize(Object root, Map<Object, Object> visited) {
        Stack<Object> stack = new Stack<>();
        stack.push(root);

        long size = 0;

        while (!stack.isEmpty()) {
            Object obj = stack.pop();

            if (obj == null || visited.containsKey(obj)) {
                continue;
            }

            visited.put(obj, null);
            size += instrumentation.getObjectSize(obj);

            Class<?> clazz = obj.getClass();

            if (clazz.isArray()) {
                if (!clazz.getComponentType().isPrimitive()) {
                    int length = Array.getLength(obj);
                    for (int i = 0; i < length; i++) {
                        Object val = Array.get(obj, i);
                        if (val != null && !visited.containsKey(val)) {
                            stack.push(val);
                        }
                    }
                }
            } else {
                while (clazz != null) {
                    Field[] fields = clazz.getDeclaredFields();
                    for (Field field : fields) {
                        if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                            continue;
                        }
                        long offset = unsafe.objectFieldOffset(field);
                        Object val = unsafe.getObject(obj, offset);
                        if (val != null && !visited.containsKey(val)) {
                            stack.push(val);
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
            }
        }
        return size;
    }

    private String qualifyFieldName(Field field) {
        return field.getDeclaringClass().getName() + "." + field.getName();
    }

    private String format(long bytes) {
        return NumberFormat.getInstance(Locale.ITALY).format(bytes) + " bytes";
    }

    /**
     * DTO representing a structured object size profile.
     */
    public static final class ObjectSizeProfile {
        private final Class<?> rootClass;
        private final long rootShallowSize;
        private final long totalDeepSize;
        /**
         * Children sizes and labels. Each entry represents a field or array element.
         */
        private final Map<String, ChildEntry> children;

        private ObjectSizeProfile(Class<?> rootClass,
                                  long rootShallowSize,
                                  long totalDeepSize,
                                  Map<String, ChildEntry> children) {
            this.rootClass = rootClass;
            this.rootShallowSize = rootShallowSize;
            this.totalDeepSize = totalDeepSize;
            this.children = new LinkedHashMap<>(children);
        }

        public Class<?> getRootClass() {
            return rootClass;
        }

        public long getRootShallowSize() {
            return rootShallowSize;
        }

        public long getTotalDeepSize() {
            return totalDeepSize;
        }

        public Map<String, ChildEntry> getChildren() {
            return children;
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ObjectSizeProfile{");
            sb.append("rootClass=").append(rootClass != null ? rootClass.getName() : "null");
            sb.append(", rootShallowSize=").append(rootShallowSize);
            sb.append(", totalDeepSize=").append(totalDeepSize);
            sb.append(", children=").append(children);
            sb.append('}');
            return sb.toString();
        }

        public static final class ChildEntry {
            private final long size;
            /**
             * Optional label for special cases such as "[SHARED/RECURSIVE]" or "null".
             * When null, the size field contains the computed deep size.
             */
            private final String label;

            private ChildEntry(long size, String label) {
                this.size = size;
                this.label = label;
            }

            public long getSize() {
                return size;
            }

            public String getLabel() {
                return label;
            }

            @Override
            public String toString() {
                return "ChildEntry{" +
                        "size=" + size +
                        ", label='" + label + '\'' +
                        '}';
            }
        }

        public static final class Builder {
            private Class<?> rootClass;
            private long rootShallowSize;
            private long totalDeepSize;
            private final Map<String, ChildEntry> children = new LinkedHashMap<>();

            public Builder rootClass(Class<?> rootClass) {
                this.rootClass = rootClass;
                return this;
            }

            public Builder rootShallowSize(long rootShallowSize) {
                this.rootShallowSize = rootShallowSize;
                return this;
            }

            public Builder totalDeepSize(long totalDeepSize) {
                this.totalDeepSize = totalDeepSize;
                return this;
            }

            public Builder child(String name, long size, String label) {
                children.put(name, new ChildEntry(size, label));
                return this;
            }

            @Override
            public String toString() {
                return "Builder{" +
                        "rootClass=" + (rootClass != null ? rootClass.getName() : "null") +
                        ", rootShallowSize=" + rootShallowSize +
                        ", totalDeepSize=" + totalDeepSize +
                        ", children=" + children +
                        '}';
            }

            public ObjectSizeProfile build() {
                return new ObjectSizeProfile(rootClass, rootShallowSize, totalDeepSize, children);
            }
        }
    }
}
