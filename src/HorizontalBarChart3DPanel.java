import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;

public class HorizontalBarChart3DPanel extends JPanel {
    private TransactionManager manager;
    private List<Transaction> transactions;

    // Theme Colors
    private final Color TEXT_DARK    = new Color(28, 14, 56);
    private final Color TEXT_GRAY    = new Color(150, 140, 180);
    private final Color GRID_COLOR   = new Color(225, 218, 238, 150);

    // 3D Colors - Income (Green)
    private final Color GREEN_FRONT_START = new Color(58, 222, 128);
    private final Color GREEN_FRONT_END   = new Color(38, 184, 98);
    private final Color GREEN_TOP         = new Color(95, 240, 160);
    private final Color GREEN_SIDE        = new Color(30, 152, 78);

    // 3D Colors - Expense (Red)
    private final Color RED_FRONT_START   = new Color(245, 98, 81);
    private final Color RED_FRONT_END     = new Color(211, 56, 41);
    private final Color RED_TOP           = new Color(255, 132, 115);
    private final Color RED_SIDE          = new Color(175, 40, 28);

    // Dimensions
    private final int leftPadding = 150; // Space for emoji + category labels
    private final int rightPadding = 45;
    private final int topPadding = 25;
    private final int bottomPadding = 45;

    // 3D Skew projection
    private final double dx = 10;
    private final double dy = -7;

    // Animation variables
    private double animProgress = 0.0;
    private javax.swing.Timer animTimer;

    // Interactive variables
    private List<Bar3DInfo> bars = new ArrayList<>();
    private int hoveredBarIdx = -1;
    private Point mousePoint = null;

    private static class Bar3DInfo {
        int index;
        String category;
        String emoji;
        String type; // "Pemasukan" or "Pengeluaran"
        double value;
        Path2D path;

        // Color theme
        Color frontStart;
        Color frontEnd;
        Color topColor;
        Color sideColor;

        // Shape details
        double x, y, w, h;
    }

    public HorizontalBarChart3DPanel(TransactionManager manager) {
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

    private static String getEmojiFromKeterangan(String keterangan) {
        if (keterangan == null || keterangan.isEmpty()) return "📌";
        int spaceIdx = keterangan.indexOf(' ');
        if (spaceIdx > 0 && spaceIdx <= 4) {
            return keterangan.substring(0, spaceIdx);
        }
        return "📌";
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

        // 1. Group transactions by category
        Map<String, Double> categoryTotals = new LinkedHashMap<>();
        Map<String, String> categoryEmojis = new HashMap<>();
        Map<String, String> categoryTypes = new HashMap<>();

        for (Transaction t : transactions) {
            String cat = getCategoryFromKeterangan(t.getKeterangan());
            String emoji = getEmojiFromKeterangan(t.getKeterangan());
            categoryTotals.merge(cat, t.getNominal(), Double::sum);
            categoryEmojis.put(cat, emoji);
            categoryTypes.put(cat, t.getJenis());
        }

        // Sort categories by total value (descending)
        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryTotals.entrySet());
        sortedCategories.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Display up to 5 categories to fit cleanly
        int maxItems = Math.min(5, sortedCategories.size());

        double maxVal = 0.0;
        for (int i = 0; i < maxItems; i++) {
            maxVal = Math.max(maxVal, sortedCategories.get(i).getValue());
        }
        if (maxVal == 0.0) maxVal = 1_000_000.0;

        // Round maxVal up
        double orderOfMagnitude = Math.pow(10, Math.floor(Math.log10(maxVal)));
        double step = orderOfMagnitude / 2.0;
        if (step == 0) step = 1;
        maxVal = Math.ceil(maxVal / step) * step;

        // 2. Draw Back-wall Grid and X Axis Labels (vertical lines)
        int gridDivisions = 3;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int i = 0; i <= gridDivisions; i++) {
            double value = (maxVal / gridDivisions) * i;
            int x = leftPadding + (int) ((value / maxVal) * chartWidth);

            // Draw X-axis text
            g2.setColor(TEXT_GRAY);
            String label = formatShortRupiah(value);
            int labelW = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, x - (labelW / 2), baseY + 18);

