/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.exception;

public class UnImplementedMethod extends GeneralException{
    public UnImplementedMethod(){
        super(501, "Unimplemented method");
    }
}
