/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.support;

import rkernel.IKernel;
import rkernel.component.IComponent;
import rkernel.signal.BasicSignal;
import rkernel.signal.SignalListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A component that answers every signal with its payload, and records what it saw. */
public class EchoComponent implements IComponent {
    private final String name;
    private final List<String> types;
    private IKernel kernel;
    private boolean running;
    public final List<BasicSignal<?>> received = new ArrayList<>();

    public EchoComponent() {
        this("echo", "echo");
    }

    public EchoComponent(String name, String... types) {
        this.name = name;
        this.types = List.of(types);
    }

    @Override public void load(IKernel kernel) { this.kernel = kernel; this.running = true; }
    @Override public String getName() { return name; }
    @Override public Collection<BasicSignal<?>> getSignals() { return List.of(); }
    @Override public void addSignalListener(SignalListener signalListener) { }
    @Override public boolean isRunning() { return running; }
    @Override public void stop() { running = false; }
    @Override public Collection<String> getSignalTypes() { return types; }
    @Override public Object processSignal(BasicSignal<?> signal) {
        received.add(signal);
        return signal.getPayload();
    }
    public IKernel getKernel() { return kernel; }
}
