package service;

import java.util.*;

public class QuantizationService {

    private final double alpha; // Hệ số guard band

    public QuantizationService(double alpha) {
        this.alpha = alpha;
    }

    private double mean(double[] samples) {
        return Arrays.stream(samples).average().orElse(0.0);
    }

    private double std(double[] samples, double mean) {
        double sumSq = 0.0;
        for (double x : samples) {
            sumSq += Math.pow(x - mean, 2);
        }
        return Math.sqrt(sumSq / samples.length);
    }

    public double[] computeThresholds(double[] samples) {
        double mean = mean(samples);
        double sigma = std(samples, mean);

        double etaPlus = mean + alpha * sigma;
        double etaMinus = mean - alpha * sigma;

        return new double[]{etaMinus, etaPlus};
    }

    private Integer quantizeSample(double rssi, double etaMinus, double etaPlus) {
        if (rssi < etaMinus) {
            return 0;
        }
        if (rssi > etaPlus) {
            return 1;
        }
        return null; // Drop sample
    }

    /**
     * Chuyển 3 mảng RSSI (Alice, Bob, Eve) thành bitstream
     */
    public Map<String, List<Integer>> quantizeABC(double[] alice, double[] bob, double[] eve) {
        // Tính thresholds dựa trên Alice + Bob để đảm bảo reciprocity
        double[] combined = new double[alice.length + bob.length];
        System.arraycopy(alice, 0, combined, 0, alice.length);
        System.arraycopy(bob, 0, combined, alice.length, bob.length);

        double[] thresholds = computeThresholds(combined);
        double etaMinus = thresholds[0];
        double etaPlus = thresholds[1];

        Map<String, List<Integer>> bitStreams = new HashMap<>();
        bitStreams.put("Alice", new ArrayList<>());
        bitStreams.put("Bob", new ArrayList<>());
        bitStreams.put("Eve", new ArrayList<>());

        for (double x : alice) {
            bitStreams.get("Alice").add(quantizeSample(x, etaMinus, etaPlus));
        }
        for (double x : bob) {
            bitStreams.get("Bob").add(quantizeSample(x, etaMinus, etaPlus));
        }
        for (double x : eve) {
            bitStreams.get("Eve").add(quantizeSample(x, etaMinus, etaPlus));
        }

        return bitStreams;
    }

    /**
     * Tính KDR giữa Alice & Bob
     */
    public double computeKDR(List<Integer> aliceBits, List<Integer> bobBits) {
        int n = Math.min(aliceBits.size(), bobBits.size());
        int mismatch = 0, valid = 0;

        for (int i = 0; i < n; i++) {
            Integer a = aliceBits.get(i);
            Integer b = bobBits.get(i);
            if (a != null && b != null) {
                valid++;
                if (!a.equals(b)) {
                    mismatch++;
                }
            }
        }
        return valid == 0 ? 0 : (double) mismatch / valid;
    }
    
    public double computeMatchRate(List<Integer> x, List<Integer> y) {
    int n = Math.min(x.size(), y.size());
    int valid = 0, match = 0;

    for (int i = 0; i < n; i++) {
        Integer a = x.get(i);
        Integer b = y.get(i);

        if (a != null && b != null) {
            valid++;
            if (a.equals(b)) {
                match++;
            }
        }
    }

    return valid == 0 ? 0 : (double) match / valid;
}


    /**
     * Tính drop rate cho từng người
     */
    public double computeDropRate(List<Integer> bits) {
        long drop = bits.stream().filter(Objects::isNull).count();
        return (double) drop / bits.size();
    }
}
