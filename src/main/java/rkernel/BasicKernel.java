/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel;

import rkernel.component.IComponent;
import rkernel.component.IComponentLoader;
import rkernel.signal.BasicSignal;
import rkernel.signal.ISignalManager;
import rkernel.signal.SignalManager;
import rkernel.signal.basic.LoggingSignal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default kernel. Components are JARs in {@code <documentRoot>/components/<kernel name>/},
 * secondary kernels are JARs in {@code <documentRoot>/}; both folders are watched
 * after the initial load.
 */
public class BasicKernel implements IKernel {

    public static final String DEFAULT_NAME = "Default rkernel";

    protected SignalManager signalManager;
    protected final IComponentLoader<IComponent> componentLoader;
    protected final IComponentLoader<IKernel> kernelLoader;
    protected final Map<String, IKernel> kernels;
    protected final Map<String, IComponent> components;
    protected final Collection<String> signals;
    protected final File documentRoot;

    protected final String kernelName;

    BasicKernel(Builder builder) {
        this.componentLoader = builder.componentLoader;
        this.kernelLoader = builder.kernelLoader;
        this.kernels = new ConcurrentHashMap<>(builder.kernels);
        this.signals = new ArrayList<>(builder.signals);
        this.components = new ConcurrentHashMap<>();
        this.kernelName = builder.name;
        this.documentRoot = builder.documentRoot;
        this.signalManager = new SignalManager(this, documentRoot);
    }

    @Override
    public void load() {
        try {
            Path componentPath = documentRoot.toPath().resolve("components").resolve(kernelName);
            if (Files.notExists(componentPath)) {
                Files.createDirectories(componentPath);
            }
            File componentFile = componentPath.toFile();
            if (componentLoader != null) {
                Thread t = new Thread(() -> {
                    componentLoader.loadComponents(componentFile);
                    componentLoader.watch(componentFile);
                }, kernelName + "-components");
                t.setDaemon(true);
                t.start();
            }
            if (kernelLoader != null) {
                Thread t = new Thread(() -> {
                    kernelLoader.loadComponents(documentRoot);
                    kernelLoader.watch(documentRoot);
                }, kernelName + "-kernels");
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            dispatchLogException(e);
        }
    }

    @Override
    public String getName() {
        return kernelName;
    }

    @Override
    public Map<String, IComponent> getComponents() {
        return components;
    }

    /**
     * Processes the signal here and returns the kernels it was actually handed
     * to: the attached kernel that interprets it, if that is where it went.
     * 1.0 always returned an empty list.
     */
    @Override
    public Collection<IKernel> dispatchSignal(BasicSignal<?> signal) {
        Object interpreter = getInterpreterOf(signal.getType());
        processSignal(signal);
        if (interpreter instanceof IKernel) {
            return Collections.singletonList((IKernel) interpreter);
        }
        return Collections.emptyList();
    }

    @Override
    public Object processSignal(BasicSignal<?> signal) {
        Object interpreter = getInterpreterOf(signal.getType());
        if (interpreter instanceof IComponent) {
            return ((IComponent) interpreter).processSignal(signal);
        }
        if (interpreter instanceof IKernel) {
            return ((IKernel) interpreter).processSignal(signal);
        }
        return null;
    }

    @Override
    public Map<String, IKernel> getKernels() {
        return kernels;
    }

    @Override
    public Collection<String> getSignalType() {
        return signals;
    }

    @Override
    public Object getInterpreterOf(String signalType) {
        return signalManager.findInterpreter(signalType);
    }

    @Override
    public IComponent findComponentByName(String componentName) {
        return components.get(componentName);
    }

    @Override
    public IKernel findKernelByName(String kernelName) {
        return kernels.get(kernelName);
    }

    @Override
    public ISignalManager getSignalManager() {
        return signalManager;
    }

    @Override
    public void addKernel(IKernel kernel) {
        kernels.put(kernel.getName(), kernel);
    }

    /**
     * Hands the exception to whichever component interprets
     * {@code exception_logging}. When none is registered the exception is
     * dropped silently — a logging component is the first one to graft.
     */
    @Override
    public void dispatchLogException(Exception e) {
        dispatchSignal(new LoggingSignal(e));
    }

    public File getDocumentRoot() {
        return documentRoot;
    }

    public static final class Builder {
        private IComponentLoader<IComponent> componentLoader;
        private IComponentLoader<IKernel> kernelLoader;
        private final Map<String, IKernel> kernels = new ConcurrentHashMap<>();
        private final List<String> signals = new ArrayList<>();
        private String name = DEFAULT_NAME;
        private File documentRoot = new File(".");

        public Builder setComponentLoader(IComponentLoader<IComponent> componentLoader) {
            this.componentLoader = componentLoader;
            return this;
        }

        public Builder setKernelLoader(IComponentLoader<IKernel> kernelLoader) {
            this.kernelLoader = kernelLoader;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /** Folder holding {@code components/} and {@code registries/}. Default: the working directory. */
        public Builder setDocumentRoot(File documentRoot) {
            this.documentRoot = documentRoot;
            return this;
        }

        /** Signal types this kernel itself interprets. */
        public Builder addSignalType(String signalType) {
            this.signals.add(signalType);
            return this;
        }

        /**
         * Builds the kernel. Loaders are optional: 1.0 dereferenced
         * {@code componentLoader} unconditionally and threw
         * {@code NullPointerException} on a kernel built without one.
         */
        public BasicKernel build() {
            BasicKernel kernel = new BasicKernel(this);
            if (componentLoader != null) componentLoader.setKernel(kernel);
            if (kernelLoader != null) kernelLoader.setKernel(kernel);
            return kernel;
        }
    }
}
