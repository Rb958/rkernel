/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel;

import rkernel.component.IComponentLoader;
import rkernel.exception.FileManagerException;
import rkernel.exception.SignalRegistryException;
import rkernel.utils.file.FileAdapter;
import rkernel.utils.file.FileEvent;
import rkernel.utils.file.FileManager;
import rkernel.utils.file.FileWatcher;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads secondary kernels from JARs and attaches them to the default kernel,
 * routing their signal types through it.
 */
public class BasicKernelLoader implements IComponentLoader<IKernel> {
    protected final Map<String, IKernel> kernels = new ConcurrentHashMap<>();
    protected IKernel kernel;

    @Override
    public void loadComponents(File folder) {
        try {
            for (File file : FileManager.getInstance(folder).getFiles()) {
                loadOne(file);
            }
        } catch (FileManagerException e) {
            kernel.dispatchLogException(e);
        }
    }

    /** Instantiates the kernel of a JAR and attaches it. Returns it, or {@code null}. */
    public IKernel loadOne(File file) {
        try {
            Class<?> kernelClass = loadSingleFile(file, IKernel.class);
            if (kernelClass == null) {
                return null;
            }
            IKernel loaded = (IKernel) kernelClass.getConstructor().newInstance();
            if (loaded.isDefault()) {
                return null; // a JAR must not bring a second default kernel
            }
            for (String signalType : loaded.getSignalType()) {
                try {
                    kernel.getSignalManager().addSignalType(signalType, loaded);
                } catch (SignalRegistryException e) {
                    kernel.dispatchLogException(e);
                }
            }
            kernels.put(loaded.getName(), loaded);
            kernel.addKernel(loaded);
            return loaded;
        } catch (ReflectiveOperationException | java.io.IOException | RuntimeException e) {
            kernel.dispatchLogException(e);
            return null;
        }
    }

    /** @deprecated use {@link #loadOne(File)}; kept for subclasses of 1.0. */
    @Deprecated
    protected void executeClass(File file) {
        loadOne(file);
    }

    @Override
    public Map<String, IKernel> getComponents() {
        return kernels;
    }

    @Override
    public void setKernel(IKernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void watch(File watchedDirectory) {
        new FileWatcher(watchedDirectory).addEventListener(new FileAdapter() {
            @Override
            public void onCreateFile(FileEvent event) {
                if (event.getFile().getName().endsWith(".jar")) loadOne(event.getFile());
            }
        }).watch();
    }
}
