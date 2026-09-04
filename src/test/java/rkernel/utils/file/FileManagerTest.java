/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.utils.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rkernel.exception.FileManagerException;
import rkernel.signal.SignalRegistry;
import rkernel.support.EchoComponent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileManagerTest {

    @TempDir
    Path dir;

    @Test
    void registryRoundTripsThroughXml() throws Exception {
        SignalRegistry written = new SignalRegistry("Main kernel");
        written.addSignalType("payment", new EchoComponent("payments", "payment"));
        written.addEntry("audit", null, "audit-kernel");

        Path file = dir.resolve("registries").resolve("Main_kernel.xml");
        FileManager fm = FileManager.getInstance(dir.toFile());
        fm.writeFileContent(written, file);
        SignalRegistry read = fm.getFileContent(file);

        assertEquals("Main kernel", read.getKernelName());
        assertEquals(written.getSignalTypeEntries(), read.getSignalTypeEntries());
    }

    @Test
    void producesTheSameXmlShapeAsVersion1() throws Exception {
        // Registries written by 1.0.1 (JAXB) must stay readable: same element and attribute names.
        SignalRegistry registry = new SignalRegistry("k");
        registry.addSignalType("payment", new EchoComponent("payments", "payment"));
        Path file = dir.resolve("k.xml");
        FileManager.getInstance(dir.toFile()).writeFileContent(registry, file);

        String xml = Files.readString(file);
        assertTrue(xml.contains("<signalRegistry>"), xml);
        assertTrue(xml.contains("<Kernel>k</Kernel>"), xml);
        assertTrue(xml.contains("<SignalTypeEntries"), xml);
        assertTrue(xml.contains("componentName=\"payments\""), xml);
        assertTrue(xml.contains("type=\"payment\""), xml);
    }

    @Test
    void readsARegistryWrittenByVersion1() throws Exception {
        String jaxbOutput = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<signalRegistry>\n"
                + "    <Kernel>Default rkernel</Kernel>\n"
                + "    <SignalTypeEntries type=\"exception_logging\" componentName=\"logger\"/>\n"
                + "    <SignalTypeEntries type=\"ping\" kernelName=\"secondary\"/>\n"
                + "</signalRegistry>\n";
        Path file = dir.resolve("old.xml");
        Files.writeString(file, jaxbOutput);

        SignalRegistry read = FileManager.getInstance(dir.toFile()).getFileContent(file);
        assertEquals("Default rkernel", read.getKernelName());
        assertEquals("logger", read.getTypeEntry("exception_logging").getComponentName());
        assertEquals("secondary", read.getTypeEntry("ping").getKernelName());
    }

    @Test
    void aMissingOrEmptyFileIsAnEmptyRegistryNotNull() throws Exception {
        FileManager fm = FileManager.getInstance(dir.toFile());
        assertNotNull(fm.getFileContent(dir.resolve("absent.xml")));
        Path empty = Files.createFile(dir.resolve("empty.xml"));
        assertTrue(fm.getFileContent(empty).getSignalTypeEntries().isEmpty());
    }

    @Test
    void oneInstancePerFolder() throws Exception {
        File a = Files.createDirectories(dir.resolve("a")).toFile();
        File b = Files.createDirectories(dir.resolve("b")).toFile();
        assertSame(FileManager.getInstance(a), FileManager.getInstance(a));
        assertNotSame(FileManager.getInstance(a), FileManager.getInstance(b));
    }

    @Test
    void refusesAFileWhereAFolderIsExpected() throws Exception {
        Path notAFolder = Files.createFile(dir.resolve("file.txt"));
        assertThrows(FileManagerException.class, () -> FileManager.getInstance(notAFolder.toFile()));
    }

    @Test
    void listsOnlyJars() throws Exception {
        Path folder = Files.createDirectories(dir.resolve("jars"));
        Files.createFile(folder.resolve("a.jar"));
        Files.createFile(folder.resolve("b.txt"));
        File[] jars = FileManager.getInstance(folder.toFile()).getFiles();
        assertEquals(1, jars.length);
        assertEquals("a.jar", jars[0].getName());
    }
}
