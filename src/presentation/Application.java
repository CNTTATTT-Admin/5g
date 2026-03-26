/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package presentation;

import entity.BitMatrixPanel;
import entity.BitSequenceData;
import entity.SignalData;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.Timer;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import service.QuantizationService;
import service.ReconciliationService;
import service.SignalGeneratorService;
import util.BitXORUtil;
import util.KeyProcessUtil;

/**
 *
 * @author ASUS
 */
public class Application extends javax.swing.JFrame {

    private final XYSeries aliceSeries = new XYSeries("Alice");
    private final XYSeries bobSeries = new XYSeries("Bob");
    private final XYSeries eveSeries = new XYSeries("Eve");
    private XYSeries upperSeries;
    private XYSeries lowerSeries;

    private SignalData signalData;
    private BitSequenceData bitSequenceData;
    private int sampleCount = 0;
    private final double measurementIntervalSec = 0.2;
    private final int windowSize = 400;
    private final double baseRSSI = -70.0;
    private double KDR;
    private final SignalGeneratorService generator = new SignalGeneratorService();
    private final ReconciliationService reconciliationService = new ReconciliationService();
    private QuantizationService quantizationService;

    private Timer simulationTimer;
    private boolean channelProbed = false;

    /**
     * Creates new form Application
     */
    public Application() {
        initComponents();
        initSetupSpecifications();
        setResizable(false);
        setLocationRelativeTo(null);
        initChart();
    }

    private void initSetupSpecifications() {

        jNoiseSlider.setMinimum(0);
        jNoiseSlider.setMaximum(30);

        jNoiseSlider.setValue(15); // default 1.5
        generator.setNoiseStd(1.5);

        jNoiseSlider.setMajorTickSpacing(10);
        jNoiseSlider.setMinorTickSpacing(1);
        jNoiseSlider.setPaintTicks(true);
        jNoiseSlider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(10, new JLabel("1"));
        labelTable.put(20, new JLabel("2"));
        labelTable.put(30, new JLabel("3"));
        jNoiseSlider.setLabelTable(labelTable);
        jNoiseSlider.setPaintLabels(true);

        jNoiseSlider.addChangeListener((javax.swing.event.ChangeEvent e) -> {
            double value = jNoiseSlider.getValue() / 10.0;
            generator.setNoiseStd(value);
            System.out.println("Noise std = " + value);
        });

        // --- Alpha slider ---
        JAlphaSilder.setMinimum(4);  // 0.4 * 10
        JAlphaSilder.setMaximum(10); // 1.0 * 10
        JAlphaSilder.setValue(7);    // default 0.7
        JAlphaSilder.setMajorTickSpacing(1);
        JAlphaSilder.setMinorTickSpacing(1);
        JAlphaSilder.setPaintTicks(true);

        quantizationService = new QuantizationService(0.7);

        Hashtable<Integer, JLabel> alphaLabels = new Hashtable<>();
        for (int i = 3; i <= 10; i++) {
            alphaLabels.put(i, new JLabel(String.format("%.1f", i / 10.0)));
        }
        JAlphaSilder.setLabelTable(alphaLabels);
        JAlphaSilder.setPaintLabels(true);

        JAlphaSilder.addChangeListener((javax.swing.event.ChangeEvent e) -> {
            double alphaValue = JAlphaSilder.getValue() / 10.0;
            quantizationService = new QuantizationService(alphaValue);
            System.out.println("Alpha = " + alphaValue);
        });

        // Force set BorderLayout cho các panel
        jPanel5.setLayout(new BorderLayout());
        jPanel6.setLayout(new BorderLayout());
        jPanel7.setLayout(new BorderLayout());

        // Set minimum size để không bị co lại
        Dimension minSize = new Dimension(100, 100);
        jPanel5.setMinimumSize(minSize);
        jPanel6.setMinimumSize(minSize);
        jPanel7.setMinimumSize(minSize);

        // Set preferred size
        Dimension prefSize = new Dimension(128, 143);
        jPanel5.setPreferredSize(prefSize);
        jPanel6.setPreferredSize(prefSize);
        jPanel7.setPreferredSize(prefSize);

    }

    private void initChart() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(aliceSeries);
        dataset.addSeries(bobSeries);
        dataset.addSeries(eveSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                "Time Lag (Δt) samples",
                "Received Power (dBm)",
                dataset
        );

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        chart.setBackgroundPaint(Color.WHITE);

