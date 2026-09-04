/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.signal;

import rkernel.IKernel;
import rkernel.component.IComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Maps each signal type to the single component or kernel that interprets it.
 *
 * <p>Registering a type that already exists replaces the previous interpreter:
 * a signal type has exactly one interpreter. Lookups ignore case.</p>
 *
 * <p>Plain object, no persistence concern: {@link rkernel.utils.file.FileManager}
 * reads and writes it as XML.</p>
 */
public class SignalRegistry {

    protected String kernelName;
    protected final List<SignalTypeEntry> signalTypeEntries = new ArrayList<>();

    public SignalRegistry() {
    }

    public SignalRegistry(String kernelName) {
        this.kernelName = kernelName;
    }

    public String getKernelName() {
        return kernelName;
    }

    public void setKernelName(String kernelName) {
        this.kernelName = kernelName;
    }

    /** Read-only view; mutate through {@link #addSignalType} and {@link #removeSignalType}. */
    public List<SignalTypeEntry> getSignalTypeEntries() {
        return Collections.unmodifiableList(signalTypeEntries);
    }

    public SignalTypeEntry getTypeEntry(String type) {
        for (SignalTypeEntry entry : signalTypeEntries) {
            if (entry.type.equalsIgnoreCase(type)) return entry;
        }
        return null;
    }

    public void addSignalType(String type, IComponent component) {
        addEntry(type, component.getName(), null);
    }

    public void addSignalType(String type, IKernel kernel) {
        addEntry(type, null, kernel.getName());
    }

    /** Adds (or replaces) an entry from raw values, as read from the XML file. */
    public void addEntry(String type, String componentName, String kernelName) {
        Objects.requireNonNull(type, "signal type");
        removeSignalType(type);
        SignalTypeEntry entry = new SignalTypeEntry();
        entry.type = type;
        entry.componentName = componentName;
        entry.kernelName = kernelName;
        signalTypeEntries.add(entry);
    }

    public void removeSignalType(String type) {
        signalTypeEntries.removeIf(entry -> entry.type.equalsIgnoreCase(type));
    }

    public static final class SignalTypeEntry {
        private String type;
        private String componentName;
        private String kernelName;

        public String getType() { return type; }
        public String getComponentName() { return componentName; }
        public String getKernelName() { return kernelName; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SignalTypeEntry entry = (SignalTypeEntry) o;
            return type.equalsIgnoreCase(entry.type)
                    && Objects.equals(componentName, entry.componentName)
                    && Objects.equals(kernelName, entry.kernelName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type.toLowerCase(), componentName, kernelName);
        }

        @Override
        public String toString() {
            return "SignalTypeEntry{type=" + type
                    + (componentName != null ? ", component=" + componentName : "")
                    + (kernelName != null ? ", kernel=" + kernelName : "") + "}";
        }
    }

    @Override
    public String toString() {
        return "SignalRegistry{kernel=" + kernelName + ", entries=" + signalTypeEntries + "}";
    }
}
