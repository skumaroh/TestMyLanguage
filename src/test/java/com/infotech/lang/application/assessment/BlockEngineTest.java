package com.infotech.lang.application.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BlockEngineTest {

    private static BlockEngine answered(boolean... answers) {
        BlockEngine engine = new BlockEngine();
        for (boolean answer : answers) {
            engine.record(answer);
        }
        return engine;
    }

    @Test
    void neededAnswersAre3Of4And6Of8() {
        BlockEngine engine = new BlockEngine();
        assertEquals(3, engine.neededFor(4));
        assertEquals(6, engine.neededFor(8));
    }

    @Test
    void fourOfFourPassesAndEndsAtFour() {
        BlockEngine engine = answered(true, true, true, true);
        assertTrue(engine.isComplete());
        assertTrue(engine.passed());
        assertEquals(4, engine.asked());
    }

    @Test
    void threeOfFourPassesAndEndsAtFour() {
        BlockEngine engine = answered(true, false, true, true);
        assertTrue(engine.isComplete());
        assertTrue(engine.passed());
    }

    @Test
    void twoOfFourAsksFourMore() {
        BlockEngine engine = answered(true, false, true, false);
        assertFalse(engine.isComplete());
        assertFalse(engine.passed());
    }

    @Test
    void twoOfFourThenAllFourExtraPassesAtSixOfEight() {
        BlockEngine engine = answered(true, false, true, false, true, true, true, true);
        assertTrue(engine.isComplete());
        assertTrue(engine.passed());
        assertEquals(8, engine.asked());
    }

    @Test
    void fiveOfEightFailsButEndsAtEight() {
        BlockEngine engine = answered(true, false, true, false, true, true, false, false);
        assertTrue(engine.isComplete());
        assertFalse(engine.passed());
        assertEquals(8, engine.asked());
    }

    @Test
    void oneOfFourStillAsksFourMoreByDefault() {
        BlockEngine engine = answered(true, false, false, false);
        assertFalse(engine.isComplete());
    }

    @Test
    void oneOfFourStopsEarlyWhenConfiguredBecauseSixOfEightIsOutOfReach() {
        BlockEngine engine = new BlockEngine(4, 4, 0.75, true);
        for (boolean a : new boolean[] {true, false, false, false}) {
            engine.record(a);
        }
        assertTrue(engine.isComplete());
        assertFalse(engine.passed());
    }

    @Test
    void twoOfFourDoesNotStopEarlyBecauseSixOfEightIsStillPossible() {
        BlockEngine engine = new BlockEngine(4, 4, 0.75, true);
        for (boolean a : new boolean[] {true, true, false, false}) {
            engine.record(a);
        }
        assertFalse(engine.isComplete());
    }

    @Test
    void cannotRecordAfterComplete() {
        BlockEngine engine = answered(true, true, true, true);
        assertThrows(IllegalStateException.class, () -> engine.record(true));
    }

    @Test
    void levelPassesOnOverallPercentNotOnBlocks() {
        BlockEngine passed = answered(true, true, true, true);
        BlockEngine failed = answered(true, false, true, false, true, true, false, false); // 5 of 8
        List<BlockEngine> blocks = List.of(passed, failed);
        assertEquals(9, LevelScore.totalCorrect(blocks));
        assertEquals(12, LevelScore.totalAsked(blocks));
        assertTrue(LevelScore.passed(blocks)); // 9 of 12 = 75%
        assertFalse(failed.passed());
    }

    @Test
    void levelFailsBelowSeventyFivePercent() {
        BlockEngine failed = answered(true, false, true, false, true, true, false, false); // 5 of 8
        assertFalse(LevelScore.passed(List.of(failed)));
        assertEquals(62.5, LevelScore.percent(List.of(failed)), 0.001);
    }

    @Test
    void emptyLevelIsNotPassed() {
        assertFalse(LevelScore.passed(List.of()));
        assertEquals(0, LevelScore.percent(List.of()), 0.0);
    }
}
