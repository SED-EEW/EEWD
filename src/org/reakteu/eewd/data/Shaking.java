package org.reakteu.eewd.data;

public class Shaking {

    public enum Type {

        PGA("pga", "PGA"),
        PGV("pgv", "PGV"),
        PSA("psa", "PSA"),
        DRS("drs", "DRS"),
        Intensity("intensity", "Intensity");

        private final String string;
        private final String label;

        private Type(final String string, final String label) {
            this.string = string;
            this.label = label;
        }

        @Override
        public String toString() {
            return string;
        }

        public String labelString() {
            return label;
        }

        public static Type FromString(String param) {
            if (param == null) {
                return null;
            } else if (param.equals(PGA.toString())) {
                return PGA;
            } else if (param.equals(PGV.toString())) {
                return PGV;
            } else if (param.equals(PSA.toString())) {
                return PSA;
            } else if (param.equals(DRS.toString())) {
                return DRS;
            } else if (param.equals(Intensity.toString())) {
                return Intensity;
            }
            return null;
        }
    }

    public double expectedSI = 0.0;
    public double percentile16 = 0.0;
    public double percentile84 = 0.0;
}
