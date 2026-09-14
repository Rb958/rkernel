/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.utils.file;

import java.util.EventListener;

public interface FileListener extends EventListener {

    void onCreateFile(FileEvent event);

    void onDeleteFile(FileEvent event);

    void onModifyFile(FileEvent event);
}
