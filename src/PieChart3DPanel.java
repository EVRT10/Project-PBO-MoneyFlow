import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;

public class PieChart3DPanel extends JPanel {
    private TransactionManager manager;
    private List<Transaction> transactions;

    // Theme colors
    private final Color TEXT_DARK    = new Color(28, 14, 56);
    private final Color TEXT_GRAY    = new Color(150, 140, 180);

    // Pastels / curated colors
    private final Color[] colors = {
        new Color(164, 96, 214),  // Medium Purple
        new Color(115, 72, 195),  // Fuchsia Blue
        new Color(46, 204, 113),  // Emerald Green
        new Color(231, 76, 60),   // Alizarin Red
        new Color(52, 152, 219),  // Peter River Blue
        new Color(241, 196, 15),  // Sun Flower Yellow
        new Color(230, 126, 34)   // Carrot Orange
    };

    // Animation & Interaction
    private double animProgress = 0.0;
    private javax.swing.Timer animTimer;

    private List<SliceInfo> slices = new ArrayList<>();
    private int hoveredSliceIdx = -1;
    private Point mousePoint = null;

    private static class SliceInfo {
        int index;
        String category;
        double value;
        double percentage;
        double startAngle;
        double sweepAngle;
        Color color;
    }

    public PieChart3DPanel(TransactionManager manager) {
        this.manager = manager;
        this.transactions = manager.getTransactions();
        setOpaque(false);

        // Hover tracking
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePoint = e.getPoint();
                int oldHover = hoveredSliceIdx;
                hoveredSliceIdx = detectHover(mousePoint);
                if (oldHover != hoveredSliceIdx) {
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredSliceIdx = -1;
                mousePoint = null;
                repaint();
            }
        });

        startAnimation();
    }

    public void refreshData() {
        this.transactions = manager.getTransactions();
        startAnimation();
    }

    public void setTransactions(List<Transaction> list) {
        this.transactions = list;
        startAnimation();
    }

    private void startAnimation() {
        animProgress = 0.0;
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        animTimer = new javax.swing.Timer(15, e -> {
            animProgress += 0.04;
            if (animProgress >= 1.0) {
                animProgress = 1.0;
                animTimer.stop();
            }
            repaint();
        });
        animTimer.start();
    }

    private int detectHover(Point p) {
        if (slices.isEmpty()) return -1;

        int width = getWidth();
        int height = getHeight();
        int cx = width / 2;
        int cy = height / 2 - 10;
        int rx = Math.min(width, height) / 3 - 5;
        int ry = (int) (rx * 0.58);

        // Convert mouse coordinates to standard circle space
        double tx = (double) (p.x - cx) / rx;
        double ty = (double) (p.y - cy) / ry;
        double dist = Math.sqrt(tx * tx + ty * ty);

        if (dist <= 1.0) {
            // Mouse is inside the top surface ellipse
            double angle = Math.toDegrees(Math.atan2(-ty, tx));
            if (angle < 0) angle += 360;

            for (int i = 0; i < slices.size(); i++) {
                SliceInfo slice = slices.get(i);
                double start = slice.startAngle;
                double end = start + slice.sweepAngle;

                // Adjust for wrapping around 360
                if (end > 360) {
                    if (angle >= start || angle <= (end - 360)) {
                        return i;
                    }
                } else {
                    if (angle >= start && angle <= end) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static String getCategoryFromKeterangan(String keterangan) {
        if (keterangan == null || keterangan.isEmpty()) return "Lainnya";
        int dashIdx = keterangan.indexOf(" - ");
        String prefix = (dashIdx > 0) ? keterangan.substring(0, dashIdx) : keterangan;
        int spaceIdx = prefix.indexOf(' ');
        if (spaceIdx > 0 && spaceIdx <= 4) {
            return prefix.substring(spaceIdx + 1).trim();
        }
        return prefix.trim();
    }

    private Color getDarkColor(Color c) {
        int r = (int) (c.getRed() * 0.7);
        int g = (int) (c.getGreen() * 0.7);
        int b = (int) (c.getBlue() * 0.7);
        return new Color(r, g, b, c.getAlpha());
    }

    private String formatRupiah(double val) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("Rp #,##0", symbols);
        return df.format(val);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. Gather expense data
        Map<String, Double> expenses = new LinkedHashMap<>();
        double totalExpense = 0.0;

        for (Transaction t : transactions) {
            if ("Pengeluaran".equals(t.getJenis())) {
                String cat = getCategoryFromKeterangan(t.getKeterangan());
                expenses.merge(cat, t.getNominal(), Double::sum);
                totalExpense += t.getNominal();
            }
        }

        if (totalExpense == 0) {
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(TEXT_GRAY);
            g2.drawString("Tidak ada data pengeluaran", width / 2 - 70, height / 2);
            return;
        }

        // Sort slices descending
        List<Map.Entry<String, Double>> list = new ArrayList<>(expenses.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Group into top 5 slices + Others
        slices.clear();
        double runningAngle = 0;
        int maxSlices = Math.min(5, list.size());
        double otherSum = 0;

        for (int i = 0; i < list.size(); i++) {
            if (i < maxSlices) {
                SliceInfo slice = new SliceInfo();
                slice.index = i;
                slice.category = list.get(i).getKey();
                slice.value = list.get(i).getValue();
                slice.percentage = (slice.value / totalExpense) * 100.0;
                slice.sweepAngle = (slice.value / totalExpense) * 360.0;
                slice.startAngle = runningAngle;
                slice.color = colors[i % colors.length];
                slices.add(slice);
                runningAngle += slice.sweepAngle;
            } else {
                otherSum += list.get(i).getValue();
            }
        }

        if (otherSum > 0) {
            SliceInfo other = new SliceInfo();
            other.index = maxSlices;
            other.category = "Lainnya";
            other.value = otherSum;
            other.percentage = (otherSum / totalExpense) * 100.0;
            other.sweepAngle = (otherSum / totalExpense) * 360.0;
            other.startAngle = runningAngle;
            other.color = new Color(180, 180, 190);
            slices.add(other);
        }

        // Apply animation sweep scaling
        for (SliceInfo slice : slices) {
            slice.sweepAngle = slice.sweepAngle * animProgress;
        }

        int cx = width / 2;
        int cy = height / 2 - 10;
        int rx = Math.min(width, height) / 3 - 5;
        int ry = (int) (rx * 0.58);
        int thickness = 14;

        // 2. Draw 3D side edge of the cylinder (bottom-to-top rendering)
        for (int t = thickness; t > 0; t--) {
            for (int i = 0; i < slices.size(); i++) {
                SliceInfo slice = slices.get(i);
                if (slice.sweepAngle <= 0) continue;

                double midAngle = slice.startAngle + slice.sweepAngle / 2.0;
                double dxShift = 0;
                double dyShift = 0;

                // Explode hovered slice outwards
                if (i == hoveredSliceIdx) {
                    dxShift = 8 * Math.cos(Math.toRadians(midAngle));
                    dyShift = -8 * Math.sin(Math.toRadians(midAngle));
                }

                g2.setColor(getDarkColor(slice.color));
                g2.fill(new Arc2D.Double(cx - rx + dxShift, cy - ry + dyShift + t, rx * 2, ry * 2, slice.startAngle, slice.sweepAngle, Arc2D.PIE));
            }
        }

        // 3. Draw top surface slices
        for (int i = 0; i < slices.size(); i++) {
            SliceInfo slice = slices.get(i);
            if (slice.sweepAngle <= 0) continue;

            double midAngle = slice.startAngle + slice.sweepAngle / 2.0;
            double dxShift = 0;
            double dyShift = 0;

            if (i == hoveredSliceIdx) {
                dxShift = 8 * Math.cos(Math.toRadians(midAngle));
                dyShift = -8 * Math.sin(Math.toRadians(midAngle));
            }

            g2.setColor(slice.color);
            g2.fill(new Arc2D.Double(cx - rx + dxShift, cy - ry + dyShift, rx * 2, ry * 2, slice.startAngle, slice.sweepAngle, Arc2D.PIE));

            // Draw clean outline
            g2.setColor(new Color(255, 255, 255, 60));
            g2.draw(new Arc2D.Double(cx - rx + dxShift, cy - ry + dyShift, rx * 2, ry * 2, slice.startAngle, slice.sweepAngle, Arc2D.PIE));
        }

        // 4. Draw pointer lines and labels
        g2.setStroke(new BasicStroke(1.0f));
        for (int i = 0; i < slices.size(); i++) {
            SliceInfo slice = slices.get(i);
            if (slice.sweepAngle <= 0) continue;

            double midAngle = slice.startAngle + slice.sweepAngle / 2.0;
            double dxShift = 0;
            double dyShift = 0;

            if (i == hoveredSliceIdx) {
                dxShift = 8 * Math.cos(Math.toRadians(midAngle));
                dyShift = -8 * Math.sin(Math.toRadians(midAngle));
            }

            // Boundary point on ellipse
            double mx = cx + dxShift + rx * Math.cos(Math.toRadians(midAngle));
            double my = cy + dyShift - ry * Math.sin(Math.toRadians(midAngle));

            // Outward pointer point
            double ex = cx + dxShift + (rx + 22) * Math.cos(Math.toRadians(midAngle));
            double ey = cy + dyShift - (ry + 22) * Math.sin(Math.toRadians(midAngle));

            // Horizontal line
            boolean isRightSide = (ex > cx);
            double hx = ex + (isRightSide ? 25 : -25);

            // Pointer line style (soft purple/gray color)
            g2.setColor(new Color(180, 170, 205));
            g2.draw(new Line2D.Double(mx, my, ex, ey));
            g2.draw(new Line2D.Double(ex, ey, hx, ey));
            
            // Draw a small dot at slice boundary
            g2.setColor(slice.color);
            g2.fill(new Ellipse2D.Double(mx - 2, my - 2, 4, 4));

            // Text content
            String categoryText = slice.category;
            String percentageText = String.format("%.1f%%", slice.percentage).replace(".", ",");

            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(TEXT_DARK);
            int catW = g2.getFontMetrics().stringWidth(categoryText);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(TEXT_GRAY);
            int pctW = g2.getFontMetrics().stringWidth(percentageText);

            int tx = (int) (isRightSide ? hx + 5 : hx - Math.max(catW, pctW) - 5);
            int ty = (int) ey;

            // Draw Category (slightly higher)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(TEXT_DARK);
            g2.drawString(categoryText, tx, ty - 3);

            // Draw Percentage (slightly lower)
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(TEXT_GRAY);
            g2.drawString(percentageText, tx, ty + 10);
        }

        // 5. Draw Tooltip for hovered slice
        if (hoveredSliceIdx >= 0 && hoveredSliceIdx < slices.size() && mousePoint != null) {
            SliceInfo slice = slices.get(hoveredSliceIdx);
            
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            String valStr = formatRupiah(slice.value);
            String labelStr = slice.category + " (" + String.format("%.1f%%", slice.percentage).replace(".", ",") + ")";

            int tooltipW = Math.max(g2.getFontMetrics().stringWidth(valStr), g2.getFontMetrics().stringWidth(labelStr)) + 24;
            int tooltipH = 46;

            int tx = mousePoint.x - (tooltipW / 2);
            int ty = mousePoint.y - tooltipH - 15;

            if (tx < 10) tx = 10;
            if (tx + tooltipW > width - 10) tx = width - tooltipW - 10;
            if (ty < 5) ty = mousePoint.y + 15;

            g2.setColor(new Color(30, 18, 58, 235));
            g2.fillRoundRect(tx, ty, tooltipW, tooltipH, 10, 10);

            g2.setColor(new Color(180, 150, 230, 150));
            g2.drawRoundRect(tx, ty, tooltipW, tooltipH, 10, 10);

            g2.setColor(new Color(210, 200, 230));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(labelStr, tx + 12, ty + 18);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString(valStr, tx + 12, ty + 35);
        }
    }
}
