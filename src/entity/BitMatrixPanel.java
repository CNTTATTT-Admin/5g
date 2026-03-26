package entity;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

public class BitMatrixPanel extends JPanel {

    private final List<Integer> bits;
    private final int nCols;
    private final int nRows;

    public BitMatrixPanel(List<Integer> originalBits) {
        this.bits = new ArrayList<>(originalBits);
        this.nCols = (int) Math.ceil(Math.sqrt(bits.size()));
        this.nRows = (int) Math.ceil(bits.size() / (double) nCols);
        int totalCells = nCols * nRows;
        while (bits.size() < totalCells) {
            bits.add(0);
        }

        // ép panel 100x100
        setPreferredSize(new Dimension(100, 100));
        setMinimumSize(new Dimension(100, 100));
        setMaximumSize(new Dimension(100, 100));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int panelW = getWidth();
        int panelH = getHeight();

        double cw = (double) panelW / nCols;
        double ch = (double) panelH / nRows;

        for (int i = 0; i < bits.size(); i++) {
            int row = i / nCols;
            int col = i % nCols;

            int x = (int) Math.round(col * cw);
            int y = (int) Math.round(row * ch);
            int w = (int) Math.ceil(cw);
            int h = (int) Math.ceil(ch);

            g.setColor(bits.get(i) == 1 ? Color.BLACK : Color.WHITE);
            g.fillRect(x, y, w, h);
        }
    }
}
