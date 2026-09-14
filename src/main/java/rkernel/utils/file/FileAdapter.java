/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.utils.file;

public abstract class FileAdapter implements FileListener{
    @Override
    public void onCreateFile(FileEvent event) {}

    @Override
    public void onDeleteFile(FileEvent event) {}

    @Override
    public void onModifyFile(FileEvent event) {}
}
