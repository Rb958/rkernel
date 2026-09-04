/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.support;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * Builds the JARs the loaders are tested against, by compiling a source string
 * at test time. The classes end up in packages that do not exist on the test
 * classpath, so the loader really has to find them in the JAR.
 */
public final class JarFixtures {
    private JarFixtures() {}

    /** Source of a minimal component under a unique package, answering {@code answer} to {@code type}. */
    public static String componentSource(String pkg, String simpleName, String componentName, String type, String answer) {
        return "package " + pkg + ";\n"
                + "import rkernel.IKernel; import rkernel.component.IComponent; import rkernel.signal.*;\n"
                + "import java.util.*;\n"
                + "public class " + simpleName + " implements IComponent {\n"
                + "  public void load(IKernel k) {}\n"
                + "  public String getName() { return \"" + componentName + "\"; }\n"
                + "  public Collection<BasicSignal<?>> getSignals() { return List.of(); }\n"
                + "  public void addSignalListener(SignalListener l) {}\n"
                + "  public boolean isRunning() { return true; }\n"
                + "  public void stop() {}\n"
                + "  public Collection<String> getSignalTypes() { return List.of(\"" + type + "\"); }\n"
                + "  public Object processSignal(BasicSignal<?> s) { return \"" + answer + "\"; }\n"
                + "}\n";
    }

    /** Compiles one class and packages it, alone, into {@code jarPath}. */
    public static Path compileToJar(Path workDir, String fullClassName, String source, Path jarPath) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("tests need a JDK, not a JRE");
        Path classes = Files.createDirectories(workDir.resolve("classes-" + fullClassName.hashCode()));

        JavaFileObject unit = new SimpleJavaFileObject(
                URI.create("string:///" + fullClassName.replace('.', '/') + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignore) { return source; }
        };
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of("-d", classes.toString(), "-classpath", System.getProperty("java.class.path"));
            boolean ok = compiler.getTask(null, fm, null, options, null, List.of(unit)).call();
            if (!ok) throw new IllegalStateException("fixture does not compile: " + fullClassName);
        }

        try (OutputStream out = Files.newOutputStream(jarPath); JarOutputStream jar = new JarOutputStream(out);
             Stream<Path> files = Files.walk(classes)) {
            for (Path p : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                jar.putNextEntry(new JarEntry(classes.relativize(p).toString().replace('\\', '/')));
                try (InputStream in = Files.newInputStream(p)) { in.transferTo(jar); }
                jar.closeEntry();
            }
        }
        return jarPath;
    }

    /** A JAR that contains a class, but no component: the case that crashed 1.0. */
    public static Path jarWithoutComponent(Path workDir, Path jarPath) throws IOException {
        String pkg = "fixture.plain" + Math.abs(jarPath.hashCode());
        String src = "package " + pkg + "; public class Nothing { public int n() { return 1; } }";
        return compileToJar(workDir, pkg + ".Nothing", src, jarPath);
    }
}
