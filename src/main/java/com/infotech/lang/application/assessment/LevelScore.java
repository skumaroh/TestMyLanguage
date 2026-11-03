package com.infotech.lang.application.assessment;

import java.util.List;

/** Level result (owner decision, 2026-11-01): the overall percentage across all questions asked decides; a failed block does not fail the level. */
public final class LevelScore {

    public static final double PASS_RATIO = 0.75;

    private LevelScore() {
    }

    public static int totalAsked(List<BlockEngine> blocks) {
        return blocks.stream().mapToInt(BlockEngine::asked).sum();
    }

    public static int totalCorrect(List<BlockEngine> blocks) {
        return blocks.stream().mapToInt(BlockEngine::correct).sum();
    }

    /** Overall percent, 0 to 100, or 0 when nothing was asked. */
    public static double percent(List<BlockEngine> blocks) {
        int asked = totalAsked(blocks);
        return asked == 0 ? 0 : 100.0 * totalCorrect(blocks) / asked;
    }

    public static boolean passed(List<BlockEngine> blocks) {
        int asked = totalAsked(blocks);
        return asked > 0 && totalCorrect(blocks) >= Math.ceil(PASS_RATIO * asked - 1e-9);
    }
}
