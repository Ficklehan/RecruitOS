package com.recruitos.common.evolution;

/**
 * PRD §6.1 六级进化信号
 */
public final class EvolutionSignalLevel {

    public static final int L1_OFFER = 1;
    public static final int L2_INTERVIEW = 2;
    public static final int L3_SCREEN = 3;
    public static final int L4_REPLY = 4;
    public static final int L5_GREET_RESUME = 5;
    public static final int L6_PROBATION = 6;

    private EvolutionSignalLevel() {
    }

    public static double defaultConfidence(int level) {
        switch (level) {
            case L1_OFFER:
                return 1.0;
            case L2_INTERVIEW:
                return 0.8;
            case L3_SCREEN:
                return 0.6;
            case L4_REPLY:
                return 0.4;
            case L5_GREET_RESUME:
                return 0.3;
            case L6_PROBATION:
                return 0.9;
            default:
                return 0.5;
        }
    }
}
