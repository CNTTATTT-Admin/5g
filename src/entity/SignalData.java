package entity;

public class SignalData {
    private double[] aliceSignal;
    private double[] bobSignal;
    private double[] eveSignal;
    private double upperThreshold;
    private double lowerThreshold;

    public SignalData() {
    }

    public SignalData(double[] aliceSignal, double[] bobSignal, double[] eveSignal, double upperThreshold, double lowerThreshold) {
        this.aliceSignal = aliceSignal;
        this.bobSignal = bobSignal;
        this.eveSignal = eveSignal;
        this.upperThreshold = upperThreshold;
        this.lowerThreshold = lowerThreshold;
    }

    public double[] getAliceSignal() {
        return aliceSignal;
    }

    public void setAliceSignal(double[] aliceSignal) {
        this.aliceSignal = aliceSignal;
    }

    public double[] getBobSignal() {
        return bobSignal;
    }

    public void setBobSignal(double[] bobSignal) {
        this.bobSignal = bobSignal;
    }

    public double[] getEveSignal() {
        return eveSignal;
    }

    public void setEveSignal(double[] eveSignal) {
        this.eveSignal = eveSignal;
    }

    public double getUpperThreshold() {
        return upperThreshold;
    }

    public void setUpperThreshold(double upperThreshold) {
        this.upperThreshold = upperThreshold;
    }

    public double getLowerThreshold() {
        return lowerThreshold;
    }

    public void setLowerThreshold(double lowerThreshold) {
        this.lowerThreshold = lowerThreshold;
    }
}
