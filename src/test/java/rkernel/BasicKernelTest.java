/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import rkernel.signal.BasicSignal;
import rkernel.support.EchoComponent;
import rkernel.support.TextSignal;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BasicKernelTest {

    @TempDir
    Path root;

    private BasicKernel kernel() {
        return new BasicKernel.Builder().setName("test").setDocumentRoot(root.toFile()).build();
    }

    @Test
    void buildsWithoutAnyLoader() {
        // 1.0 threw NullPointerException here: build() dereferenced the component loader unconditionally.
        assertDoesNotThrow(this::kernel);
    }

    @Test
    void routesASignalToTheComponentRegisteredForItsType() throws Exception {
        BasicKernel kernel = kernel();
        EchoComponent payments = new EchoComponent("payments", "payment");
        kernel.getComponents().put(payments.getName(), payments);
        kernel.getSignalManager().addSignalType("payment", payments);

        Object response = kernel.processSignal(new TextSignal("payment", "25000 XAF"));

        assertEquals("25000 XAF", response);
        assertEquals(1, payments.received.size());
    }

    @Test
    void anUnroutedSignalReturnsNullInsteadOfFailing() {
        assertNull(kernel().processSignal(new TextSignal("unknown", "x")));
    }

    @Test
    void loggingAnExceptionWithoutALoggerIsHarmless() {
        assertDoesNotThrow(() -> kernel().dispatchLogException(new IllegalStateException("boom")));
    }

    @Test
    void loggingAnExceptionReachesTheLoggingComponent() throws Exception {
        BasicKernel kernel = kernel();
        EchoComponent logger = new EchoComponent("logger", "exception_logging");
        kernel.getComponents().put("logger", logger);
        kernel.getSignalManager().addSignalType("exception_logging", logger);

        kernel.dispatchLogException(new IllegalStateException("boom"));

        assertEquals(1, logger.received.size());
        assertEquals("boom", ((Exception) logger.received.get(0).getPayload()).getMessage());
    }

    @Test
    void dispatchReportsTheKernelASignalWasHandedTo() throws Exception {
        BasicKernel main = kernel();
        IKernel secondary = new StubKernel("secondary", "ping");
        main.addKernel(secondary);
        main.getSignalManager().addSignalType("ping", secondary);

        Collection<IKernel> reached = main.dispatchSignal(new TextSignal("ping", "?"));

        assertEquals(List.of(secondary), List.copyOf(reached));
    }

    @Test
    void theDefaultNameIsRecognised() {
        assertTrue(new BasicKernel.Builder().setDocumentRoot(root.toFile()).build().isDefault());
        assertFalse(kernel().isDefault());
    }

    /** The smallest possible secondary kernel. */
    private static final class StubKernel implements IKernel {
        private final String name;
        private final List<String> types;
        StubKernel(String name, String... types) { this.name = name; this.types = List.of(types); }
        @Override public String getName() { return name; }
        @Override public Map<String, rkernel.component.IComponent> getComponents() { return Map.of(); }
        @Override public Object processSignal(BasicSignal<?> signal) { return "pong"; }
        @Override public Map<String, IKernel> getKernels() { return Map.of(); }
        @Override public Collection<String> getSignalType() { return types; }
        @Override public Object getInterpreterOf(String signalType) { return null; }
        @Override public rkernel.component.IComponent findComponentByName(String n) { return null; }
        @Override public IKernel findKernelByName(String n) { return null; }
        @Override public rkernel.signal.ISignalManager getSignalManager() { return null; }
        @Override public void addKernel(IKernel k) { }
        @Override public void dispatchLogException(Exception e) { }
    }
}
