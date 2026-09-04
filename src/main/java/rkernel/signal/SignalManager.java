/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.signal;

import rkernel.IKernel;
import rkernel.component.IComponent;
import rkernel.exception.FileManagerException;
import rkernel.exception.SignalRegistryException;
import rkernel.utils.file.FileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Routes signal types to their interpreter and keeps the registry on disk, in
 * {@code <documentRoot>/registries/<kernel name>.xml}, so that a restarted
 * kernel finds its components again without reloading every JAR first.
 *
 * <p>The registry used to be a {@code static} field shared by every kernel of
 * the JVM: two kernels in the same process overwrote each other's routing
 * table. It is now an instance field.</p>
 */
public class SignalManager implements ISignalManager {

    protected SignalRegistry registry;
    protected IKernel kernel;

    protected final File documentRoot;

    public SignalManager(IKernel kernel) {
        this(kernel, new File("."));
    }

    /** @param documentRoot folder under which {@code registries/} is created. */
    public SignalManager(IKernel kernel, File documentRoot) {
        this.kernel = kernel;
        this.documentRoot = documentRoot;
        try {
            registry = FileManager.getInstance(documentRoot).getFileContent(getRegistryPath());
            if (registry.getKernelName() == null) {
                registry.setKernelName(kernel.getName());
                flush();
            }
        } catch (FileManagerException | IOException e) {
            registry = new SignalRegistry(kernel.getName());
            kernel.dispatchLogException(e);
        }
    }

    @Override
    public Object findInterpreter(String type) {
        SignalRegistry.SignalTypeEntry entry = registry.getTypeEntry(type);
        if (entry == null) return null;
        if (entry.getComponentName() != null && !entry.getComponentName().isEmpty()) {
            return kernel.findComponentByName(entry.getComponentName());
        }
        if (entry.getKernelName() != null && !entry.getKernelName().isEmpty()) {
            return kernel.findKernelByName(entry.getKernelName());
        }
        return null;
    }

    @Override
    public void setKernel(IKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public SignalRegistry.SignalTypeEntry getRegistryEntry(String type) {
        return registry.getTypeEntry(type);
    }

    public SignalRegistry getRegistry() {
        return registry;
    }

    public Path getRegistryPath() {
        String fileName = kernel.getName().replace(' ', '_') + ".xml";
        return documentRoot.toPath().resolve("registries").resolve(fileName);
    }

    @Override
    public void addSignalType(String type, IComponent component) throws SignalRegistryException {
        registry.addSignalType(type, component);
        flushOrThrow();
    }

    @Override
    public void addSignalType(String type, IKernel kernel) throws SignalRegistryException {
        registry.addSignalType(type, kernel);
        flushOrThrow();
    }

    @Override
    public void removeSignalType(String type) throws SignalRegistryException {
        registry.removeSignalType(type);
        flushOrThrow();
    }

    private void flush() throws FileManagerException, IOException {
        FileManager.getInstance(documentRoot).writeFileContent(registry, getRegistryPath());
    }

    private void flushOrThrow() throws SignalRegistryException {
        try {
            flush();
        } catch (FileManagerException | IOException e) {
            throw new SignalRegistryException(e.getMessage());
        }
    }
}
