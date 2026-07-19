package com.dong.dongpicturebackendcollaborationservice;

import com.dong.dongpicturebackendcollaborationservice.collab.engine.LamportClock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LamportClockTest {

    @Test
    void tickShouldIncrementClock() {
        LamportClock clock = new LamportClock();
        assertEquals(1, clock.tick());
        assertEquals(2, clock.tick());
        assertEquals(3, clock.tick());
    }

    @Test
    void updateShouldTakeMaxPlusOne() {
        LamportClock clock = new LamportClock(5);
        // remote clock = 10, local = 5 → max = 10 → 10 + 1 = 11
        long result = clock.update(10);
        assertEquals(11, result);
    }

    @Test
    void updateShouldIgnoreSmallerClock() {
        LamportClock clock = new LamportClock(20);
        long result = clock.update(5);
        assertEquals(21, result); // max(20, 5) + 1 = 21
    }

    @Test
    void currentShouldNotIncrement() {
        LamportClock clock = new LamportClock(10);
        assertEquals(10, clock.current());
        assertEquals(10, clock.current());
    }

    @Test
    void happensBeforeLowerClockIsTrue() {
        assertTrue(LamportClock.happensBefore(1, "A", 2, "B"));
    }

    @Test
    void happensBeforeHigherClockIsFalse() {
        assertFalse(LamportClock.happensBefore(5, "A", 3, "B"));
    }

    @Test
    void happensBeforeSameClockTieBreaksByClientId() {
        // a=5, A vs a=5, B → A < B alphabetically → true
        assertTrue(LamportClock.happensBefore(5, "A", 5, "B"));
        // a=5, B vs a=5, A → B > A → false
        assertFalse(LamportClock.happensBefore(5, "B", 5, "A"));
    }

    @Test
    void concurrency() throws Exception {
        LamportClock clock = new LamportClock();
        int numThreads = 10;
        int ticksPerThread = 100;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < ticksPerThread; j++) {
                    clock.tick();
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }

        assertEquals(numThreads * ticksPerThread, clock.current());
    }
}
