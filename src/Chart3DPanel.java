import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;

public class Chart3DPanel extends JPanel {
    private TransactionManager manager;
    private List<Transaction> transactions;
    
    // Theme colors
    private final Color ACCENT_GREEN = new Color(46, 204, 113);
    private final Color ACCENT_RED   = new Color(231, 76, 60);
    private final Color TEXT_DARK    = new Color(28, 14, 56);
    private final Color TEXT_GRAY    = new Color(150, 140, 180);
    private final Color GRID_COLOR   = new Color(225, 218, 238, 150);

    // 3D Colors - Green
    private final Color GREEN_FRONT_START = new Color(58, 222, 128);
    private final Color GREEN_FRONT_END   = new Color(38, 184, 98);
    private final Color GREEN_TOP         = new Color(95, 240, 160);
    private final Color GREEN_SIDE        = new Color(30, 152, 78);

    // 3D Colors - Red
    private final Color RED_FRONT_START   = new Color(245, 98, 81);
    private final Color RED_FRONT_END     = new Color(211, 56, 41);
    private final Color RED_TOP           = new Color(255, 132, 115);
    private final Color RED_SIDE          = new Color(175, 40, 28);

    // Chart layouts
    private final int leftPadding = 90;
    private final int rightPadding = 40;
    private final int topPadding = 45;
    private final int bottomPadding = 55;

    // 3D Projection offset
    private final double dx = 12;
    private final double dy = -9;

    // Animation variables
    private double animProgress = 0.0;
    private javax.swing.Timer animTimer;

    // Interactive variables
    private List<Bar3DInfo> bars = new ArrayList<>();
    private int hoveredBarIdx = -1;
    private Point mousePoint = null;

    private static class Bar3DInfo {
        int index;
        String monthLabel;
        String type; // "Pemasukan" or "Pengeluaran"
        double value;
        Path2D path; // Combined 3D shape for hover detection
        
        // Colors
        Color frontStart;
        Color frontEnd;
        Color topColor;
        Color sideColor;

        // Shape details
        double x, y, w, h;
    }

