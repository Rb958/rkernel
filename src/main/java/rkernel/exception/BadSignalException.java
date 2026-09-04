/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.exception;

public class BadSignalException extends GeneralException {
    public BadSignalException(int code, String message) {
        super(code, message);
    }
}
