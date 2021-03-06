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
import java.nio.file.Paths;
import java.util.*;

public class BasicKernel implements IKernel{

    protected SignalManager signalManager;
    protected final IComponentLoader<IComponent> componentLoader;
    protected final IComponentLoader<IKernel> kernelLoader;
    protected final Map<String, IKernel> kernels;
    protected final Map<String, IComponent> components;
    protected final Collection<String> signals;

    protected final String kernelName;

    BasicKernel(Builder builder) {
        this.componentLoader = builder.getComponentLoader();
        this.kernelLoader = builder.getKernelLoader();
        this.kernels = builder.getKernels();
        this.signals = builder.getSignals();
        this.components = new HashMap<>();
        this.kernelName = builder.name;
    }

    @Override
    public void load() {
        try {
            signalManager = new SignalManager(this);
            File file = Paths.get(".").toFile();
            Path componentPath = Paths.get("components/".concat(kernelName));
            if (Files.notExists(componentPath))
                Files.createDirectories(componentPath);
            File componentFile = componentPath.toFile();
            if (componentLoader != null) {
                new Thread(() -> {
                    componentLoader.loadComponents(componentFile);
                    componentLoader.watch(componentFile);
                }).start();
            }
            if (kernelLoader != null) {
                new Thread(() -> {
                    kernelLoader.loadComponents(file);
                    kernelLoader.watch(file);
                }).start();
            }
        } catch (IOException e) {
            dispatchLogException(e);
        }
    }

    public String getName() {
        return kernelName;
    }

    @Override
    public Map<String, IComponent> getComponents() {
        return components;
    }

    @Override
    public Collection<IKernel> dispatchSignal(BasicSignal<?> signal) {
        List<IKernel> tmpKernel = new ArrayList<>();
        processSignal(signal);
        return tmpKernel;
    }

    @Override
    public Object processSignal(BasicSignal<?> signal) {
        Object response = null;
        // Find Interpreter
        Object interpreter = getInterpreterOf(signal.getType());
        // Call Interpreter with his data
        if (interpreter instanceof IComponent){
            response = ((IComponent) interpreter).processSignal(signal);
        }else if (interpreter instanceof IKernel){
            response = ((IKernel) interpreter).processSignal(signal);
        }
        return response;
    }

    @Override
    public Map<String,IKernel> getKernels() {
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
        return components.getOrDefault(componentName, null);
    }

    @Override
    public IKernel findKernelByName(String kernelName) {
        return kernels.getOrDefault(kernelName, null);
    }

    @Override
    public ISignalManager getSignalManager() {
        return signalManager;
    }

    @Override
    public void addKernel(IKernel kernel) {
        kernels.put(kernel.getName(), kernel);
    }

    @Override
    public void dispatchLogException(Exception e) {
        LoggingSignal loggingSignal = new LoggingSignal(e);
        this.dispatchSignal(loggingSignal);
    }

    public static final class Builder{
        private IComponentLoader<IComponent> componentLoader;
        private IComponentLoader<IKernel> kernelLoader;
        private final Map<String, IKernel> kernels;
        private final Collection<String> signals;
        private String name;

        public Builder() {
            this.componentLoader = null;
            this.kernelLoader = null;
            this.kernels = new HashMap<>();
            this.signals = new ArrayList<>();
            this.name = "Default rkernel";
        }

        IComponentLoader<IComponent> getComponentLoader() {
            return componentLoader;
        }

        public Builder setComponentLoader(IComponentLoader<IComponent> componentLoader) {
            this.componentLoader = componentLoader;
            return this;
        }

        IComponentLoader<IKernel> getKernelLoader() {
            return kernelLoader;
        }

        public Builder setKernelLoader(IComponentLoader<IKernel> kernelLoader) {
            this.kernelLoader = kernelLoader;
            return this;
        }

        public Builder setName(String name){
            this.name = name;
            return this;
        }

        Map<String, IKernel> getKernels() {
            return kernels;
        }

        Collection<String> getSignals() {
            return signals;
        }

        public BasicKernel build(){
            BasicKernel kernel = new BasicKernel(this);
            this.componentLoader.setKernel(kernel);
            return kernel;
        }
    }
}
