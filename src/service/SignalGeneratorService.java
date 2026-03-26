package service;
import java.util.Random;

/**
 * SignalGeneratorService - Mô phỏng RSSI cho Physical Layer Key Generation
 * 
 * Nguyên lý bảo mật:
 * - Alice/Bob: Channel reciprocity → cùng shadowing + fading → tương quan cao
 * - Eve: Vị trí khác → shadowing RIÊNG + fading RIÊNG → không tương quan
 */
public class SignalGeneratorService {
    
    private static final Random RANDOM = new Random();
    
    // ====== AR(1) State Variables ======
    private double fadingAB;        // Fast fading chung Alice-Bob (channel reciprocity)
    private double fadingEve;       // Fast fading riêng Eve (independent channel)
    private double shadowingAB;     // Shadowing chung Alice-Bob (cùng môi trường)
    private double shadowingEve;    // Shadowing riêng Eve (môi trường khác)
    
    // ====== Simulation Parameters ======
    private final double alphaFast = 0.97;   // Fast fading correlation (cao → biến động chậm)
    private final double alphaSlow = 0.995;  // Shadowing correlation (rất cao → biến động rất chậm)
    private final double fastStd = 2.5;      // Độ lệch chuẩn fast fading (dB)
    private final double slowStd = 3.0;      // Độ lệch chuẩn shadowing (dB)
    private double noiseStd = 3.0;     // Measurement noise (dB)
    
    public SignalGeneratorService() {
        reset();
    }
    
    public void setNoiseStd(double noiseStd) {
        this.noiseStd = noiseStd;
    }

    public double getNoiseStd() {
        return this.noiseStd;
    }
    
    /**
     * AR(1) process: x(t) = alpha * x(t-1) + sqrt(1-alpha²) * noise
     * (Normalized để variance ổn định)
     */
    private double nextAR1(double last, double alpha, double stdDev) {
        // Dùng sqrt(1-alpha²) để giữ variance không tăng vô hạn
        double innovation = Math.sqrt(1 - alpha * alpha) * stdDev;
        return alpha * last + RANDOM.nextGaussian() * innovation;
    }
    
    /**
     * Sinh 1 mẫu RSSI đồng thời cho Alice, Bob, Eve
     * 
     * @param baseRSSI RSSI trung bình (path loss)
     * @return {aliceRSSI, bobRSSI, eveRSSI}
     */
    public double[] generateSample(double baseRSSI) {
        // --- Cập nhật các thành phần fading theo thời gian ---
        
        // 1. Shadowing (slow fading) - Phụ thuộc vị trí
        shadowingAB = nextAR1(shadowingAB, alphaSlow, slowStd);   // A-B cùng vị trí
        shadowingEve = nextAR1(shadowingEve, alphaSlow, slowStd); // Eve vị trí khác
        
        // 2. Fast fading - Phụ thuộc multipath
        fadingAB = nextAR1(fadingAB, alphaFast, fastStd);         // A-B cùng kênh
        fadingEve = nextAR1(fadingEve, alphaFast, fastStd);       // Eve kênh độc lập
        
        // --- Tính RSSI ---
        
        // Alice & Bob: Reciprocal channel
        double commonPartAB = baseRSSI + shadowingAB + fadingAB;
        double alice = commonPartAB + RANDOM.nextGaussian() * noiseStd;
        double bob = commonPartAB + RANDOM.nextGaussian() * noiseStd;
        
        // Eve: Independent channel (shadowing + fading hoàn toàn khác)
        double evePathLoss = 10 * 2.5 * Math.log10(1.5);
        double eve = (baseRSSI - evePathLoss) + shadowingEve + fadingEve + RANDOM.nextGaussian() * noiseStd;
        return new double[] {alice, bob, eve};
    }
    
    /**
     * Tính hệ số tương quan Pearson
     */
    public static double correlation(double[] x, double[] y) {
        int n = x.length;
        if (n == 0 || y.length != n) return 0;
        
        double meanX = 0, meanY = 0;
        for (int i = 0; i < n; i++) {
            meanX += x[i];
            meanY += y[i];
        }
        meanX /= n;
        meanY /= n;
        
        double cov = 0, varX = 0, varY = 0;
        for (int i = 0; i < n; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            cov += dx * dy;
            varX += dx * dx;
            varY += dy * dy;
        }
        
        if (varX < 1e-9 || varY < 1e-9) return 0;
        return cov / Math.sqrt(varX * varY);
    }
    
    /**
     * Reset simulation state
     */
    public final void reset() {
        fadingAB = 0;
        fadingEve = 0;
        shadowingAB = 0;
        shadowingEve = 0;
    }
}