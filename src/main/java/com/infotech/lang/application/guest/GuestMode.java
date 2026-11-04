package com.infotech.lang.application.guest;

import com.infotech.lang.application.assessment.BlockEngine;

/** The block rule for the person on this request: the usual one, or a single question for a guest. */
public final class GuestMode {

    private GuestMode() {
    }

    public static BlockEngine engine() {
        return GuestAccount.isGuestNow() ? single(BlockEngine.PASS_RATIO) : new BlockEngine();
    }

    public static BlockEngine engine(double passRatio) {
        return GuestAccount.isGuestNow() ? single(passRatio) : new BlockEngine(BlockEngine.FIRST_ROUND, BlockEngine.EXTRA_ROUND, passRatio, false);
    }

    /** One question; the module is passed when it is answered correctly. */
    static BlockEngine single(double passRatio) {
        return new BlockEngine(1, 0, passRatio, false);
    }
}
