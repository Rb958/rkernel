/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rkernel.BasicKernel;
import rkernel.support.JarFixtures;
import rkernel.support.TextSignal;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BasicComponentLoaderTest {

    @TempDir
    Path root;

    private BasicKernel kernelWith(BasicComponentLoader loader) {
        return new BasicKernel.Builder().setName("host").setDocumentRoot(root.toFile())
                .setComponentLoader(loader).build();
    }

    @Test
    void loadsAComponentFromAJarAndRoutesItsSignals() throws Exception {
        BasicComponentLoader loader = new BasicComponentLoader();
        BasicKernel kernel = kernelWith(loader);
        Path jar = JarFixtures.compileToJar(root, "fixture.pay.PaymentComponent",
                JarFixtures.componentSource("fixture.pay", "PaymentComponent", "payments", "payment", "ok"),
                root.resolve("payments.jar"));

        IComponent loaded = loader.loadOne(jar.toFile());

        assertNotNull(loaded);
        assertEquals("payments", loaded.getName());
        assertSame(loaded, kernel.findComponentByName("payments"));
        assertEquals("ok", kernel.processSignal(new TextSignal("payment", "x")));
    }

    @Test
    void aJarWithoutAComponentIsSkippedNotACrash() throws Exception {
        // 1.0 looped with "hasMoreElements() || !found" and died in NoSuchElementException here.
        BasicComponentLoader loader = new BasicComponentLoader();
        kernelWith(loader);
        Path jar = JarFixtures.jarWithoutComponent(root, root.resolve("plain.jar"));

        assertNull(loader.loadSingleFile(jar.toFile(), IComponent.class));
        assertNull(loader.loadOne(jar.toFile()));
    }

    @Test
    void aMissingJarYieldsNull() throws Exception {
        BasicComponentLoader loader = new BasicComponentLoader();
        assertNull(loader.loadSingleFile(new File(root.toFile(), "absent.jar"), IComponent.class));
    }

    @Test
    void loadComponentsPicksUpEveryJarOfTheFolder() throws Exception {
        BasicComponentLoader loader = new BasicComponentLoader();
        BasicKernel kernel = kernelWith(loader);
        Path folder = Files.createDirectories(root.resolve("components"));
        JarFixtures.compileToJar(root, "fixture.a.A",
                JarFixtures.componentSource("fixture.a", "A", "a", "type-a", "A"), folder.resolve("a.jar"));
        JarFixtures.compileToJar(root, "fixture.b.B",
                JarFixtures.componentSource("fixture.b", "B", "b", "type-b", "B"), folder.resolve("b.jar"));
        Files.writeString(folder.resolve("notes.txt"), "ignored");

        loader.loadComponents(folder.toFile());

        assertEquals(2, kernel.getComponents().size());
        assertEquals("B", kernel.processSignal(new TextSignal("type-b", "")));
    }

    @Test
    void aJarDroppedInTheWatchedFolderIsLoadedWithoutRestart() throws Exception {
        BasicComponentLoader loader = new BasicComponentLoader();
        BasicKernel kernel = kernelWith(loader);
        Path folder = Files.createDirectories(root.resolve("watched"));
        loader.watch(folder.toFile());
        Thread.sleep(300); // let the WatchService register before the file appears

        Path staged = JarFixtures.compileToJar(root, "fixture.hot.Hot",
                JarFixtures.componentSource("fixture.hot", "Hot", "hot", "hot", "reloaded"),
                root.resolve("hot-staged.jar"));
        Files.move(staged, folder.resolve("hot.jar")); // atomic: the watcher sees a complete JAR

        long deadline = System.currentTimeMillis() + 10_000;
        while (kernel.findComponentByName("hot") == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertEquals("reloaded", kernel.processSignal(new TextSignal("hot", "")));
    }

    @Test
    void aDeletedJarUnregistersItsComponent() throws Exception {
        BasicComponentLoader loader = new BasicComponentLoader();
        BasicKernel kernel = kernelWith(loader);
        Path jar = JarFixtures.compileToJar(root, "fixture.gone.Gone",
                JarFixtures.componentSource("fixture.gone", "Gone", "gone", "gone", "?"), root.resolve("gone.jar"));
        loader.loadOne(jar.toFile());
        assertNotNull(kernel.findComponentByName("gone"));

        loader.wipeComponent(jar.toFile());

        assertNull(kernel.findComponentByName("gone"));
        assertNull(kernel.getSignalManager().findInterpreter("gone"));
    }
}
