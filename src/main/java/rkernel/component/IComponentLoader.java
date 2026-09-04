/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.component;

import rkernel.IKernel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads modules of type {@code T} from JAR files and watches a folder for new ones.
 */
public interface IComponentLoader<T> {

    void loadComponents(File folder);

    Map<String, T> getComponents();

    /**
     * Finds, in a JAR, the first class that implements {@code interClass} and
     * returns it loaded, or {@code null} when the JAR contains none.
     *
     * <p>The class loader is deliberately left open: the returned class, and
     * everything it references lazily, must stay loadable for as long as the
     * module lives. Closing the loader here — as 1.0 did with try-with-resources —
     * worked only because the JVM had already loaded everything eagerly in the
     * scan loop; it is not something to rely on.</p>
     */
    default Class<?> loadSingleFile(File file, Class<?> interClass) throws IOException {
        if (!file.exists()) {
            return null;
        }
        URL url = file.toPath().toUri().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[]{url}, interClass.getClassLoader());
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            // 1.0 looped with "hasMoreElements() || !found": on a JAR with no
            // matching class, it ran past the last entry and died with
            // NoSuchElementException. A JAR without a component is a normal case.
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.endsWith(".class") || name.equals("module-info.class") || name.startsWith("META-INF/")) {
                    continue;
                }
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                Class<?> candidate;
                try {
                    candidate = Class.forName(className, false, loader);
                } catch (ClassNotFoundException | LinkageError e) {
                    continue; // a class whose dependencies are missing is not the one we want
                }
                if (candidate != interClass && interClass.isAssignableFrom(candidate)
                        && !candidate.isInterface()
                        && !java.lang.reflect.Modifier.isAbstract(candidate.getModifiers())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    void setKernel(IKernel kernel);

    void watch(File watchDirectory);
}
