/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.component;

import rkernel.IKernel;
import rkernel.exception.SignalRegistryException;
import rkernel.utils.file.FileAdapter;
import rkernel.utils.file.FileEvent;
import rkernel.utils.file.FileWatcher;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads components from the JARs of a folder, registers their signal types with
 * the kernel, and keeps watching the folder: a JAR dropped in is loaded, a JAR
 * removed is unregistered — without restarting the application.
 */
public class BasicComponentLoader implements IComponentLoader<IComponent> {

    protected final Map<String, IComponent> components = new ConcurrentHashMap<>();
    /** JAR file name → component name, so a deleted JAR can still be unregistered. */
    protected final Map<String, String> origins = new ConcurrentHashMap<>();
    protected IKernel defaultKernel;

    @Override
    public void setKernel(IKernel defaultKernel) {
        this.defaultKernel = defaultKernel;
    }

    @Override
    public void loadComponents(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files != null) {
            for (File file : files) {
                loadOne(file);
            }
        }
    }

    /** Instantiates the component of a JAR and wires it to the kernel. Returns it, or {@code null}. */
    public IComponent loadOne(File file) {
        try {
            Class<?> componentClass = loadSingleFile(file, IComponent.class);
            if (componentClass == null) {
                return null;
            }
            IComponent component = (IComponent) componentClass.getConstructor().newInstance();
            for (String signalType : component.getSignalTypes()) {
                try {
                    defaultKernel.getSignalManager().addSignalType(signalType, component);
                } catch (SignalRegistryException e) {
                    defaultKernel.dispatchLogException(e);
                }
            }
            components.put(component.getName(), component);
            origins.put(file.getName(), component.getName());
            defaultKernel.getComponents().put(component.getName(), component);
            component.load(defaultKernel);
            return component;
        } catch (ReflectiveOperationException | java.io.IOException | RuntimeException e) {
            defaultKernel.dispatchLogException(e);
            return null;
        }
    }

    /** @deprecated use {@link #loadOne(File)}; kept for subclasses of 1.0. */
    @Deprecated
    protected void executeClass(File file) {
        loadOne(file);
    }

    @Override
    public void watch(File watchedDirectory) {
        FileWatcher watcher = new FileWatcher(watchedDirectory);
        watcher.addEventListener(new FileAdapter() {
            @Override
            public void onCreateFile(FileEvent event) {
                if (event.getFile().getName().endsWith(".jar")) loadOne(event.getFile());
            }

            @Override
            public void onDeleteFile(FileEvent event) {
                wipeComponent(event.getFile());
            }
        }).watch();
    }

    /**
     * Unregisters the component that came from this JAR. The JAR is gone, so it
     * cannot be re-read: the component is found by the file name it was loaded
     * from, tracked in {@link #origins}.
     */
    protected void wipeComponent(File file) {
        String name = origins.remove(file.getName());
        if (name == null) return;
        IComponent component = components.remove(name);
        if (component == null) return;
        Collection<String> signalTypes = component.getSignalTypes();
        for (String signalType : signalTypes) {
            try {
                defaultKernel.getSignalManager().removeSignalType(signalType);
            } catch (SignalRegistryException e) {
                defaultKernel.dispatchLogException(e);
            }
        }
        defaultKernel.getComponents().remove(name);
        component.stop();
    }

    @Override
    public Map<String, IComponent> getComponents() {
        return components;
    }
}
