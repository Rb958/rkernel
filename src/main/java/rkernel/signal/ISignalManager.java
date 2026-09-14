/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.signal;

import rkernel.IKernel;
import rkernel.component.IComponent;
import rkernel.exception.SignalRegistryException;

public interface ISignalManager {

    /** The component or kernel registered for this signal type, or {@code null}. */
    Object findInterpreter(String type);

    void addSignalType(String type, IComponent component) throws SignalRegistryException;

    void addSignalType(String type, IKernel kernel) throws SignalRegistryException;

    void removeSignalType(String type) throws SignalRegistryException;

    void setKernel(IKernel kernel);

    SignalRegistry.SignalTypeEntry getRegistryEntry(String type);
}
