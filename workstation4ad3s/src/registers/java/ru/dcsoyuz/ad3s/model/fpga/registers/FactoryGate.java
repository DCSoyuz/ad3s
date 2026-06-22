package ru.dcsoyuz.ad3s.model.fpga.registers;

/**
 * Public (non-factory) build gate.
 *
 * In the public build no factory registers exist, so every factory feature is
 * inert: the "is factory-only" checks report false and the factory-register
 * accessors return {@code null} / {@code -1}. Callers null-guard the results.
 *
 * The factory build (Maven profile {@code -Pfactory}) replaces this exact class
 * (same FQN, living under {@code src/factory/java}) with a real implementation
 * that exposes {@code RegF1}/{@code RegF2}/{@code RegF3}. That factory source
 * set is git-ignored and never reaches the public repository.
 */
public final class FactoryGate {

    private FactoryGate() {
    }

    public static boolean isFactoryOnlyAddress(int address) {
        return false;
    }

    public static boolean isFactoryOnlyReg(IReg reg) {
        return false;
    }

    public static AllRegAddr regF1() {
        return null;
    }

    public static AllRegAddr regF2() {
        return null;
    }

    public static AllRegAddr regF3() {
        return null;
    }

    public static int regF1Address() {
        return -1;
    }
}