        plot.getRangeAxis().setRange(-85, -55);
        plot.getRangeAxis().setAutoRange(false);

        ChartPanel chartPanel = new ChartPanel(chart);
        // --- Bật zoom bằng chuột ---
        chartPanel.setMouseWheelEnabled(true); // zoom bằng scroll wheel
        chartPanel.setDomainZoomable(true);    // zoom theo trục X
        chartPanel.setRangeZoomable(true);     // zoom theo trục Y

        // --- Bật toggle series khi click legend ---
        chart.getLegend().setItemFont(new java.awt.Font("Tahoma", java.awt.Font.BOLD, 12));
        chart.getLegend().setItemLabelPadding(new RectangleInsets(2, 2, 2, 2));
        Map<Integer, Stroke> originalStrokes = new HashMap<>();

        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ChartEntity entity = chartPanel.getEntityForPoint(e.getX(), e.getY());
                if (entity instanceof LegendItemEntity legendItem) {

                    Comparable seriesKey = legendItem.getSeriesKey();
                    XYPlot p = chart.getXYPlot();
                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) p.getRenderer();

                    int seriesIndex = dataset.indexOf(seriesKey);
                    if (seriesIndex >= 0) {

                        // Lưu stroke gốc nếu chưa lưu
                        originalStrokes.putIfAbsent(
                                seriesIndex,
                                renderer.getSeriesStroke(seriesIndex)
                        );

                        Stroke current = renderer.getSeriesStroke(seriesIndex);

                        // Nếu stroke hiện tại là 0f → đang ẩn → bật lại
                        if (current instanceof BasicStroke bs && bs.getLineWidth() == 0f) {
                            renderer.setSeriesStroke(seriesIndex, originalStrokes.get(seriesIndex));
                        } // Ngược lại → tắt line bằng stroke 0f
                        else {
                            renderer.setSeriesStroke(seriesIndex, new BasicStroke(0f));
                        }

                        p.setNotify(true);
                    }
                }
            }
        });

        chartPanel.setPreferredSize(new Dimension(jPanel2.getWidth(), jPanel2.getHeight()));
        jPanel2.setLayout(new BorderLayout());
        jPanel2.add(chartPanel, BorderLayout.CENTER);
        jPanel2.validate();
    }

    private void startSimulation() {
        clearInformation();

        simulationTimer = new Timer((int) (measurementIntervalSec * 1000), e -> {

            if (sampleCount >= windowSize) {
                simulationTimer.stop();
                buildSignalData();
                return;
            }

            // Lấy mẫu RSSI
            double[] sample = generator.generateSample(baseRSSI);
            double alice = sample[0];
            double bob = sample[1];
            double eve = sample[2];

            // Thêm vào series (không remove FIFO)
            aliceSeries.add(sampleCount, alice);
            bobSeries.add(sampleCount, bob);
            eveSeries.add(sampleCount, eve);

            sampleCount++;
        });

        simulationTimer.start();
    }

    private void buildSignalData() {
        double[] alice = new double[windowSize];
        double[] bob = new double[windowSize];
        double[] eve = new double[windowSize];
        for (int i = 0; i < windowSize; i++) {
            alice[i] = aliceSeries.getY(i).doubleValue();
            bob[i] = bobSeries.getY(i).doubleValue();
            eve[i] = eveSeries.getY(i).doubleValue();
        }

        double[] combined = new double[alice.length + bob.length];
        System.arraycopy(alice, 0, combined, 0, alice.length);
        System.arraycopy(bob, 0, combined, alice.length, bob.length);

        double[] thresholds = quantizationService.computeThresholds(combined);
        double lower = thresholds[0];
        double upper = thresholds[1];
        signalData = new SignalData(alice, bob, eve, upper, lower);
        channelProbed = true;
    }

    private void clearInformation() {
        // --- Reset mọi thứ mỗi lần Channel Probing ---
        sampleCount = 0;
        aliceSeries.clear();
        bobSeries.clear();
        eveSeries.clear();

        ChartPanel chartPanel = (ChartPanel) jPanel2.getComponent(0);
        XYPlot plot = chartPanel.getChart().getXYPlot();
        XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

        int aliceIndex = dataset.indexOf(aliceSeries.getKey());
        int bobIndex = dataset.indexOf(bobSeries.getKey());

        renderer.setSeriesShapesVisible(aliceIndex, false);
        renderer.setSeriesShapesVisible(bobIndex, false);

        // --- Xóa nếu tồn tại series cũ ---
        if (upperSeries != null) {
            dataset.removeSeries(upperSeries);
        }
        if (lowerSeries != null) {
            dataset.removeSeries(lowerSeries);
        }

        // Nếu timer đang chạy thì dừng và tạo timer mới (đúng nhất)
        if (simulationTimer != null && simulationTimer.isRunning()) {
            simulationTimer.stop();
        }
        simulationTimer = null;
        txtKGR.setText("");
        txtKDR.setText("");
        txtCorAB.setText("");
        txtCorAE.setText("");

        jPanel5.removeAll();
        jPanel5.revalidate();
        jPanel5.repaint();

        jPanel6.removeAll();
        jPanel6.revalidate();
        jPanel6.repaint();

        jPanel7.removeAll();
        jPanel7.revalidate();
        jPanel7.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton14 = new javax.swing.JButton();
        abstractOverlay1 = new org.jfree.chart.panel.AbstractOverlay();
        jPanel1 = new javax.swing.JPanel();
        btnQuanzatition = new javax.swing.JButton();
        btnAmplification = new javax.swing.JButton();
        btnQuit = new javax.swing.JButton();
        btnReconcilliation = new javax.swing.JButton();
        btnChannelProbing = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jNoiseSlider = new javax.swing.JSlider();
        JAlphaSilder = new javax.swing.JSlider();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtKGR = new javax.swing.JLabel();
        txtKDR = new javax.swing.JLabel();
        txtCorAB = new javax.swing.JLabel();
        txtCorAE = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();

        jButton14.setText("Info Reconcilliation");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Bảng điều khiển", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14)));
        jPanel1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jPanel1.setName("ControlPanel"); // NOI18N

        btnQuanzatition.setText("Quanzatition");
        btnQuanzatition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuanzatitionActionPerformed(evt);
            }
        });

        btnAmplification.setText("Pricacy Amplification");
        btnAmplification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAmplificationActionPerformed(evt);
            }
        });

        btnQuit.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnQuit.setForeground(new java.awt.Color(255, 51, 51));
        btnQuit.setText("QUIT !");
        btnQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuitActionPerformed(evt);
            }
        });

        btnReconcilliation.setText("Info Reconcilliation");
        btnReconcilliation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReconcilliationActionPerformed(evt);
            }
        });

        btnChannelProbing.setText("Channel Probing");
        btnChannelProbing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChannelProbingActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnQuanzatition, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnAmplification, javax.swing.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                    .addComponent(btnQuit, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnChannelProbing, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnReconcilliation, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnChannelProbing, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnQuanzatition, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnReconcilliation, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnAmplification, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnQuit, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jTextField1.setBackground(new java.awt.Color(0, 153, 153));
        jTextField1.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField1.setText("AUTOMATIC SECURE KEY GENERATION SIMULATOR");
        jTextField1.setFocusable(false);
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                none(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "RSSI", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14)));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 239, Short.MAX_VALUE)
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Bảng thông số", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14)));

        jLabel1.setText("Alpha (α) Guard Band:");

        jLabel2.setText("Noise Level:");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jNoiseSlider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(JAlphaSilder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(JAlphaSilder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(4, 4, 4)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jNoiseSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Metrics", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14)));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("KGR (Key Generation Rate):");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setText("KMR (Key Mismatch Rate):");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setText("Correlation (Alice-Bob):");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Correlation (Alice-Eve):");

        txtKGR.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtKDR.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtCorAB.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        txtCorAE.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtCorAE, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 90, Short.MAX_VALUE)
                    .addComponent(txtKDR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtCorAB, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(txtKGR, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtKGR))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtKDR))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtCorAB))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtCorAE)))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Alice Bits", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14)));
        jPanel5.setMaximumSize(new java.awt.Dimension(100, 100));
        jPanel5.setMinimumSize(new java.awt.Dimension(100, 100));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Bob Bits", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14)));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "DIFF Bits", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 14)));

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("Each square denotes one bit, White represents \"0\"  and Black represents \"1\"");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTextField1, javax.swing.GroupLayout.DEFAULT_SIZE, 1140, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(259, 259, 259)
                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(115, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void none(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_none
        // TODO add your handling code here:
    }//GEN-LAST:event_none

    private void btnAmplificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAmplificationActionPerformed
        // TODO add your handling code here:
        if (!channelProbed || this.signalData == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng thực hiện Channel Probing trước!",
                    "Chưa có dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (this.bitSequenceData == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng thực hiện Quanzatiton trước!",
                    "Chưa có dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!bitSequenceData.getAliceBits().equals(bitSequenceData.getBobBits())) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng thực hiện Reconcilliation trước!",
                    "Chưa có dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String aliceHash = KeyProcessUtil.hashSHA256(bitSequenceData.getAliceBits());
        String bobHash = KeyProcessUtil.hashSHA256(bitSequenceData.getBobBits());
        String eveHash = KeyProcessUtil.hashSHA256(bitSequenceData.getEveBits());

        boolean match = aliceHash.equals(bobHash);

        String kmrText = "0.00%";

        String status = match
                ? "RECONCILIATION SUCCESS - System Ready"
                : "KEY MISMATCH - RECONCILIATION FAILED";

        // --- TẠO TEXTPANE ĐẸP ---
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        textPane.setFont(new Font("Consolas", Font.PLAIN, 14));

        StyledDocument doc = textPane.getStyledDocument();

        // Styles
        Style normal = doc.addStyle("normal", null);
        StyleConstants.setForeground(normal, Color.BLACK);

        Style title = doc.addStyle("title", null);
        StyleConstants.setForeground(title, new Color(20, 20, 20));
        StyleConstants.setBold(title, true);
        StyleConstants.setFontSize(title, 16);

        Style hashStyle = doc.addStyle("hash", null);
        StyleConstants.setForeground(hashStyle, new Color(50, 90, 200));

        Style okStyle = doc.addStyle("ok", null);
        StyleConstants.setForeground(okStyle, new Color(0, 160, 0));
        StyleConstants.setBold(okStyle, true);

        Style errorStyle = doc.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, Color.RED);
        StyleConstants.setBold(errorStyle, true);

        // Helper (insert nhanh)
        BiConsumer<String, Style> add = (t, s) -> {
            try {
                doc.insertString(doc.getLength(), t, s);
            } catch (Exception ignored) {
            }
        };

        // --- Insert nội dung ---
        add.accept("[LOCK] FINAL KEYS (After Privacy Amplification)\n\n", title);

        // Alice
        add.accept("Alice: ", normal);
        add.accept(aliceHash + "   ", hashStyle);
        add.accept(match ? "✔ MATCH\n" : "✘ ERROR\n",
                match ? okStyle : errorStyle);

        // Bob
        add.accept("Bob:   ", normal);
        add.accept(bobHash + "   ", hashStyle);
        add.accept(match ? "✔ MATCH\n" : "✘ NOT MATCH\n",
                match ? okStyle : errorStyle);

        // Eve
        add.accept("Eve:   ", normal);
        add.accept(eveHash + "   ", hashStyle);
        add.accept("✘ DIFFERENT\n\n", errorStyle);

        // KMR
        add.accept("[INFO] Key Mismatch Rate: ", normal);
        add.accept(kmrText + "   ", hashStyle);
        add.accept(match ? "✔ SECURE KEY ESTABLISHED\n" : "✘ UNSAFE\n",
                match ? okStyle : errorStyle);

        // Status
        add.accept("[SECURE] Status: ", normal);
        add.accept(status, match ? okStyle : errorStyle);

        // --- SHOW ---
        JOptionPane.showMessageDialog(
                this,
                textPane,
                "FINAL KEY RESULT",
                JOptionPane.INFORMATION_MESSAGE
        );

    }//GEN-LAST:event_btnAmplificationActionPerformed

    private void btnQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuitActionPerformed
        // TODO add your handling code here:
        int option = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn thoát chương trình?",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (option == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
        // nếu chọn No thì không làm gì
    }//GEN-LAST:event_btnQuitActionPerformed

    private void btnChannelProbingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChannelProbingActionPerformed
        // TODO add your handling code here:
        startSimulation();
        channelProbed = true;
    }//GEN-LAST:event_btnChannelProbingActionPerformed

    private void btnQuanzatitionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuanzatitionActionPerformed
        // TODO add your handling code here:
        if (!channelProbed || this.signalData == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng thực hiện Channel Probing trước!",
                    "Chưa có dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        ChartPanel chartPanel = (ChartPanel) jPanel2.getComponent(0);
        XYPlot plot = chartPanel.getChart().getXYPlot();
        XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

        // --- Xóa nếu tồn tại series cũ ---
        if (upperSeries != null) {
            dataset.removeSeries(upperSeries);
        }
        if (lowerSeries != null) {
            dataset.removeSeries(lowerSeries);
        }

        // --- Tạo threshold series ---
        upperSeries = new XYSeries("Upper Threshold");
        lowerSeries = new XYSeries("Lower Threshold");

        double[] combined = new double[signalData.getAliceSignal().length + signalData.getBobSignal().length];
        System.arraycopy(signalData.getAliceSignal(), 0, combined, 0, signalData.getAliceSignal().length);
        System.arraycopy(signalData.getAliceSignal(), 0, combined, signalData.getAliceSignal().length, signalData.getBobSignal().length);

        double[] thresholds = quantizationService.computeThresholds(combined);
        signalData.setLowerThreshold(thresholds[0]);
        signalData.setUpperThreshold(thresholds[1]);

        double upper = signalData.getUpperThreshold();
        double lower = signalData.getLowerThreshold();

        for (int i = 0; i < windowSize; i++) {
            upperSeries.add(i, upper);
            lowerSeries.add(i, lower);
        }

        // --- Chỉnh renderer cho Alice/Bob để vẽ shape tròn tại các điểm ---
        int aliceIndex = dataset.indexOf(aliceSeries.getKey());
        int bobIndex = dataset.indexOf(bobSeries.getKey());

        dataset.addSeries(upperSeries);
        dataset.addSeries(lowerSeries);

        // --- Chỉnh renderer cho threshold ---
        int upperIndex = dataset.indexOf(upperSeries.getKey());
        int lowerIndex = dataset.indexOf(lowerSeries.getKey());

        renderer.setSeriesPaint(upperIndex, Color.RED);
        renderer.setSeriesStroke(upperIndex, new BasicStroke(2f));
        renderer.setSeriesShapesVisible(upperIndex, false); // line liền, không shape

        renderer.setSeriesPaint(lowerIndex, Color.BLUE);
        renderer.setSeriesStroke(lowerIndex, new BasicStroke(2f));
        renderer.setSeriesShapesVisible(lowerIndex, false); // line liền, không shape

        renderer.setSeriesShapesVisible(aliceIndex, true);
        renderer.setSeriesShape(aliceIndex, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        renderer.setSeriesPaint(aliceIndex, Color.RED); // giữ màu Alice

        renderer.setSeriesShapesVisible(bobIndex, true);
        renderer.setSeriesShape(bobIndex, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        renderer.setSeriesPaint(bobIndex, Color.BLUE); // giữ màu Bob

        plot.setNotify(true);

        // === RUN QUANTIZATION ===
        double[] alice = signalData.getAliceSignal();
        double[] bob = signalData.getBobSignal();
        double[] eve = signalData.getEveSignal();

        Map<String, List<Integer>> bits = quantizationService.quantizeABC(alice, bob, eve);

        List<Integer> aliceBits = bits.get("Alice");
        List<Integer> bobBits = bits.get("Bob");
        List<Integer> eveBits = bits.get("Eve");

        List<Integer> aliceBitsSync = new ArrayList<>();
        List<Integer> bobBitsSync = new ArrayList<>();

        int n = Math.min(aliceBits.size(), bobBits.size());
        for (int i = 0; i < n; i++) {
            Integer a = aliceBits.get(i);
            Integer b = bobBits.get(i);
            if (a != null && b != null) {
                aliceBitsSync.add(a);
                bobBitsSync.add(b);
            }
        }

        long validBits = aliceBitsSync.size();
        double KGR = validBits / (aliceBitsSync.size() * measurementIntervalSec);

        // Bit data
        bitSequenceData = new BitSequenceData();
        bitSequenceData.setAliceBits(aliceBitsSync);
        bitSequenceData.setBobBits(bobBitsSync);
        bitSequenceData.setEveBits(eveBits.stream()
                .filter(bit -> bit != null)
                .collect(Collectors.toList()));

        KDR = quantizationService.computeKDR(bitSequenceData.getAliceBits(), bitSequenceData.getBobBits());
        double abMatch = quantizationService.computeMatchRate(bitSequenceData.getAliceBits(), bitSequenceData.getBobBits());
        double aeMatch = quantizationService.computeMatchRate(bitSequenceData.getAliceBits(), bitSequenceData.getEveBits());

        System.out.println("KGR (bit/s) = " + KGR);
        System.out.println("Alice bits: " + bitSequenceData.getAliceBits());
        System.out.println("Bob bits:   " + bitSequenceData.getBobBits());
        System.out.println("Eve bits:   " + bitSequenceData.getEveBits());
        System.out.println("KDR Alice-Bob = " + KDR);

        txtKDR.setText(String.format("%.2f %%", KDR * 100));
        txtKGR.setText(String.format("%.2f %%", KGR));
        txtCorAB.setText(String.format("%.2f %%", abMatch * 100));
        txtCorAE.setText(String.format("%.2f %%", aeMatch * 100));

        BitMatrixPanel alicePanel = new BitMatrixPanel(bitSequenceData.getAliceBits());
        jPanel5.removeAll();
        jPanel5.add(alicePanel, BorderLayout.CENTER);

        BitMatrixPanel bobPanel = new BitMatrixPanel(bitSequenceData.getBobBits());
        jPanel6.removeAll();
        jPanel6.add(bobPanel, BorderLayout.CENTER);

        List<Integer> diffABBits = BitXORUtil.xorBits(bitSequenceData.getAliceBits(), bitSequenceData.getBobBits());

        BitMatrixPanel diffPanel = new BitMatrixPanel(diffABBits);
        jPanel7.removeAll();
        jPanel7.add(diffPanel, BorderLayout.CENTER);
    }//GEN-LAST:event_btnQuanzatitionActionPerformed

    private void btnReconcilliationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReconcilliationActionPerformed
        // TODO add your handling code here:
        if (!channelProbed || this.signalData == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng thực hiện Channel Probing trước!",
                    "Chưa có dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (this.bitSequenceData == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng thực hiện Quanzatiton trước!",
                    "Chưa có dữ liệu",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        ReconciliationService.ReconciliationResult result = reconciliationService.reconcile(bitSequenceData.getAliceBits(),
                bitSequenceData.getBobBits(), KDR);
        if (!result.isSuccess()) {
            JOptionPane.showMessageDialog(this, result.getMessage(), "Có lỗi đã xảy ra", JOptionPane.WARNING_MESSAGE);
        } else {
            System.out.println("Resul = " + result.getCorrectedBits());
            List<Integer> finalReconcilliationBits = result.getCorrectedBits();
            bitSequenceData.setAliceBits(finalReconcilliationBits);
            bitSequenceData.setBobBits(finalReconcilliationBits);

            BitMatrixPanel alicePanel = new BitMatrixPanel(bitSequenceData.getAliceBits());
            jPanel5.removeAll();
            jPanel5.add(alicePanel, BorderLayout.CENTER);
            jPanel5.revalidate();
            jPanel5.repaint();
            BitMatrixPanel bobPanel = new BitMatrixPanel(bitSequenceData.getBobBits());

            jPanel6.removeAll();
            jPanel6.add(bobPanel, BorderLayout.CENTER);
            jPanel6.revalidate();
            jPanel6.repaint();

            jPanel7.removeAll();
            jPanel7.revalidate();
            jPanel7.repaint();

            txtKDR.setText("0.00 %");
            txtCorAB.setText("100 %");
            JOptionPane.showMessageDialog(this, result.getMessage(), "Xử lý lỗi hoàn thành", JOptionPane.PLAIN_MESSAGE);
        }

    }//GEN-LAST:event_btnReconcilliationActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Application.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new Application().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSlider JAlphaSilder;
    private org.jfree.chart.panel.AbstractOverlay abstractOverlay1;
    private javax.swing.JButton btnAmplification;
    private javax.swing.JButton btnChannelProbing;
    private javax.swing.JButton btnQuanzatition;
    private javax.swing.JButton btnQuit;
    private javax.swing.JButton btnReconcilliation;
    private javax.swing.JButton jButton14;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JSlider jNoiseSlider;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel txtCorAB;
    private javax.swing.JLabel txtCorAE;
    private javax.swing.JLabel txtKDR;
    private javax.swing.JLabel txtKGR;
    // End of variables declaration//GEN-END:variables
}
