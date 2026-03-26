package util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ASUS
 */
public class BitXORUtil {
    public static List<Integer> xorBits(List<Integer> alice, List<Integer> bob) {
        int n = Math.min(alice.size(), bob.size());
        List<Integer> result = new ArrayList<>(n);

        for (int i = 0; i < n; i++) {
            result.add(alice.get(i) ^ bob.get(i)); // XOR bit
        }

        return result;
    }
}
