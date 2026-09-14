/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.signal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rkernel.BasicKernel;
import rkernel.support.EchoComponent;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SignalManagerTest {

    @TempDir
    Path root;

    private BasicKernel kernel(String name) {
        return new BasicKernel.Builder().setName(name).setDocumentRoot(root.toFile()).build();
    }

    @Test
    void createsTheRegistryFileUnderRegistries() {
        BasicKernel kernel = kernel("Main kernel");
        Path expected = root.resolve("registries").resolve("Main_kernel.xml");
        assertEquals(expected, ((SignalManager) kernel.getSignalManager()).getRegistryPath());
        assertTrue(Files.exists(expected));
    }

    @Test
    void routingSurvivesARestart() throws Exception {
        BasicKernel first = kernel("k");
        EchoComponent payments = new EchoComponent("payments", "payment");
        first.getComponents().put("payments", payments);
        first.getSignalManager().addSignalType("payment", payments);

        // A new kernel with the same name and root reads the registry back from disk.
        BasicKernel restarted = kernel("k");
        restarted.getComponents().put("payments", payments);
        assertSame(payments, restarted.getSignalManager().findInterpreter("payment"));
    }

    @Test
    void twoKernelsInTheSameJvmDoNotShareARegistry() throws Exception {
        BasicKernel a = kernel("a");
        BasicKernel b = kernel("b");
        a.getSignalManager().addSignalType("only-a", new EchoComponent("ca", "only-a"));

        assertNotNull(a.getSignalManager().getRegistryEntry("only-a"));
        assertNull(b.getSignalManager().getRegistryEntry("only-a"));
    }

    @Test
    void anUnknownTypeHasNoInterpreter() {
        assertNull(kernel("k").getSignalManager().findInterpreter("nothing"));
    }

    @Test
    void removingATypeIsPersisted() throws Exception {
        BasicKernel kernel = kernel("k");
        kernel.getSignalManager().addSignalType("tmp", new EchoComponent("c", "tmp"));
        kernel.getSignalManager().removeSignalType("tmp");

        assertNull(kernel("k").getSignalManager().getRegistryEntry("tmp"));
    }
}
