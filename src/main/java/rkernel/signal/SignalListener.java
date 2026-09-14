/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.signal;

import java.util.EventListener;

public interface SignalListener extends EventListener {
    void onProcessIncoming(SignalEvent<?> signalEvent);
}
