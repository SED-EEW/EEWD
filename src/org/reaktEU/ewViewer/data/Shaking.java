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

    public double expectedSI;
    public double percentile16;
    public double percentile84;
}
