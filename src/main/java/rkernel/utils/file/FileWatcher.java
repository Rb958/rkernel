/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.utils.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.*;

public class FileWatcher implements Runnable{

    protected List<FileListener> listeners = new ArrayList<>();
    protected File target;

    protected List<WatchService> watchServices = new ArrayList<>();

    public FileWatcher(File folder){
        this.target = folder;
    }

    public void watch(){
        if (target.exists()) {
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public List<FileListener> getListeners() {
        return listeners;
    }

    public FileWatcher setListeners(List<FileListener> listeners) {
        this.listeners = listeners;
        return this;
    }

    public File getFolder() {
        return target;
    }

    public FileWatcher setFolder(File folder) {
        this.target = folder;
        return this;
    }

    @Override
    public void run() {
        try(WatchService watchService = FileSystems.getDefault().newWatchService()){
            Path targetPath = Paths.get(target.getAbsolutePath());
            targetPath.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchServices.add(watchService);
            boolean poll = true;

            while (poll){
                poll = pollEvent(watchService);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    protected boolean pollEvent(WatchService watchService) throws InterruptedException {
        WatchKey key = watchService.take();
        Path path = (Path) key.watchable();

        for (WatchEvent<?> event : key.pollEvents()){
            notifyListeners(event.kind(), path.resolve((Path) event.context()).toFile());
        }
        return key.reset();
    }

    private void notifyListeners(WatchEvent.Kind<?> kind, File file) {
        FileEvent fileEvent = new FileEvent(file);
        if (kind == ENTRY_CREATE){
            for (FileListener listener : listeners){
                listener.onCreateFile(fileEvent);
            }
            if (file.isDirectory()){
                new FileWatcher(file).setListeners(listeners).watch();
            }
        }else if (kind == ENTRY_DELETE){
            for (FileListener listener : listeners){
                listener.onDeleteFile(fileEvent);
            }
        }else if (kind == ENTRY_MODIFY){
            for (FileListener listener : listeners){
                listener.onModifyFile(fileEvent);
            }
        }
    }

    public FileWatcher addEventListener(FileListener fileListener){
        this.listeners.add(fileListener);
        return this;
    }

    public List<WatchService> getWatchServices() {
        return Collections.unmodifiableList(watchServices);
    }
}
