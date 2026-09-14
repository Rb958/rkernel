/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.signal;

import java.util.EventObject;

public class SignalEvent<T> extends EventObject {
    public SignalEvent(BasicSignal<T> source) {
        super(source);
    }

    public BasicSignal<?> getSignal(){
        return (BasicSignal<?>) getSource();
    }
}
