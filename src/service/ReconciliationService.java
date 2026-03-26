package service;

import java.util.*;

public class ReconciliationService {

    private static final double MAX_KDR_THRESHOLD = 0.15;
    private static final int RANDOM_SEED = 42;

    public static class ReconciliationResult {

        private final boolean success;
        private final List<Integer> correctedBits;
        private final double initialKDR;
        private final double finalKDR;
        private final int totalCorrections;
        private final int totalParityExchanges;
        private final String message;

        public ReconciliationResult(boolean success, List<Integer> correctedBits, double initialKDR,
                double finalKDR, int corrections, int exchanges, String message) {
            this.success = success;
            this.correctedBits = correctedBits;
            this.initialKDR = initialKDR;
            this.finalKDR = finalKDR;
            this.totalCorrections = corrections;
            this.totalParityExchanges = exchanges;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public List<Integer> getCorrectedBits() {
            return correctedBits;
        }

        public double getInitialKDR() {
            return initialKDR;
        }

        public double getFinalKDR() {
            return finalKDR;
        }

        public int getTotalCorrections() {
            return totalCorrections;
        }

        public int getTotalParityExchanges() {
            return totalParityExchanges;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format(
                    "ReconciliationResult{success=%b, initialKDR=%.2f%%, finalKMR=%.4f%%, corrections=%d, parityExchanges=%d, message='%s'}",
                    success, initialKDR * 100, finalKDR * 100, totalCorrections, totalParityExchanges, message
            );
        }
    }

    public ReconciliationResult reconcile(List<Integer> aliceBits, List<Integer> bobBits, double kdr) {
        if (aliceBits == null || bobBits == null) {
            return new ReconciliationResult(false, null, 0, 0, 0, 0, "Error: Null input bits");
        }

        if (aliceBits.size() != bobBits.size()) {
            return new ReconciliationResult(false, null, 0, 0, 0, 0, "Error: Alice and Bob bits length mismatch");
        }

        if (aliceBits.size() < 10) {
            return new ReconciliationResult(false, null, 0, 0, 0, 0, "Error: Too few bits (minimum 10 required)");
        }

        int[] aliceArray = aliceBits.stream().mapToInt(Integer::intValue).toArray();
        int[] bobArray = bobBits.stream().mapToInt(Integer::intValue).toArray();

        if (kdr > MAX_KDR_THRESHOLD) {
            return new ReconciliationResult(false, null, kdr, kdr, 0, 0,
                    String.format("KMR quá lớn (%.2f%% > %.0f%%). Không thể sửa lỗi.",
                            kdr * 100, MAX_KDR_THRESHOLD * 100));
        }

        if (kdr == 0.0) {
            return new ReconciliationResult(
                    true,
                    aliceBits,
                    0.0,
                    0.0,
                    0,
                    0,
                    "Sửa lỗi thành công! Không phát hiện thấy lỗi (KDR = 0%)"
            );
        }

        try {
            CascadeCorrection cascade = new CascadeCorrection(aliceArray, bobArray, RANDOM_SEED);
            cascade.correct();

            int[] corrected = cascade.getCorrectedBits();
            double finalKDR = (double) countErrors(aliceArray, corrected) / aliceArray.length;

            List<Integer> correctedList = Arrays.stream(corrected).boxed().collect(java.util.stream.Collectors.toList());

            String message = String.format("Sửa lõi thành công ! KDR: %.2f%% → %.2f%%",
                    kdr * 100, finalKDR * 100);

            return new ReconciliationResult(true, correctedList, kdr, finalKDR,
                    cascade.getTotalCorrections(), cascade.getTotalParityExchanges(), message);

        } catch (Exception e) {
            return new ReconciliationResult(false, null, kdr, kdr, 0, 0,
                    "Error during reconciliation: " + e.getMessage());
        }
    }

    public static int countErrors(int[] a, int[] b) {
        int errors = 0;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            if (a[i] != b[i]) {
                errors++;
            }
        }
        return errors;
    }

    private static class CascadeCorrection {

        private int[] aliceBits;
        private int[] bobBits;
        private Random random;
        private int totalCorrections = 0;
        private int totalParityExchanges = 0;

        public CascadeCorrection(int[] alice, int[] bob, int seed) {
            this.aliceBits = alice.clone();
            this.bobBits = bob.clone();
            this.random = new Random(seed);
        }

        public void correct() {
            double kdr = calculateKDR();

            int k = calculateInitialBlockSize(kdr);
            cascadePass(k, true);

            k *= 2;
            cascadePass(k, true);

            k *= 2;
            cascadePass(k, true);

            if (calculateKDR() > 0.01) {
                k *= 2;
                cascadePass(k, false);
            }

            if (calculateKDR() > 0.005) {
                k *= 2;
                cascadePass(k, false);
            }
        }

        private int calculateInitialBlockSize(double kdr) {
            if (kdr > 0.10) {
                return 4;
            }
            if (kdr > 0.05) {
                return 6;
            }
            if (kdr > 0.02) {
                return 10;
            }
            return Math.max(12, (int) (0.73 / kdr));
        }

        private void cascadePass(int blockSize, boolean shuffle) {
            int[] indices = new int[aliceBits.length];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i;
            }

            if (shuffle) {
                shuffleArray(indices);
            }

            for (int start = 0; start < aliceBits.length; start += blockSize) {
                int end = Math.min(start + blockSize, aliceBits.length);
                correctBlock(indices, start, end);
            }
        }

        private void correctBlock(int[] indices, int start, int end) {
            int aliceParity = 0, bobParity = 0;

            for (int i = start; i < end; i++) {
                aliceParity ^= aliceBits[indices[i]];
                bobParity ^= bobBits[indices[i]];
            }

            totalParityExchanges++;

            if (aliceParity != bobParity) {
                int errorPos = binarySearch(indices, start, end);
                if (errorPos != -1) {
                    bobBits[indices[errorPos]] ^= 1;
                    totalCorrections++;
                }
            }
        }

        private int binarySearch(int[] indices, int start, int end) {
            if (end - start <= 1) {
                return start;
            }

            int mid = (start + end) / 2;
            int aliceParity = 0, bobParity = 0;

            for (int i = start; i < mid; i++) {
                aliceParity ^= aliceBits[indices[i]];
                bobParity ^= bobBits[indices[i]];
            }

            totalParityExchanges++;

            if (aliceParity != bobParity) {
                return binarySearch(indices, start, mid);
            } else {
                return binarySearch(indices, mid, end);
            }
        }

        private void shuffleArray(int[] array) {
            for (int i = array.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }

        private double calculateKDR() {
            int errors = 0;
            for (int i = 0; i < aliceBits.length; i++) {
                if (aliceBits[i] != bobBits[i]) {
                    errors++;
                }
            }
            return (double) errors / aliceBits.length;
        }

        public int[] getCorrectedBits() {
            return bobBits.clone();
        }

        public int getTotalCorrections() {
            return totalCorrections;
        }

        public int getTotalParityExchanges() {
            return totalParityExchanges;
        }
    }
}
