package util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class BitMatrixUtil {
    
    public static BufferedImage generateMatrix(List<Integer> bits, int bitSize, int nCols) {

        int nRows = (int) Math.ceil(bits.size() * 1.0 / nCols);

        int width = nCols * bitSize;
        int height = nRows * bitSize;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        int index = 0;

        for (int r = 0; r < nRows; r++) {
            for (int c = 0; c < nCols; c++) {

                if (index < bits.size() && bits.get(index) == 1)
                    g.setColor(Color.BLACK);
                else
                    g.setColor(Color.WHITE);

                g.fillRect(c * bitSize, r * bitSize, bitSize, bitSize);
                index++;
            }
        }

        g.dispose();
        return img;
    }
    
}
