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

        public static Type FromString(String param) {
            if (param.equals(PGA.toString())) {
                return PGA;
            } else if (param.equals(PGV.toString())) {
                return PGV;
            } else if (param.equals(PSA.toString())) {
                return PSA;
            } else if (param.equals(Intensity.toString())) {
                return Intensity;
            }
            return null;
        }
    }

    public double expectedSI;
    public double percentile16;
    public double percentile84;
}