            // Gridline
            if (i > 0) {
                g2.setColor(GRID_COLOR);
                g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4f, 4f}, 0f));
            } else {
                g2.setColor(new Color(200, 190, 220));
                g2.setStroke(new BasicStroke(1.5f));
            }
            g2.draw(new Line2D.Double(x, topPadding, x, baseY));
        }

        // Draw 3D floor baseline platform
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

        // 3. Render categories and their 3D bars
        bars.clear();
        double rowHeight = (double) chartHeight / (maxItems == 0 ? 1 : maxItems);
        double barHeight = 16.0;

        for (int i = 0; i < maxItems; i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            String cat = entry.getKey();
            double val = entry.getValue();
            String emoji = categoryEmojis.get(cat);
            String type = categoryTypes.get(cat);

            double yCenter = topPadding + (i * rowHeight) + (rowHeight / 2.0);
            double barY = yCenter - (barHeight / 2.0);
            double barW = (val / maxVal) * chartWidth * animProgress;

            // Draw Y axis labels: Emoji + Category
            // Emoji Icon (Async downloaded Twemoji)
            ImageIcon emojiIcon = EmojiUtils.getIcon(emoji, 18, this);
            int emojiX = 15;
            int labelX = 40;

            if (emojiIcon != null) {
                emojiIcon.paintIcon(this, g2, emojiX, (int) (yCenter - 9));
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(TEXT_DARK);
            g2.drawString(cat, labelX, (int) (yCenter + 4));

            // Choose color based on type
            Color fs = GREEN_FRONT_START;
            Color fe = GREEN_FRONT_END;
            Color tc = GREEN_TOP;
            Color sc = GREEN_SIDE;

            if ("Pengeluaran".equals(type)) {
                fs = RED_FRONT_START;
                fe = RED_FRONT_END;
                tc = RED_TOP;
                sc = RED_SIDE;
            }

            // Create Bar3DInfo
            Bar3DInfo bar = createBarInfo(i, cat, emoji, type, val, leftPadding, barY, barW, barHeight, fs, fe, tc, sc);
            bars.add(bar);
        }

        // Draw Shadows first
        g2.setStroke(new BasicStroke(1.0f));
        for (Bar3DInfo bar : bars) {
            if (bar.w > 2) {
                Path2D shadow = new Path2D.Double();
                shadow.moveTo(bar.x, bar.y + bar.h + 3);
                shadow.lineTo(bar.x + bar.w, bar.y + bar.h + 3);
                shadow.lineTo(bar.x + bar.w + dx, bar.y + bar.h + dy + 3);
                shadow.lineTo(bar.x + dx, bar.y + bar.h + dy + 3);
                shadow.closePath();
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fill(shadow);
            }
        }

        // Draw 3D Bars
        for (int i = 0; i < bars.size(); i++) {
            Bar3DInfo bar = bars.get(i);
            boolean isHovered = (i == hoveredBarIdx);
            drawBar3D(g2, bar, isHovered);
        }

        // 4. Render tooltip
        if (hoveredBarIdx >= 0 && hoveredBarIdx < bars.size() && mousePoint != null) {
            Bar3DInfo bar = bars.get(hoveredBarIdx);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            String valStr = formatRupiah(bar.value);
            String labelStr = bar.emoji + " " + bar.category + " (" + bar.type + ")";

            int tooltipW = Math.max(g2.getFontMetrics().stringWidth(valStr), g2.getFontMetrics().stringWidth(labelStr)) + 24;
            int tooltipH = 46;

            int tx = mousePoint.x - (tooltipW / 2);
            int ty = mousePoint.y - tooltipH - 15;

            if (tx < 10) tx = 10;
            if (tx + tooltipW > width - 10) tx = width - tooltipW - 10;
            if (ty < 5) ty = mousePoint.y + 15;

            g2.setColor(new Color(30, 18, 58, 235));
            g2.fillRoundRect(tx, ty, tooltipW, tooltipH, 10, 10);

            g2.setColor(bar.type.equals("Pemasukan") ? new Color(46, 204, 113, 150) : new Color(231, 76, 60, 150));
            g2.drawRoundRect(tx, ty, tooltipW, tooltipH, 10, 10);

            g2.setColor(new Color(210, 200, 230));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(labelStr, tx + 12, ty + 18);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString(valStr, tx + 12, ty + 35);
        }
    }

    private Bar3DInfo createBarInfo(int index, String category, String emoji, String type, double value, double x, double y, double w, double h,
                                    Color frontStart, Color frontEnd, Color topColor, Color sideColor) {
        Bar3DInfo info = new Bar3DInfo();
        info.index = index;
        info.category = category;
        info.emoji = emoji;
        info.type = type;
        info.value = value;
        info.x = x;
        info.y = y;
        info.w = w;
        info.h = h;
        info.frontStart = frontStart;
        info.frontEnd = frontEnd;
        info.topColor = topColor;
        info.sideColor = sideColor;

        Path2D p = new Path2D.Double();
        // Front Face
        p.moveTo(x, y);
        p.lineTo(x + w, y);
        p.lineTo(x + w, y + h);
        p.lineTo(x, y + h);
        // Top Face
        p.lineTo(x + dx, y + dy);
        p.lineTo(x + w + dx, y + dy);
        p.lineTo(x + w, y);
        // Right Side Face
        p.moveTo(x + w, y);
        p.lineTo(x + w + dx, y + dy);
        p.lineTo(x + w + dx, y + h + dy);
        p.lineTo(x + w, y + h);
        p.closePath();

        info.path = p;
        return info;
    }

    private void drawBar3D(Graphics2D g2, Bar3DInfo bar, boolean isHovered) {
        if (bar.w <= 0) return;

        double x = bar.x;
        double y = bar.y;
        double w = bar.w;
        double h = bar.h;

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

        // Front Face
        Rectangle2D front = new Rectangle2D.Double(x, y, w, h);
        GradientPaint grad = new GradientPaint((float) x, (float) y, fs, (float) x, (float) (y + h), fe);
        g2.setPaint(grad);
        g2.fill(front);

        // Top Face
        Path2D top = new Path2D.Double();
        top.moveTo(x, y);
        top.lineTo(x + dx, y + dy);
        top.lineTo(x + w + dx, y + dy);
        top.lineTo(x + w, y);
        top.closePath();
        g2.setColor(tc);
        g2.fill(top);

        // Right End Face
        Path2D side = new Path2D.Double();
        side.moveTo(x + w, y);
        side.lineTo(x + w + dx, y + dy);
        side.lineTo(x + w + dx, y + h + dy);
        side.lineTo(x + w, y + h);
        side.closePath();
        g2.setColor(sc);
        g2.fill(side);

        // Borders
        g2.setColor(new Color(255, 255, 255, 90));
        g2.draw(front);
        g2.draw(top);
        g2.draw(side);

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
