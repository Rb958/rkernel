/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.signal;

import org.junit.jupiter.api.Test;
import rkernel.support.EchoComponent;

import static org.junit.jupiter.api.Assertions.*;

class SignalRegistryTest {

    @Test
    void registersAComponentForAType() {
        SignalRegistry registry = new SignalRegistry("k");
        registry.addSignalType("payment", new EchoComponent("payments", "payment"));

        SignalRegistry.SignalTypeEntry entry = registry.getTypeEntry("payment");
        assertNotNull(entry);
        assertEquals("payments", entry.getComponentName());
        assertNull(entry.getKernelName());
    }

    @Test
    void aTypeHasExactlyOneInterpreter_registeringAgainReplaces() {
        SignalRegistry registry = new SignalRegistry("k");
        registry.addSignalType("payment", new EchoComponent("first", "payment"));
        registry.addSignalType("payment", new EchoComponent("second", "payment"));

        assertEquals(1, registry.getSignalTypeEntries().size());
        assertEquals("second", registry.getTypeEntry("payment").getComponentName());
    }

    @Test
    void lookupIgnoresCase() {
        SignalRegistry registry = new SignalRegistry("k");
        registry.addSignalType("Payment", new EchoComponent("payments", "Payment"));

        assertNotNull(registry.getTypeEntry("PAYMENT"));
        registry.removeSignalType("payment");
        assertNull(registry.getTypeEntry("Payment"));
    }

    @Test
    void removingAnUnknownTypeIsNotAnError() {
        SignalRegistry registry = new SignalRegistry("k");
        registry.removeSignalType("nothing");
        assertTrue(registry.getSignalTypeEntries().isEmpty());
    }

    @Test
    void entriesAreNotMutableFromOutside() {
        SignalRegistry registry = new SignalRegistry("k");
        assertThrows(UnsupportedOperationException.class, () -> registry.getSignalTypeEntries().clear());
    }

    @Test
    void aTypeIsRequired() {
        SignalRegistry registry = new SignalRegistry("k");
        assertThrows(NullPointerException.class, () -> registry.addEntry(null, "c", null));
    }
}
