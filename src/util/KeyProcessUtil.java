package util;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

public class KeyProcessUtil {
    
    public static String hashSHA256(String key) {
        return DigestUtils.sha256Hex(key);
    }
    
    public static String hashSHA256(byte[] data) {
        return DigestUtils.sha256Hex(data);
    }
    
    public static String hashSHA256(List<Integer> bits) {
        StringBuilder sb = new StringBuilder();
        for (Integer bit : bits) {
            sb.append(bit);
        }
        return DigestUtils.sha256Hex(sb.toString());
    }
}