    public Chart3DPanel(TransactionManager manager) {
        this.manager = manager;
        this.transactions = manager.getTransactions();
        setOpaque(false);

        // Hover tracking
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePoint = e.getPoint();
                int oldHover = hoveredBarIdx;
                hoveredBarIdx = -1;
                for (int i = 0; i < bars.size(); i++) {
                    if (bars.get(i).path.contains(mousePoint)) {
                        hoveredBarIdx = i;
                        break;
                    }
                }
                if (oldHover != hoveredBarIdx) {
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredBarIdx = -1;
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

    private List<String> getLast6Months() {
        List<String> months = new ArrayList<>();
        int maxYear = Calendar.getInstance().get(Calendar.YEAR);
        int maxMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;

        // Find max year and month in the transaction records to handle arbitrary datasets
        for (Transaction t : transactions) {
            try {
                String[] parts = t.getTanggal().split("-");
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                if (y > maxYear || (y == maxYear && m > maxMonth)) {
                    maxYear = y;
                    maxMonth = m;
                }
            } catch (Exception ignored) {}
        }

        // Generate the 6-month sequence ending at the max month
        for (int i = 5; i >= 0; i--) {
            int y = maxYear;
            int m = maxMonth - i;
            while (m <= 0) {
                m += 12;
                y -= 1;
            }
            months.add(String.format("%04d-%02d", y, m));
        }
        return months;
    }

    private String getIndonesianMonthLabel(String yearMonth) {
        String[] parts = yearMonth.split("-");
        int m = Integer.parseInt(parts[1]);
        String yy = parts[0].substring(2);
        String[] months = {"Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des"};
        return months[m - 1] + " '" + yy;
    }

    private String formatRupiah(double val) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("Rp #,##0", symbols);
        return df.format(val);
    }

    private String formatShortRupiah(double val) {
        if (val == 0) return "Rp 0";
        if (val >= 1_000_000_000) {
            return String.format("Rp %.1fM", val / 1_000_000_000.0).replace(".0", "");
        } else if (val >= 1_000_000) {
            return String.format("Rp %.1fJt", val / 1_000_000.0).replace(".0", "");
        } else if (val >= 1_000) {
            return String.format("Rp %.1fRb", val / 1_000.0).replace(".0", "");
        } else {
            return "Rp " + (int) val;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        int chartWidth = width - leftPadding - rightPadding;
        int chartHeight = height - topPadding - bottomPadding;
        int baseY = height - bottomPadding;

        // 1. Gather data for the last 6 months
        List<String> last6Months = getLast6Months();
        double[] incomes = new double[6];
        double[] expenses = new double[6];
        double maxVal = 0.0;

        for (int i = 0; i < 6; i++) {
            String ym = last6Months.get(i);
            for (Transaction t : transactions) {
                if (t.getTanggal().startsWith(ym)) {
                    if (t.getJenis().equals("Pemasukan")) {
                        incomes[i] += t.getNominal();
                    } else {
                        expenses[i] += t.getNominal();
                    }
                }
            }
            maxVal = Math.max(maxVal, incomes[i]);
            maxVal = Math.max(maxVal, expenses[i]);
        }

        // Default max if zero
        if (maxVal == 0.0) {
            maxVal = 1_000_000.0;
        }

        // Round maxVal up to clean numbers (e.g. multiples of 100K, 1M, etc.)
        double orderOfMagnitude = Math.pow(10, Math.floor(Math.log10(maxVal)));
        double step = orderOfMagnitude / 2.0;
        if (step == 0) step = 1;
        maxVal = Math.ceil(maxVal / step) * step;

        // 2. Draw Back-wall Grid and Y Axis Labels
        int gridDivisions = 4;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int i = 0; i <= gridDivisions; i++) {
            double value = (maxVal / gridDivisions) * i;
            int y = baseY - (int) ((value / maxVal) * chartHeight);

            // Draw Y-axis text
            g2.setColor(TEXT_GRAY);
            String label = formatShortRupiah(value);
            int labelW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, leftPadding - labelW - 15, y + 4);

            // Gridline
            if (i > 0) {
                g2.setColor(GRID_COLOR);
                g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f, 4f}, 0f));
            } else {
                g2.setColor(new Color(200, 190, 220));
                g2.setStroke(new BasicStroke(1.5f));
            }
            g2.draw(new Line2D.Double(leftPadding, y, leftPadding + chartWidth, y));
        }

        // Draw 3D floor platform base
        Path2D floor = new Path2D.Double();
        floor.moveTo(leftPadding, baseY);
        floor.lineTo(leftPadding + chartWidth, baseY);
        floor.lineTo(leftPadding + chartWidth + dx, baseY + dy);
        floor.lineTo(leftPadding + dx, baseY + dy);
        floor.closePath();
        g2.setColor(new Color(230, 225, 245, 100));
        g2.fill(floor);
        g2.setColor(new Color(200, 190, 220, 150));
        g2.draw(floor);

        // 3. Prepare to draw columns
        bars.clear();
        double groupWidth = (double) chartWidth / 6.0;
        double barWidth = (groupWidth - 25) / 2.0; // 25px gap between months
        if (barWidth < 12) barWidth = 12;

        int barIndex = 0;

        for (int i = 0; i < 6; i++) {
            double groupCenterX = leftPadding + (i * groupWidth) + (groupWidth / 2.0);
            
            // X position for Pemasukan and Pengeluaran
            double xInc = groupCenterX - barWidth - 3;
            double xExp = groupCenterX + 3;

            // Height with animation
            double hInc = (incomes[i] / maxVal) * chartHeight * animProgress;
            double hExp = (expenses[i] / maxVal) * chartHeight * animProgress;

            // --- 3A. Draw Pemasukan (Green) Bar ---
            if (incomes[i] > 0 || !manager.getTransactions().isEmpty()) {
                Bar3DInfo incBar = createBarInfo(barIndex++, last6Months.get(i), "Pemasukan", incomes[i], xInc, baseY, barWidth, hInc, 
                                                GREEN_FRONT_START, GREEN_FRONT_END, GREEN_TOP, GREEN_SIDE);
                bars.add(incBar);
            }

            // --- 3B. Draw Pengeluaran (Red) Bar ---
            if (expenses[i] > 0 || !manager.getTransactions().isEmpty()) {
                Bar3DInfo expBar = createBarInfo(barIndex++, last6Months.get(i), "Pengeluaran", expenses[i], xExp, baseY, barWidth, hExp, 
                                                RED_FRONT_START, RED_FRONT_END, RED_TOP, RED_SIDE);
                bars.add(expBar);
            }

            // Draw X Axis labels
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(TEXT_DARK);
            String monthLabel = getIndonesianMonthLabel(last6Months.get(i));
            int mLabelW = g2.getFontMetrics().stringWidth(monthLabel);
            g2.drawString(monthLabel, (int) (groupCenterX - (mLabelW / 2.0)), baseY + 20);
        }

        // Render shadow of all bars first, then the actual columns so shadows sit beneath adjacent bars
        g2.setStroke(new BasicStroke(1.0f));
        for (Bar3DInfo bar : bars) {
            if (bar.h > 2) {
                // Drop shadow on the floor (parallelogram)
                Path2D shadow = new Path2D.Double();
                shadow.moveTo(bar.x, bar.y + 4);
                shadow.lineTo(bar.x + bar.w, bar.y + 4);
                shadow.lineTo(bar.x + bar.w + dx, bar.y + dy + 4);
                shadow.lineTo(bar.x + dx, bar.y + dy + 4);
                shadow.closePath();
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fill(shadow);
            }
        }

        // Render actual 3D columns
        for (int i = 0; i < bars.size(); i++) {
            Bar3DInfo bar = bars.get(i);
            boolean isHovered = (i == hoveredBarIdx);
            drawBar3D(g2, bar, isHovered);
        }

        // 4. Render Tooltip if hovered
        if (hoveredBarIdx >= 0 && hoveredBarIdx < bars.size() && mousePoint != null) {
            Bar3DInfo bar = bars.get(hoveredBarIdx);
            
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            String valStr = formatRupiah(bar.value);
            String labelStr = getIndonesianMonthLabel(bar.monthLabel) + " - " + bar.type;

            int tooltipW = Math.max(g2.getFontMetrics().stringWidth(valStr), g2.getFontMetrics().stringWidth(labelStr)) + 24;
            int tooltipH = 46;

            int tx = mousePoint.x - (tooltipW / 2);
            int ty = mousePoint.y - tooltipH - 15;

            // Keep tooltip within component bounds
            if (tx < 10) tx = 10;
            if (tx + tooltipW > width - 10) tx = width - tooltipW - 10;
            if (ty < 5) ty = mousePoint.y + 15;

            // Draw glassmorphic tooltip card
            g2.setColor(new Color(30, 18, 58, 235));
            g2.fillRoundRect(tx, ty, tooltipW, tooltipH, 10, 10);
            
            // Soft border glow
            g2.setColor(bar.type.equals("Pemasukan") ? new Color(46, 204, 113, 150) : new Color(231, 76, 60, 150));
            g2.drawRoundRect(tx, ty, tooltipW, tooltipH, 10, 10);

            // Text content
            g2.setColor(new Color(210, 200, 230));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(labelStr, tx + 12, ty + 18);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString(valStr, tx + 12, ty + 35);
        }
    }

    private Bar3DInfo createBarInfo(int index, String monthLabel, String type, double value, double x, double baseY, double w, double h,
                                    Color frontStart, Color frontEnd, Color topColor, Color sideColor) {
        Bar3DInfo info = new Bar3DInfo();
        info.index = index;
        info.monthLabel = monthLabel;
        info.type = type;
        info.value = value;
        info.x = x;
        info.y = baseY;
        info.w = w;
        info.h = h;
        info.frontStart = frontStart;
        info.frontEnd = frontEnd;
        info.topColor = topColor;
        info.sideColor = sideColor;

        // Path representing the full 3D boundary of this bar
        Path2D p = new Path2D.Double();
        // Front Face
        p.moveTo(x, baseY);
        p.lineTo(x + w, baseY);
        p.lineTo(x + w, baseY - h);
        p.lineTo(x, baseY - h);
        // Top Face
        p.lineTo(x + dx, baseY - h + dy);
        p.lineTo(x + w + dx, baseY - h + dy);
        p.lineTo(x + w, baseY - h);
        // Side Face
        p.moveTo(x + w, baseY);
        p.lineTo(x + w + dx, baseY + dy);
        p.lineTo(x + w + dx, baseY - h + dy);
        p.lineTo(x + w, baseY - h);
        p.closePath();

        info.path = p;
        return info;
    }

    private void drawBar3D(Graphics2D g2, Bar3DInfo bar, boolean isHovered) {
        if (bar.h <= 0) return;

        double x = bar.x;
        double y = bar.y;
        double w = bar.w;
        double h = bar.h;

        // Colors (with hover glow effect)
        Color fs = bar.frontStart;
        Color fe = bar.frontEnd;
        Color tc = bar.topColor;
        Color sc = bar.sideColor;

        if (isHovered) {
            fs = getHighlightColor(fs);
            fe = getHighlightColor(fe);
            tc = getHighlightColor(tc);
            sc = getHighlightColor(sc);
        }

        // Draw Front Face
        Rectangle2D front = new Rectangle2D.Double(x, y - h, w, h);
        GradientPaint grad = new GradientPaint((float) x, (float) (y - h), fs, (float) x, (float) y, fe);
        g2.setPaint(grad);
        g2.fill(front);

        // Draw Top Face
        Path2D top = new Path2D.Double();
        top.moveTo(x, y - h);
        top.lineTo(x + dx, y - h + dy);
        top.lineTo(x + w + dx, y - h + dy);
        top.lineTo(x + w, y - h);
        top.closePath();
        g2.setColor(tc);
        g2.fill(top);

        // Draw Side Face
        Path2D side = new Path2D.Double();
        side.moveTo(x + w, y - h);
        side.lineTo(x + w + dx, y - h + dy);
        side.lineTo(x + w + dx, y + dy);
        side.lineTo(x + w, y);
        side.closePath();
        g2.setColor(sc);
        g2.fill(side);

        // Subtle borders to make faces pop (especially in light backgrounds)
        g2.setColor(new Color(255, 255, 255, 90));
        g2.draw(front);
        g2.draw(top);
        g2.draw(side);

        // Golden hover ring/glow overlay
        if (isHovered) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(bar.path);
            g2.setStroke(new BasicStroke(1.0f));
        }
    }

    private Color getHighlightColor(Color c) {
        int r = Math.min(255, c.getRed() + 35);
        int g = Math.min(255, c.getGreen() + 35);
        int b = Math.min(255, c.getBlue() + 35);
        return new Color(r, g, b, c.getAlpha());
    }
}
