/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.utils.file;

import java.io.File;
import java.util.EventObject;

public class FileEvent extends EventObject {
    public FileEvent(File source) {
        super(source);
    }

    public File getFile(){
        return (File) getSource();
    }
}
