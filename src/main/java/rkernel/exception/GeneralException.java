/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.exception;

public class GeneralException extends Exception {
    protected int code;

    public GeneralException(int code, String message) {
        super(message);
        this.code = code;
    }

    public GeneralException(String message) {
        super(message);
        this.code = 500;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
