package com.dong.dongpicturebackendcollaborationservice;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.StateVector;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StateVectorTest {

    @Test
    void updateShouldTakeMaxValue() {
        StateVector sv = new StateVector();
        sv.update("clientA", 5);
        assertEquals(5, sv.get("clientA"));

        sv.update("clientA", 3); // smaller → ignored
        assertEquals(5, sv.get("clientA"));

        sv.update("clientA", 10); // larger → accepted
        assertEquals(10, sv.get("clientA"));
    }

    @Test
    void getDefaultIsZero() {
        StateVector sv = new StateVector();
        assertEquals(0, sv.get("unknown"));
    }

    @Test
    void computeMissingShouldReturnGaps() {
        StateVector server = new StateVector(Map.of(
                "A", 10L,
                "B", 5L,
                "C", 3L
        ));

        Map<String, Long> client = Map.of(
                "A", 5L,   // client missing A's operations 6..10
                "B", 5L,   // client has all of B's
                "C", 0L    // client has none of C's
        );

        Map<String, Long> missing = server.computeMissing(client);
        assertEquals(2, missing.size());
        assertEquals(6L, missing.get("A"));  // from seq 6
        assertEquals(1L, missing.get("C"));  // from seq 1
        assertNull(missing.get("B"));         // no missing
    }

    @Test
    void computeMissingWhenClientHasEverything() {
        StateVector server = new StateVector(Map.of("A", 10L));
        Map<String, Long> client = Map.of("A", 15L); // client is AHEAD of server
        Map<String, Long> missing = server.computeMissing(client);
        assertTrue(missing.isEmpty());
    }

    @Test
    void coversShouldReturnTrueWhenFullyCovered() {
        StateVector sv = new StateVector(Map.of("A", 10L, "B", 5L));
        assertTrue(sv.covers(Map.of("A", 5L, "B", 3L)));
        assertFalse(sv.covers(Map.of("A", 15L)));
        assertFalse(sv.covers(Map.of("C", 1L)));
    }

    @Test
    void mergeShouldTakeMaxPerClient() {
        StateVector sv1 = new StateVector(Map.of("A", 10L, "B", 5L));
        StateVector sv2 = new StateVector(Map.of("A", 5L, "B", 15L, "C", 3L));

        sv1.merge(sv2);
        assertEquals(10, sv1.get("A"));
        assertEquals(15, sv1.get("B"));
        assertEquals(3, sv1.get("C"));
    }

    @Test
    void snapshotShouldBeImmutableToInternalChanges() {
        StateVector sv = new StateVector(Map.of("A", 5L));
        Map<String, Long> snap = sv.snapshot();
        snap.put("A", 100L);
        assertEquals(5L, sv.get("A"));
    }

    @Test
    void removeShouldCleanup() {
        StateVector sv = new StateVector(Map.of("A", 5L, "B", 3L));
        sv.remove("A");
        assertEquals(0, sv.get("A"));
        assertEquals(3, sv.get("B"));
    }
}
