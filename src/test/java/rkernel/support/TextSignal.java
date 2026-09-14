/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.support;

import rkernel.signal.BasicSignal;

public class TextSignal extends BasicSignal<String> {
    public TextSignal(String type, String payload) {
        super(type, payload);
    }
}
