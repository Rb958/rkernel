/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.signal.basic;

import rkernel.signal.BasicSignal;

public final class LoggingSignal extends BasicSignal<Exception> {
    public LoggingSignal(Exception payload) {
        super("exception_logging", payload);
    }
}
