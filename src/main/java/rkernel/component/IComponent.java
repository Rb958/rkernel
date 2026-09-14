/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.component;

import rkernel.IKernel;
import rkernel.signal.BasicSignal;
import rkernel.signal.SignalListener;

import java.util.Collection;

/**
 * A module grafted onto a kernel. Packaged as a JAR dropped in the kernel's
 * {@code components/<kernel name>/} folder, it is loaded by reflection and
 * declares the signal types it interprets.
 */
public interface IComponent {

    void load(IKernel kernel);

    String getName();

    Collection<BasicSignal<?>> getSignals();

    void addSignalListener(SignalListener signalListener);

    boolean isRunning();

    void stop();

    /** The signal types this component interprets. */
    Collection<String> getSignalTypes();

    /** @deprecated typo kept for 1.0.x binary compatibility; use {@link #getSignalTypes()}. */
    @Deprecated
    default Collection<String> getSIgnalType() {
        return getSignalTypes();
    }

    Object processSignal(BasicSignal<?> signal);
}
