package org.reaktEU.ewViewer.data;

public class Shaking {

    public enum Type {

        PGA("pga"),
        PGV("pgv"),
        PSA("psa"),
        Intensity("intensity");

        private final String string;

        private Type(final String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private double shakingExpectedSI;
    private double shaking16percentile;
    private double shaking84percentile;

    public void setShakingExpected(double shaking) {
        shakingExpectedSI = shaking;
    }

    public void setShaking16percentile(double shaking) {
        shaking16percentile = shaking;
    }

    public void setShaking84percentile(double shaking) {
        shaking84percentile = shaking;
    }

    public double getShakingExpected() {
        return shakingExpectedSI;
    }

    public double getShaking16percentile() {
        return shaking16percentile;
    }

    public double getShaking84percentile() {
        return shaking84percentile;
    }
}
