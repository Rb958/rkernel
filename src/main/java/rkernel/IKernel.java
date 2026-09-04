/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel;

import rkernel.component.IComponent;
import rkernel.exception.UnImplementedMethod;
import rkernel.signal.BasicSignal;
import rkernel.signal.ISignalManager;

import java.util.Collection;
import java.util.Map;

/**
 * @author Richie AKAWA richiebayless@gmail.com
 * @version 1.1
 * @since 1.0
 */
public interface IKernel {

    /**
     *
     * @return The name of the rkernel
     */
    String getName();

    /**
     * Load
     * @throws UnImplementedMethod Exception thrown when this method was not implemented
     */
    default void load() throws UnImplementedMethod {
        throw new UnImplementedMethod();
    }

    /**
     * Get all components related to the current rkernel
     * All these related component only interact with the current. if the component need to interact with another component,
     * the component have to make a signal to the rkernel
     * @see IKernel
     * @return A collection of Components related to this rkernel
     */
    Map<String, IComponent> getComponents();

    /**
     * Dispatch a signal to all the rkernel which are related to this rkernel.
     * @param signal Instance of Signal
     * @return Collection of kernels which the signal has been sent to
     * @throws UnImplementedMethod Exception thrown when this method was not implemented
     */
    default Collection<IKernel> dispatchSignal(BasicSignal<?> signal) throws UnImplementedMethod {
        throw new UnImplementedMethod();
    }

    /**
     * Process the incoming signal
     * @param signal Instance of signal received
     * @return the interpreter's response, or {@code null} when no interpreter is registered
     */
    Object processSignal(BasicSignal<?> signal);

    /**
     * Map of Kernel which are related to the current rkernel
     * @return Collection of rkernel
     */
    Map<String, IKernel> getKernels();

    /**
     * Get the collection of signals which could be interpreted by the current rkernel
     * @return collection of Signal
     */
    Collection<String> getSignalType();

    /**
     * Check if the current rkernel has an interpreter for specific signal
     * @param signalType instance of the signal
     * @return true if the current rkernel has an interpreter and false else
     */
    Object getInterpreterOf(String signalType);

    /**
     * Find one Component by his name
     * @param componentName The component's name
     * @return an instance of Component
     * @see IComponent
     */
    IComponent findComponentByName(String componentName);

    /**
     * Find one Kernel by his name
     * @param kernelName The rkernel's Name
     * @return an Instance of Kernel
     * @see IKernel
     */
    IKernel findKernelByName(String kernelName);

    /**
     * Get an instance of Signal manager. this component it in charge to manage
     * all the signals between the current rkernel and other component (Kernel or Components)
     * @return an Instance of <strong>SignalManager</strong>
     * @see rkernel.signal.SignalManager
     */
    ISignalManager getSignalManager();

    /**
     * Create connection between the current rkernel and the provided rkernel
     * @param tmpkernel Instance of provided rkernel
     */
    void addKernel(IKernel tmpkernel);

    /**
     * Dispatch Exception to The component which are able to process and log the exception
     * @param e Instance of exception By default, it accept all Instance which inherit from {@link Exception}
     */
    void dispatchLogException(Exception e);

    /**
     * Check if The current rkernel is the default rkernel or
     * @return true if it's the default rkernel and false else
     */
    default boolean isDefault(){
        return getName() != null && getName().equalsIgnoreCase(BasicKernel.DEFAULT_NAME);
    }
}
