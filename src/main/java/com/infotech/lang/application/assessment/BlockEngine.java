package com.infotech.lang.application.assessment;

/**
 * The rule for one block of questions (owner decisions, 2026-11-01):
 * ask {@code firstRound} questions; if at least 75% are correct the block is passed and ends;
 * otherwise ask {@code extraRound} more (so 8 in total) and the block passes only at 75% of all asked.
 * The student moves on after the last question whatever the result. Java owns this decision; no model is involved.
 */
public final class BlockEngine {

    public static final int FIRST_ROUND = 4;
    public static final int EXTRA_ROUND = 4;
    public static final double PASS_RATIO = 0.75;

    private final int firstRound;
    private final int extraRound;
    private final double passRatio;
    private final boolean stopEarlyWhenImpossible;

    private int asked;
    private int correct;

    public BlockEngine() {
        this(FIRST_ROUND, EXTRA_ROUND, PASS_RATIO, false);
    }

    public BlockEngine(int firstRound, int extraRound, double passRatio, boolean stopEarlyWhenImpossible) {
        if (firstRound < 1 || extraRound < 0 || passRatio <= 0 || passRatio > 1) {
            throw new IllegalArgumentException("invalid block settings");
        }
        this.firstRound = firstRound;
        this.extraRound = extraRound;
        this.passRatio = passRatio;
        this.stopEarlyWhenImpossible = stopEarlyWhenImpossible;
    }

    /** Records the answer to the current question: 1 point when correct, 0 when not. */
    public void record(boolean answeredCorrectly) {
        if (isComplete()) {
            throw new IllegalStateException("block is already complete");
        }
        asked++;
        if (answeredCorrectly) {
            correct++;
        }
    }

    public int asked() {
        return asked;
    }

    public int correct() {
        return correct;
    }

    /** The most questions this block can ask (first round plus the extra round). */
    public int maxQuestions() {
        return firstRound + extraRound;
    }

    /** Correct answers needed for {@code total} questions to reach the pass ratio (3 of 4, 6 of 8). */
    public int neededFor(int total) {
        return (int) Math.ceil(passRatio * total - 1e-9);
    }

    public boolean isComplete() {
        if (asked < firstRound) {
            return false;
        }
        if (asked == firstRound) {
            if (correct >= neededFor(firstRound)) {
                return true;
            }
            return extraRound == 0 || (stopEarlyWhenImpossible && correct + extraRound < neededFor(maxQuestions()));
        }
        return asked >= maxQuestions();
    }

    /** True once the block is complete and enough answers were correct for the questions asked. */
    public boolean passed() {
        return isComplete() && correct >= neededFor(asked);
    }
}
