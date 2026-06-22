import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public class MainFrame extends JFrame {

    private TransactionManager manager;
    private CategoryManager categoryManager;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    private int laporanSelectedBulan = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH);
    private String laporanSelectedTahun = String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));

    private int dashboardFilterBulan = -1;
    private String dashboardFilterTahun = "Semua Tahun";
    private int transaksiFilterBulan = -1;
    private String transaksiFilterTahun = "Semua Tahun";
    private List<Transaction> currentTableTransactions = new ArrayList<>();

    private final Color BG_DARK      = new Color(30, 15, 60);
    private final Color BG_SIDEBAR   = new Color(28, 14, 56);   // Daisy Bush dark
    private final Color BG_CARD      = new Color(255, 255, 255);
    private final Color BG_MAIN      = new Color(243, 240, 250); // very light purple tint
    private final Color ACCENT_GREEN  = new Color(46, 204, 113);
    private final Color ACCENT_RED    = new Color(231, 76, 60);
    private final Color ACCENT_BLUE   = new Color(115, 72, 195); // Fuchsia Blue (used as primary)
    private final Color ACCENT_PURPLE = new Color(164, 96, 214); // Medium Purple
    private final Color ACCENT_LAVENDER = new Color(204, 144, 224); // Lavender
    private final Color TEXT_WHITE    = new Color(240, 235, 255);
    private final Color TEXT_GRAY     = new Color(180, 160, 210);
    private final Color TEXT_DARK     = new Color(28, 14, 56);

    private JLabel[] menuLabels;
    private String[] menuNames = {"Dashboard", "Transaksi", "Kategori", "Laporan", "About"};
    private String[] menuIcons = {"[D]", "[T]", "[K]", "[L]", "[A]"};

    public MainFrame(TransactionManager manager, CategoryManager categoryManager) {
        this.manager = manager;
        this.categoryManager = categoryManager;
        setTitle("MoneyFlow - Personal Finance Tracker");
        setSize(1200, 750);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.add(buildDashboardPanel(), "Dashboard");
        contentPanel.add(buildTransaksiPanel(), "Transaksi");
        contentPanel.add(buildKategoriPanel(), "Kategori");
        contentPanel.add(buildLaporanPanel(), "Laporan");
        contentPanel.add(buildAboutPanel(), "About");

        add(contentPanel, BorderLayout.CENTER);
        switchMenu(0);
    }

    // ===================== SIDEBAR =====================
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(210, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(25, 0, 25, 0));

        // Gradient logo area
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        logoPanel.setBackground(new Color(66, 34, 162)); // Daisy Bush
        logoPanel.setMaximumSize(new Dimension(220, 54));
        JLabel logo = new JLabel("MoneyFlow");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoPanel.add(logo);
        sidebar.add(logoPanel);
        sidebar.add(Box.createVerticalStrut(28));

        menuLabels = new JLabel[menuNames.length];
        for (int i = 0; i < menuNames.length; i++) {
            final int idx = i;
            MenuButton menuItem = new MenuButton(menuNames[i]);
            menuItem.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { switchMenu(idx); }
            });
            sidebar.add(menuItem);
            sidebar.add(Box.createVerticalStrut(2));
            menuLabels[i] = menuItem.getLabel(); // keep reference for compatibility if needed
        }

        sidebar.add(Box.createVerticalGlue());
        
        JPanel footerCard = new RoundedPanel(12, 0);
        footerCard.setBackground(new Color(38, 20, 78));
        footerCard.setLayout(new BorderLayout(8, 0));
        footerCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        footerCard.setMaximumSize(new Dimension(170, 48));
        footerCard.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLabel = new JLabel("✨");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconLabel.setForeground(Color.WHITE);
        
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        
        JLabel mainText = new JLabel("MoneyFlow Pro");
        mainText.setForeground(Color.WHITE);
        mainText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        
        JLabel subText = new JLabel("Secure Local Database");
        subText.setForeground(new Color(170, 150, 200));
        subText.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        
        textPanel.add(mainText);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subText);
        
        footerCard.add(iconLabel, BorderLayout.WEST);
        footerCard.add(textPanel, BorderLayout.CENTER);
        
        sidebar.add(footerCard);
        sidebar.add(Box.createVerticalStrut(15));

        return sidebar;
    }

    private void switchMenu(int idx) {
        Component[] comps = ((JPanel)getContentPane().getComponent(0)).getComponents();
        int menuIndex = 0;
        for (Component c : comps) {
            if (c instanceof MenuButton) {
                ((MenuButton) c).setActive(menuIndex == idx);
                menuIndex++;
            }
        }
        cardLayout.show(contentPanel, menuNames[idx]);
    }

    // ===================== DASHBOARD =====================
    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(TEXT_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Filter Setup
        Set<String> tahunSet = new TreeSet<>(Collections.reverseOrder());
        for (Transaction t : manager.getTransactions()) {
            try {
                String yyyy = t.getTanggal().substring(0, 4);
                tahunSet.add(yyyy);
            } catch (Exception e) {}
        }
        if (tahunSet.isEmpty()) {
            tahunSet.add(String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)));
        }
        List<String> tahunList = new ArrayList<>(tahunSet);
        tahunList.add(0, "Semua Tahun");

        String[] bulanList = {"Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                              "Juli", "Agustus", "September", "Oktober", "November", "Desember"};

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setBackground(BG_MAIN);
        filterRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JComboBox<String> cbBulan = new JComboBox<>(bulanList);
        cbBulan.setSelectedIndex(dashboardFilterBulan + 1);
        cbBulan.setPreferredSize(new Dimension(130, 32));
        styleComboBox(cbBulan);

        JComboBox<String> cbTahun = new JComboBox<>(tahunList.toArray(new String[0]));
        cbTahun.setSelectedItem(dashboardFilterTahun);
        cbTahun.setPreferredSize(new Dimension(110, 32));
        styleComboBox(cbTahun);

        RoundedButton btnClear = new RoundedButton("Hapus Filter", 8);
        btnClear.setBackground(new Color(230, 80, 80));
        btnClear.setForeground(Color.WHITE);
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        btnClear.setPreferredSize(new Dimension(110, 32));
        btnClear.setVisible(dashboardFilterBulan != -1 || !dashboardFilterTahun.equals("Semua Tahun"));

        cbBulan.addActionListener(e -> {
            dashboardFilterBulan = cbBulan.getSelectedIndex() - 1;
            refreshDashboardPanel();
        });
        cbTahun.addActionListener(e -> {
            dashboardFilterTahun = (String) cbTahun.getSelectedItem();
            refreshDashboardPanel();
        });
        btnClear.addActionListener(e -> {
            dashboardFilterBulan = -1;
            dashboardFilterTahun = "Semua Tahun";
            refreshDashboardPanel();
        });

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        filterLabel.setForeground(TEXT_DARK);

        filterRow.add(filterLabel);
        filterRow.add(cbBulan);
        filterRow.add(cbTahun);
        filterRow.add(btnClear);

        // Fetch Filtered Data
        List<Transaction> filteredList = getFilteredTransactions(dashboardFilterBulan, dashboardFilterTahun);
        double totalPemasukan = 0;
        double totalPengeluaran = 0;
        for (Transaction t : filteredList) {
            if ("Pemasukan".equals(t.getJenis())) {
                totalPemasukan += t.getNominal();
            } else {
                totalPengeluaran += t.getNominal();
            }
        }
        double totalSaldo = totalPemasukan - totalPengeluaran;

        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        cardsPanel.setBackground(BG_MAIN);
        cardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        cardsPanel.add(buildSummaryCard("Pemasukan", formatRupiah(totalPemasukan), ACCENT_GREEN));
        cardsPanel.add(buildSummaryCard("Pengeluaran", formatRupiah(totalPengeluaran), ACCENT_RED));
        cardsPanel.add(buildSummaryCard("Saldo", formatRupiah(totalSaldo), ACCENT_BLUE));

        JPanel chartRow = new JPanel(new GridLayout(1, 2, 20, 0));
        chartRow.setBackground(BG_MAIN);
        chartRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        chartRow.setPreferredSize(new Dimension(0, 320));

        // Left Card - Pemasukan & Pengeluaran per Kategori
        RoundedPanel leftCard = new RoundedPanel(16, 6);
        leftCard.setLayout(new BorderLayout());
        leftCard.setBackground(BG_CARD);
        leftCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        leftCard.setHeader(50, new Color(225, 218, 242));

        JLabel leftTitle = new JLabel("Pemasukan & Pengeluaran per Kategori");
        leftTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        leftTitle.setForeground(TEXT_DARK);
        leftTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        HorizontalBarChart3DPanel leftChart = new HorizontalBarChart3DPanel(manager);
        leftChart.setTransactions(filteredList);
        leftCard.add(leftTitle, BorderLayout.NORTH);
        leftCard.add(leftChart, BorderLayout.CENTER);

        // Right Card - Penggunaan Uang per Kategori
        RoundedPanel rightCard = new RoundedPanel(16, 6);
        rightCard.setLayout(new BorderLayout());
        rightCard.setBackground(BG_CARD);
        rightCard.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        rightCard.setHeader(50, new Color(225, 218, 242));

        JLabel rightTitle = new JLabel("Penggunaan Uang per Kategori");
        rightTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        rightTitle.setForeground(TEXT_DARK);
        rightTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        PieChart3DPanel rightChart = new PieChart3DPanel(manager);
        rightChart.setTransactions(filteredList);
        rightCard.add(rightTitle, BorderLayout.NORTH);
        rightCard.add(rightChart, BorderLayout.CENTER);

        chartRow.add(leftCard);
        chartRow.add(rightCard);

        JPanel recentPanel = buildRecentTransaksi();

        JPanel centerPanel = new JPanel();
        centerPanel.setBackground(BG_MAIN);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(cardsPanel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(chartRow);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(recentPanel);

        JScrollPane scroll = new JScrollPane(centerPanel);
        styleScrollPane(scroll);
        scroll.getViewport().setBackground(BG_MAIN);

        JPanel topHeader = new JPanel();
        topHeader.setBackground(BG_MAIN);
        topHeader.setLayout(new BoxLayout(topHeader, BoxLayout.Y_AXIS));
        topHeader.add(title);
        topHeader.add(filterRow);

        panel.add(topHeader, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSummaryCard(String label, String value, Color color) {
        RoundedPanel card = new RoundedPanel(16, 6);
        card.setLayout(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        // Colored left bar
        JPanel left = new JPanel();
        left.setBackground(color);
        left.setPreferredSize(new Dimension(5, 0));

        JPanel textPanel = new JPanel();
        textPanel.setBackground(BG_CARD);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));

        JLabel lbTitle = new JLabel(label.toUpperCase());
        lbTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbTitle.setForeground(new Color(160, 140, 190));

        JLabel lbValue = new JLabel(value);
        lbValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbValue.setForeground(color);

        textPanel.add(lbTitle);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(lbValue);

        card.add(left, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecentTransaksi() {
        RoundedPanel card = new RoundedPanel(18, 8);
        card.setLayout(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        card.setHeader(60, new Color(225, 218, 242));

        JLabel title = new JLabel("Transaksi Terbaru");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel listPanel = new JPanel();
        listPanel.setBackground(BG_CARD);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        List<Transaction> all = getFilteredTransactions(dashboardFilterBulan, dashboardFilterTahun);
        int start = Math.max(0, all.size() - 5);
        for (int i = all.size() - 1; i >= start; i--) {
            Transaction t = all.get(i);
            listPanel.add(buildRecentRow(t));
            if (i > start) {
                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(235, 228, 248));
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                listPanel.add(sep);
            }
        }

        if (all.isEmpty()) {
            JLabel kosong = new JLabel("Belum ada transaksi");
            kosong.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            kosong.setForeground(TEXT_GRAY);
            listPanel.add(kosong);
        }

        card.add(title, BorderLayout.NORTH);
        card.add(listPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecentRow(Transaction t) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_CARD);
        row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        // Left icon box
        JPanel colorBox = new JPanel();
        boolean isPemasukan = t.getJenis().equals("Pemasukan");
        colorBox.setBackground(isPemasukan ? new Color(46, 204, 113, 45) : new Color(231, 76, 60, 45));
        colorBox.setPreferredSize(new Dimension(42, 42));
        colorBox.setLayout(new GridBagLayout());
        JLabel sign = new JLabel(isPemasukan ? "+" : "-");
        sign.setFont(new Font("Segoe UI", Font.BOLD, 18));
        sign.setForeground(isPemasukan ? ACCENT_GREEN : ACCENT_RED);
        colorBox.add(sign);

        JPanel info = new JPanel();
        info.setBackground(BG_CARD);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));

        JLabel keterangan = new JLabel();
        String text = t.getKeterangan();
        int spaceIdx = text.indexOf(' ');
        if (spaceIdx > 0 && spaceIdx <= 3) {
            String emojiStr = text.substring(0, spaceIdx);
            keterangan.setText(text.substring(spaceIdx));
            keterangan.setIcon(EmojiUtils.getIcon(emojiStr, 18, keterangan));
            keterangan.setIconTextGap(8);
        } else {
            keterangan.setText(text);
        }
        keterangan.setFont(new Font("Segoe UI", Font.BOLD, 13));
        keterangan.setForeground(TEXT_DARK);

        JLabel tanggal = new JLabel(t.getTanggal() + "  ·  " + t.getJenis());
        tanggal.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        tanggal.setForeground(new Color(170, 150, 200));

        info.add(keterangan);
        info.add(tanggal);

        JLabel nominal = new JLabel((isPemasukan ? "+" : "-") + formatRupiah(t.getNominal()));
        nominal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nominal.setForeground(isPemasukan ? ACCENT_GREEN : ACCENT_RED);

        row.add(colorBox, BorderLayout.WEST);
        row.add(info, BorderLayout.CENTER);
        row.add(nominal, BorderLayout.EAST);
        return row;
    }

    // ===================== DONUT CHART =====================
    private JPanel buildDonutCard(String judul, boolean isIncome) {
        RoundedPanel card = new RoundedPanel(16, 6);
        card.setLayout(new BorderLayout());
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setHeader(48, new Color(225, 218, 242));

        JLabel title = new JLabel(judul);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(TEXT_DARK);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        Map<String, Double> data = new LinkedHashMap<>();
        for (Transaction t : manager.getTransactions()) {
            if (isIncome == t.getJenis().equals("Pemasukan")) {
                data.merge(t.getKeterangan(), t.getNominal(), Double::sum);
            }
        }

        DonutChartPanel chart = new DonutChartPanel(data);
        chart.setBackground(BG_CARD);

        card.add(title, BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    // ===================== TRANSAKSI =====================
    private JPanel buildTransaksiPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_MAIN);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Daftar Transaksi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_DARK);

        // Filter Setup
        Set<String> tahunSet = new TreeSet<>(Collections.reverseOrder());
        for (Transaction t : manager.getTransactions()) {
            try {
                String yyyy = t.getTanggal().substring(0, 4);
                tahunSet.add(yyyy);
            } catch (Exception e) {}
        }
        if (tahunSet.isEmpty()) {
            tahunSet.add(String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)));
        }
        List<String> tahunList = new ArrayList<>(tahunSet);
        tahunList.add(0, "Semua Tahun");

        String[] bulanList = {"Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                              "Juli", "Agustus", "September", "Oktober", "November", "Desember"};

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        filterPanel.setBackground(BG_MAIN);

        JComboBox<String> cbBulan = new JComboBox<>(bulanList);
        cbBulan.setSelectedIndex(transaksiFilterBulan + 1);
        cbBulan.setPreferredSize(new Dimension(130, 32));
        styleComboBox(cbBulan);

        JComboBox<String> cbTahun = new JComboBox<>(tahunList.toArray(new String[0]));
        cbTahun.setSelectedItem(transaksiFilterTahun);
        cbTahun.setPreferredSize(new Dimension(110, 32));
        styleComboBox(cbTahun);

        RoundedButton btnClear = new RoundedButton("Hapus Filter", 8);
        btnClear.setBackground(new Color(230, 80, 80));
        btnClear.setForeground(Color.WHITE);
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        btnClear.setPreferredSize(new Dimension(110, 32));
        btnClear.setVisible(transaksiFilterBulan != -1 || !transaksiFilterTahun.equals("Semua Tahun"));

        cbBulan.addActionListener(e -> {
            transaksiFilterBulan = cbBulan.getSelectedIndex() - 1;
            refreshTransaksiPanel();
        });
        cbTahun.addActionListener(e -> {
            transaksiFilterTahun = (String) cbTahun.getSelectedItem();
            refreshTransaksiPanel();
        });
        btnClear.addActionListener(e -> {
            transaksiFilterBulan = -1;
            transaksiFilterTahun = "Semua Tahun";
            refreshTransaksiPanel();
        });

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        filterLabel.setForeground(TEXT_DARK);

        filterPanel.add(filterLabel);
        filterPanel.add(cbBulan);
        filterPanel.add(cbTahun);
        filterPanel.add(btnClear);

        JButton btnTambah = makeButton("+ Tambah Transaksi", ACCENT_BLUE);
        btnTambah.addActionListener(e -> {
            showTransaksiDialog(null);
            contentPanel.remove(1);
            contentPanel.add(buildTransaksiPanel(), "Transaksi", 1);
            contentPanel.remove(0);
            contentPanel.add(buildDashboardPanel(), "Dashboard", 0);
            contentPanel.remove(3);
            contentPanel.add(buildLaporanPanel(), "Laporan", 3);
            cardLayout.show(contentPanel, "Transaksi");
        });

        header.add(title, BorderLayout.WEST);
        header.add(filterPanel, BorderLayout.CENTER);
        header.add(btnTambah, BorderLayout.EAST);

        String[] cols = {"Tanggal", "Deskripsi", "Jenis", "Jumlah", "Aksi"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        refreshTableModel(model);

        JTable table = new JTable(model);
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_DARK);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(46);
        table.setGridColor(new Color(235, 228, 248));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(115, 72, 195, 35));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader th = table.getTableHeader();
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(0, 42));
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                l.setOpaque(true);
                l.setBackground(new Color(240, 235, 252));
                l.setForeground(new Color(115, 72, 195));
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                if (col == 3) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (col == 4) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });

        // Renderer jumlah
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(115, 72, 195, 35) : (row % 2 == 0 ? BG_CARD : new Color(248, 246, 252)));
                setHorizontalAlignment(SwingConstants.RIGHT);
                String jenis = (String) t.getValueAt(row, 2);
                setForeground("Pemasukan".equals(jenis) ? ACCENT_GREEN : ACCENT_RED);
                setFont(new Font("Segoe UI", Font.BOLD, 13));
                setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 14));
                return this;
            }
        });

        // Renderer jenis
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(115, 72, 195, 35) : (row % 2 == 0 ? BG_CARD : new Color(248, 246, 252)));
                setForeground("Pemasukan".equals(val) ? ACCENT_GREEN : ACCENT_RED);
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
                return this;
            }
        });

        DefaultTableCellRenderer def = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(115, 72, 195, 35) : (row % 2 == 0 ? BG_CARD : new Color(248, 246, 252)));
                setForeground(TEXT_DARK);
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
                return this;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(def);

        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(sel ? new Color(115, 72, 195, 35) : (row % 2 == 0 ? BG_CARD : new Color(248, 246, 252)));
                setForeground(TEXT_DARK);
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
                
                String text = val != null ? val.toString() : "";
                int spaceIdx = text.indexOf(' ');
                if (spaceIdx > 0 && spaceIdx <= 3) {
                    setText(text.substring(spaceIdx));
                    setIcon(EmojiUtils.getIcon(text.substring(0, spaceIdx), 18, t));
                    setIconTextGap(8);
                } else {
                    setText(text);
                    setIcon(null);
                }
                return this;
            }
        });

        table.getColumn("Aksi").setCellRenderer(new ActionPanelRenderer());
        table.getColumn("Aksi").setCellEditor(new ActionPanelEditor(this));

        // Lebar kolom
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);

        JScrollPane scroll = new JScrollPane(table);
        styleScrollPane(scroll);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(220, 210, 240)));
        scroll.getViewport().setBackground(BG_CARD);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ===================== KATEGORI =====================
    private JPanel buildKategoriPanel() {
        return buildKategoriPanel("Pengeluaran");
    }

    private JPanel buildKategoriPanel(String activeTab) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // ---- Header ----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_MAIN);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setBackground(BG_MAIN);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Kategori");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_DARK);

        JLabel subTitle = new JLabel("Manajemen Kategori");
        subTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        subTitle.setForeground(new Color(100, 100, 130));
        subTitle.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        titleBlock.add(title);
        titleBlock.add(subTitle);

        JButton btnTambah = makeButton("+ Tambah Kategori", ACCENT_BLUE);
        btnTambah.addActionListener(e -> {
            showKategoriDialog(null, activeTab);
            refreshKategoriPanel(activeTab);
        });

        header.add(titleBlock, BorderLayout.WEST);
        header.add(btnTambah, BorderLayout.EAST);

        // ---- Tab Filter ----
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setBackground(BG_MAIN);
        tabPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JButton tabPengeluaran = makeTabButton("Pengeluaran", activeTab.equals("Pengeluaran"));
        JButton tabPemasukan   = makeTabButton("Pemasukan",   activeTab.equals("Pemasukan"));

        tabPengeluaran.addActionListener(e -> refreshKategoriPanel("Pengeluaran"));
        tabPemasukan.addActionListener(e -> refreshKategoriPanel("Pemasukan"));

        tabPanel.add(tabPengeluaran);
        tabPanel.add(Box.createHorizontalStrut(8));
        tabPanel.add(tabPemasukan);

        // ---- Grid Kartu ----
        List<Category> list = categoryManager.getByTipe(activeTab);
        JPanel grid = new JPanel(new GridLayout(0, 4, 15, 15));
        grid.setBackground(BG_MAIN);

        for (int i = 0; i < list.size(); i++) {
            final Category cat = list.get(i);
            final int globalIdx = categoryManager.getAll().indexOf(cat);

            RoundedPanel card = new RoundedPanel(16, 5);
            card.setLayout(new BorderLayout());
            card.setBackground(BG_CARD);
            card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Ikon dari data kategori
            JLabel icon = new JLabel("", SwingConstants.CENTER);
            icon.setIcon(EmojiUtils.getIcon(cat.getIcon(), 48, icon));
            icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

            JPanel textPanel = new JPanel();
            textPanel.setBackground(BG_CARD);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            JLabel namaLabel = new JLabel(cat.getNama(), SwingConstants.CENTER);
            namaLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            namaLabel.setForeground(TEXT_DARK);
            namaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel tipeLabel = new JLabel(cat.isDefault() ? "Default" : "Custom", SwingConstants.CENTER);
            tipeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            tipeLabel.setForeground(new Color(160, 160, 180));
            tipeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            textPanel.add(namaLabel);
            textPanel.add(Box.createVerticalStrut(3));
            textPanel.add(tipeLabel);

            // Tombol Edit & Hapus di bawah kartu
            JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
            btnRow.setBackground(BG_CARD);
            btnRow.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

            RoundedButton btnEdit = new RoundedButton("Edit", 8);
            styleSmallBtn(btnEdit, ACCENT_BLUE);
            btnEdit.addActionListener(e -> {
                showKategoriDialog(globalIdx, activeTab);
                refreshKategoriPanel(activeTab);
            });

            RoundedButton btnHapus = new RoundedButton("Hapus", 8);
            styleSmallBtn(btnHapus, ACCENT_RED);
            btnHapus.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Hapus kategori \"" + cat.getNama() + "\"?",
                    "Konfirmasi", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    categoryManager.deleteCategory(globalIdx);
                    refreshKategoriPanel(activeTab);
                }
            });

            btnRow.add(btnEdit);
            btnRow.add(btnHapus);

            JPanel center = new JPanel();
            center.setBackground(BG_CARD);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.add(icon);
            center.add(textPanel);

            card.add(center, BorderLayout.CENTER);
            card.add(btnRow, BorderLayout.SOUTH);
            grid.add(card);
        }

        // Jika kosong
        if (list.isEmpty()) {
            JLabel kosong = new JLabel("Belum ada kategori " + activeTab, SwingConstants.CENTER);
            kosong.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            kosong.setForeground(TEXT_GRAY);
            grid.add(kosong);
        }

        JPanel gridWrapper = new JPanel(new BorderLayout());
        gridWrapper.setBackground(BG_MAIN);
        gridWrapper.add(grid, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(gridWrapper);
        styleScrollPane(scroll);
        scroll.getViewport().setBackground(BG_MAIN);

        JPanel topSection = new JPanel();
        topSection.setBackground(BG_MAIN);
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(header);
        topSection.add(tabPanel);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void refreshKategoriPanel(String activeTab) {
        contentPanel.remove(2);
        contentPanel.add(buildKategoriPanel(activeTab), "Kategori", 2);
        cardLayout.show(contentPanel, "Kategori");
    }

    private void refreshLaporanPanel() {
        contentPanel.remove(3);
        contentPanel.add(buildLaporanPanel(), "Laporan", 3);
        cardLayout.show(contentPanel, "Laporan");
    }

    private JButton makeTabButton(String text, boolean active) {
        RoundedButton btn = new RoundedButton(text, 10);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 22, 9, 22));
        if (active) {
            btn.setBackground(ACCENT_BLUE); // purple
            btn.setForeground(Color.WHITE);
            btn.setBorderPainted(false);
        } else {
            btn.setBackground(BG_CARD);
            btn.setForeground(new Color(115, 72, 195));
            btn.setBorderPainted(true);
        }
        return btn;
    }

    private void styleSmallBtn(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 32));
    }

    private void showKategoriDialog(Integer editGlobalIdx, String activeTab) {
        boolean isEdit = editGlobalIdx != null;
        Category cat = null;
        if (isEdit) {
            List<Category> all = categoryManager.getAll();
            if (editGlobalIdx < 0 || editGlobalIdx >= all.size()) return;
            cat = all.get(editGlobalIdx);
        }

        JDialog dialog = new JDialog(this, isEdit ? "Edit Kategori" : "Tambah Kategori", true);
        dialog.setSize(440, 540);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 10, 30));
        JLabel dlgTitle = new JLabel(isEdit ? "Edit Kategori" : "Tambah Kategori");
        dlgTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dlgTitle.setForeground(TEXT_DARK);
        
        JButton btnBatal = new JButton("Batal");
        btnBatal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBatal.setBorderPainted(false); btnBatal.setFocusPainted(false);
        btnBatal.setBackground(Color.WHITE);
        btnBatal.setForeground(new Color(120, 120, 150));
        btnBatal.addActionListener(e -> dialog.dispose());
        
        headerPanel.add(dlgTitle, BorderLayout.WEST);
        headerPanel.add(btnBatal, BorderLayout.EAST);

        // Form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        form.setBackground(Color.WHITE);

        Font fLabel = new Font("Segoe UI", Font.PLAIN, 13);
        Font fInput = new Font("Segoe UI", Font.PLAIN, 14);
        Color borderCol = new Color(210, 210, 230);

        class InputFactory {
            JTextField createTF(String text) {
                JTextField tf = new JTextField(text);
                tf.setFont(fInput);
                tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                tf.setAlignmentX(Component.LEFT_ALIGNMENT);
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderCol, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
                return tf;
            }
            JLabel createLabel(String text) {
                JLabel l = new JLabel(text);
                l.setFont(fLabel);
                l.setForeground(new Color(80, 80, 110));
                l.setAlignmentX(Component.LEFT_ALIGNMENT);
                l.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
                return l;
            }
        }
        InputFactory factory = new InputFactory();

        // 1. Tipe (Segmented Control)
        class SegmentedControl extends JPanel {
            private String selectedValue;
            private JButton btnPengeluaran = new JButton("Pengeluaran");
            private JButton btnPemasukan = new JButton("Pemasukan");
            
            public SegmentedControl(String defaultVal) {
                setLayout(new GridLayout(1, 2));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                setAlignmentX(Component.LEFT_ALIGNMENT);
                
                setupBtn(btnPengeluaran);
                setupBtn(btnPemasukan);
                
                btnPengeluaran.addActionListener(e -> select("Pengeluaran"));
                btnPemasukan.addActionListener(e -> select("Pemasukan"));
                
                add(btnPengeluaran);
                add(btnPemasukan);
                select(defaultVal);
            }
            private void setupBtn(JButton b) {
                b.setFont(new Font("Segoe UI", Font.BOLD, 13));
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.setBorder(BorderFactory.createLineBorder(borderCol));
            }
            public void select(String val) {
                selectedValue = val;
                boolean isOut = val.equals("Pengeluaran");
                btnPengeluaran.setBackground(isOut ? new Color(231, 76, 60) : new Color(248, 248, 252));
                btnPengeluaran.setForeground(isOut ? Color.WHITE : new Color(120, 120, 140));
                btnPemasukan.setBackground(!isOut ? new Color(46, 204, 113) : new Color(248, 248, 252));
                btnPemasukan.setForeground(!isOut ? Color.WHITE : new Color(120, 120, 140));
            }
            public String getSelectedValue() { return selectedValue; }
        }
        SegmentedControl cbTipe = new SegmentedControl(isEdit ? cat.getTipe() : activeTab);
        
        JTextField tfNama = factory.createTF(isEdit ? cat.getNama() : "");

        // 2. Icon Input with Preview
        JPanel iconInputRow = new JPanel(new BorderLayout(8, 0));
        iconInputRow.setBackground(Color.WHITE);
        iconInputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        iconInputRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel iconPreview = new JLabel(isEdit ? cat.getIcon() : "📌");
        iconPreview.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        iconPreview.setPreferredSize(new Dimension(38, 38));
        iconPreview.setHorizontalAlignment(SwingConstants.CENTER);
        iconPreview.setBorder(BorderFactory.createLineBorder(borderCol));
        
        JTextField tfIcon = new JTextField(isEdit ? cat.getIcon() : "📌");
        tfIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        tfIcon.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderCol, 1, true), BorderFactory.createEmptyBorder(6, 12, 6, 12)));
            
        tfIcon.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePreview(); }
            void updatePreview() {
                String v = tfIcon.getText().trim();
                iconPreview.setText(v.isEmpty() ? "📌" : v);
            }
        });
        
        iconInputRow.add(iconPreview, BorderLayout.WEST);
        iconInputRow.add(tfIcon, BorderLayout.CENTER);

        // Emoji quick-pick grid
        String[] quickEmojis = {
            "🍔","🚗","🛒","📋","🎬","💊","📚","💼","🎁","📈",
            "🍜","⚽","🎵","✈️","🏠","📱","💳","💰","❤️","🔧",
            "🐶","🌿","⭐","🛡️","🎉","📅","📖","📦","🔍","🛢️"
        };
        JPanel pickerGrid = new JPanel(new GridLayout(3, 10, 4, 4));
        pickerGrid.setBackground(new Color(248, 248, 252));
        pickerGrid.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220,220,235)), BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        pickerGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        pickerGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        for (String em : quickEmojis) {
            JButton eb = new JButton();
            eb.setIcon(EmojiUtils.getIcon(em, 24, eb));
            eb.setMargin(new Insets(0, 0, 0, 0)); // Fix truncation
            eb.setBackground(new Color(248,248,252));
            eb.setBorderPainted(false); eb.setFocusPainted(false);
            eb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            eb.addActionListener(ev -> { tfIcon.setText(em); iconPreview.setText(em); });
            pickerGrid.add(eb);
        }

        form.add(factory.createLabel("Tipe Kategori")); form.add(cbTipe);
        form.add(factory.createLabel("Nama Kategori")); form.add(tfNama);
        form.add(factory.createLabel("Icon (Emoji)")); form.add(iconInputRow);
        form.add(Box.createVerticalStrut(10));
        form.add(pickerGrid);

        // Buttons Footer
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 25, 30));
        
        JButton btnSimpan = makeButton("Simpan", ACCENT_BLUE);
        btnSimpan.addActionListener(e -> {
            String nama = tfNama.getText().trim();
            if (nama.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Nama kategori tidak boleh kosong!");
                return;
            }
            String tipe = cbTipe.getSelectedValue();
            String iconVal = tfIcon.getText().trim();
            if (iconVal.isEmpty()) iconVal = "📌";
            
            if (isEdit) categoryManager.updateCategory(editGlobalIdx, nama, tipe, iconVal);
            else categoryManager.addCategory(new Category(nama, tipe, false, iconVal));
            
            dialog.dispose();
        });
        
        btnPanel.add(btnSimpan);

        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ===================== LAPORAN =====================
    // ===================== LAPORAN =====================
    private JPanel buildLaporanPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_MAIN);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Header
        JLabel pageTitle = new JLabel("Laporan");
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        pageTitle.setForeground(TEXT_DARK);
        pageTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        pageTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subTitle = new JLabel("Laporan Keuangan");
        subTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        subTitle.setForeground(TEXT_DARK);
        subTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        subTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Filter Setup
        Set<String> tahunSet = new TreeSet<>(Collections.reverseOrder());
        for(Transaction t : manager.getTransactions()) {
            try {
                String yyyy = t.getTanggal().substring(0, 4);
                tahunSet.add(yyyy);
            } catch (Exception e){}
        }
        if(tahunSet.isEmpty()) tahunSet.add(String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)));
        if(!tahunSet.contains(laporanSelectedTahun)) laporanSelectedTahun = tahunSet.iterator().next();

        String[] bulanList = {"Semua Bulan","Januari","Februari","Maret","April","Mei","Juni",
                              "Juli","Agustus","September","Oktober","November","Desember"};
        JComboBox<String> cbBulan = new JComboBox<>(bulanList);
        cbBulan.setSelectedIndex(laporanSelectedBulan + 1);
        cbBulan.setPreferredSize(new Dimension(130, 32));
        styleComboBox(cbBulan);

        JComboBox<String> cbTahun = new JComboBox<>(tahunSet.toArray(new String[0]));
        cbTahun.setSelectedItem(laporanSelectedTahun);
        cbTahun.setPreferredSize(new Dimension(90, 32));
        styleComboBox(cbTahun);

        cbBulan.addActionListener(e -> {
            laporanSelectedBulan = cbBulan.getSelectedIndex() - 1;
            refreshLaporanPanel();
        });
        cbTahun.addActionListener(e -> {
            laporanSelectedTahun = (String) cbTahun.getSelectedItem();
            refreshLaporanPanel();
        });

        JButton btnExport = makeButton("Export Laporan (.html)", ACCENT_BLUE);
        btnExport.addActionListener(e -> exportHTML());

        String defaultYear = String.valueOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
        boolean hasFilter = (laporanSelectedBulan != -1 || !laporanSelectedTahun.equals(defaultYear));

        RoundedButton btnClear = new RoundedButton("Hapus Filter", 8);
        btnClear.setBackground(new Color(230, 80, 80));
        btnClear.setForeground(Color.WHITE);
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        btnClear.setPreferredSize(new Dimension(110, 32));
        btnClear.setVisible(hasFilter);
        btnClear.addActionListener(e -> {
            laporanSelectedBulan = -1;
            laporanSelectedTahun = defaultYear;
            refreshLaporanPanel();
        });

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setBackground(BG_MAIN);
        filterPanel.add(cbBulan);
        filterPanel.add(cbTahun);
        filterPanel.add(btnClear);
        filterPanel.add(btnExport);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        filterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Filter Data
        List<Transaction> filteredList = new ArrayList<>();
        double totalIn = 0, totalOut = 0;
        Map<String, Double> catIn = new LinkedHashMap<>();
        Map<String, Double> catOut = new LinkedHashMap<>();

        for (Transaction t : manager.getTransactions()) {
            try {
                String yyyy = t.getTanggal().substring(0, 4);
                String mm = t.getTanggal().substring(5, 7);
                int m = Integer.parseInt(mm) - 1;

                if (yyyy.equals(laporanSelectedTahun) && (laporanSelectedBulan == -1 || m == laporanSelectedBulan)) {
                    filteredList.add(t);
                    if (t.getJenis().equals("Pemasukan")) {
                        totalIn += t.getNominal();
                        catIn.merge(t.getKeterangan(), t.getNominal(), Double::sum);
                    } else {
                        totalOut += t.getNominal();
                        catOut.merge(t.getKeterangan(), t.getNominal(), Double::sum);
                    }
                }
            } catch(Exception ignored){}
        }

        // Summary Card
        RoundedPanel ringkasanCard = new RoundedPanel(16, 6);
        ringkasanCard.setBackground(BG_CARD);
        ringkasanCard.setLayout(new GridLayout(1, 3, 15, 0));
        ringkasanCard.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel p1 = buildLaporanMetric("Total Pemasukan", formatRupiah(totalIn), ACCENT_GREEN);
        JPanel p2 = buildLaporanMetric("Total Pengeluaran", formatRupiah(totalOut), ACCENT_RED);
        JPanel p3 = buildLaporanMetric("Saldo", formatRupiah(totalIn - totalOut), ACCENT_BLUE);
        ringkasanCard.add(p1); ringkasanCard.add(p2); ringkasanCard.add(p3);

        // Kategori Details Grid
        JPanel detailGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        detailGrid.setBackground(BG_MAIN);
        detailGrid.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        detailGrid.add(buildKategoriBreakdown("Pengeluaran per Kategori", catOut, ACCENT_RED));
        detailGrid.add(buildKategoriBreakdown("Pemasukan per Kategori", catIn, ACCENT_GREEN));

        // Assemble
        JPanel topSection = new JPanel();
        topSection.setBackground(BG_MAIN);
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.add(pageTitle);
        topSection.add(subTitle);
        topSection.add(filterPanel);

        JPanel content = new JPanel();
        content.setBackground(BG_MAIN);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(ringkasanCard);
        content.add(detailGrid);

        JScrollPane scroll = new JScrollPane(content);
        styleScrollPane(scroll);
        scroll.getViewport().setBackground(BG_MAIN);

        panel.add(topSection, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLaporanMetric(String title, String val, Color color) {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(new Color(120, 120, 140));
        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 18));
        v.setForeground(color);
        p.add(t);
        p.add(Box.createVerticalStrut(5));
        p.add(v);
        return p;
    }

    private JPanel buildKategoriBreakdown(String title, Map<String, Double> map, Color color) {
        RoundedPanel card = new RoundedPanel(16, 6);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        card.setHeader(57, new Color(225, 218, 242));

        JLabel lbTitle = new JLabel(title);
        lbTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbTitle.setForeground(TEXT_DARK);
        lbTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        lbTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lbTitle);

        if (map.isEmpty()) {
            JLabel kosong = new JLabel("Tidak ada data");
            kosong.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            kosong.setForeground(TEXT_GRAY);
            kosong.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(kosong);
        } else {
            // Sort by value descending
            List<Map.Entry<String, Double>> list = new ArrayList<>(map.entrySet());
            list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            for (Map.Entry<String, Double> entry : list) {
                JPanel row = new JPanel(new BorderLayout());
                row.setBackground(BG_CARD);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);

                String text = entry.getKey();
                JLabel nama = new JLabel();
                int spaceIdx = text.indexOf(' ');
                if (spaceIdx > 0 && spaceIdx <= 3) {
                    nama.setText(text.substring(spaceIdx));
                    nama.setIcon(EmojiUtils.getIcon(text.substring(0, spaceIdx), 16, nama));
                    nama.setIconTextGap(8);
                } else {
                    nama.setText("• " + text);
                }
                nama.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                nama.setForeground(TEXT_DARK);

                JLabel nilai = new JLabel(formatRupiah(entry.getValue()));
                nilai.setFont(new Font("Segoe UI", Font.BOLD, 13));
                nilai.setForeground(color);

                row.add(nama, BorderLayout.WEST);
                row.add(nilai, BorderLayout.EAST);
                card.add(row);

                JSeparator sep = new JSeparator();
                sep.setForeground(new Color(235, 235, 245));
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                sep.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(sep);
            }
        }
        
        // Wrap in North so it doesn't stretch vertically
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_MAIN);
        wrapper.add(card, BorderLayout.NORTH);
        return wrapper;
    }

    private void exportHTML() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("laporan_moneyflow.html"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("HTML File (*.html)", "html"));
        int result = fc.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".html")) {
            file = new java.io.File(file.getAbsolutePath() + ".html");
        }

        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
            bw.write(buildHtmlReport());
            int opt = JOptionPane.showConfirmDialog(this,
                "Export berhasil!\nBuka file sekarang?",
                "Export Laporan", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal export: " + e.getMessage());
        }
    }

    private String buildHtmlReport() {
        double in  = manager.getTotalPemasukan();
        double out = manager.getTotalPengeluaran();
        double bal = manager.getSaldo();
        String tgl = new java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", new java.util.Locale("id","ID")).format(new java.util.Date());
        List<Transaction> txList = manager.getTransactions();
        int total = txList.size();

        StringBuilder s = new StringBuilder();

        // ===== HEAD =====
        s.append("<!DOCTYPE html>\n<html lang=\"id\">\n<head>\n");
        s.append("<meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n");
        s.append("<title>Laporan Keuangan - MoneyFlow</title>\n");
        s.append("<style>\n");
        s.append("@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');\n");
        s.append("*{margin:0;padding:0;box-sizing:border-box}\n");
        s.append("body{font-family:'Inter','Segoe UI',Arial,sans-serif;background:#f0f2f8;color:#1e1e32;padding:40px 20px}\n");
        s.append(".wrap{max-width:980px;margin:0 auto}\n");
        // Header
        s.append(".hdr{background:linear-gradient(135deg,#12121e,#1a1a2a);border-radius:16px;padding:34px 40px;margin-bottom:26px;display:flex;justify-content:space-between;align-items:center}\n");
        s.append(".brand{color:#fff;font-size:26px;font-weight:700}.brand em{color:#2ecc71;font-style:normal}\n");
        s.append(".meta{text-align:right}.meta h2{color:#fff;font-size:17px;font-weight:600;margin-bottom:3px}.meta p{color:#9696b4;font-size:12px}\n");
        // Summary
        s.append(".cards{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:26px}\n");
        s.append(".card{background:#fff;border-radius:12px;padding:20px 22px;border:1px solid #dcdceb;display:flex;align-items:center;gap:14px}\n");
        s.append(".bar{width:6px;height:52px;border-radius:3px;flex-shrink:0}\n");
        s.append(".clbl{font-size:11px;color:#9090a8;font-weight:600;text-transform:uppercase;letter-spacing:.5px;margin-bottom:5px}\n");
        s.append(".cval{font-size:19px;font-weight:700}\n");
        s.append(".g{background:#2ecc71}.r{background:#e74c3c}.b{background:#3498db}\n");
        s.append(".vg{color:#2ecc71}.vr{color:#e74c3c}.vb{color:#3498db}\n");
        // Section
        s.append(".sec{background:#fff;border-radius:14px;border:1px solid #dcdceb;overflow:hidden;margin-bottom:20px}\n");
        // Toolbar
        s.append(".tb{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:12px;padding:18px 24px;border-bottom:1px solid #ededf5}\n");
        s.append(".tb-left h3{font-size:15px;font-weight:700}\n");
        s.append(".rc{font-size:12px;color:#aaa;margin-top:3px}\n");
        s.append(".tb-right{display:flex;gap:10px;align-items:center;flex-wrap:wrap}\n");
        // Search
        s.append(".sw{position:relative}\n");
        s.append(".sw svg{position:absolute;left:10px;top:50%;transform:translateY(-50%);pointer-events:none}\n");
        s.append("#si{padding:8px 12px 8px 33px;border:1.5px solid #e0e0ee;border-radius:8px;font-size:13px;font-family:inherit;width:210px;outline:none;transition:border-color .2s}\n");
        s.append("#si:focus{border-color:#3498db}\n");
        // Pills
        s.append(".pills{display:flex;gap:6px}\n");
        s.append(".pill{padding:7px 15px;border-radius:20px;font-size:12px;font-weight:600;cursor:pointer;border:1.5px solid #e0e0ee;background:#fff;color:#6060a0;transition:all .15s;user-select:none}\n");
        s.append(".pill:hover{border-color:#aaa}\n");
        s.append(".pa{background:#1e1e32!important;color:#fff!important;border-color:#1e1e32!important}\n");
        s.append(".pi{background:#e8faf0!important;color:#1a9b52!important;border-color:#2ecc71!important}\n");
        s.append(".po{background:#fdecea!important;color:#c0392b!important;border-color:#e74c3c!important}\n");
        // Table
        s.append("table{width:100%;border-collapse:collapse}\n");
        s.append("thead th{background:#f8f9fc;padding:12px 18px;font-size:11px;font-weight:700;color:#7878a0;text-transform:uppercase;letter-spacing:.6px;text-align:left;border-bottom:2px solid #e8e8f0;white-space:nowrap}\n");
        s.append("thead th:last-child{text-align:right}\n");
        s.append("tbody tr{border-bottom:1px solid #f0f0f8;transition:background .1s}\n");
        s.append("tbody tr:last-child{border-bottom:none}\n");
        s.append("tbody tr:nth-child(even){background:#fafafd}\n");
        s.append("tbody tr:hover{background:#eef4ff}\n");
        s.append("tbody td{padding:13px 18px;font-size:13px;vertical-align:middle}\n");
        s.append(".nc{color:#c8c8d8;font-size:12px}.dc{color:#8888a8;font-size:12px;white-space:nowrap}.kc{font-weight:500}\n");
        s.append(".badge{display:inline-block;padding:3px 11px;border-radius:20px;font-size:11px;font-weight:600}\n");
        s.append(".bi{background:#e8faf0;color:#1a9b52}.bo{background:#fdecea;color:#c0392b}\n");
        s.append(".nom{text-align:right;font-weight:700;font-size:13.5px;white-space:nowrap}\n");
        s.append(".ni{color:#2ecc71}.no2{color:#e74c3c}\n");
        s.append("mark{background:#fff176;border-radius:2px;padding:0 1px;font-weight:600}\n");
        // No result
        s.append(".empty{text-align:center;padding:48px;color:#bbb;font-size:14px;display:none}\n");
        s.append(".empty svg{display:block;margin:0 auto 14px;opacity:.3}\n");
        // Footer
        s.append(".ftr{text-align:center;margin-top:30px;color:#bbb;font-size:11px;padding-bottom:16px}\n");
        s.append(".ftr strong{color:#7878a0}\n");
        s.append("</style>\n</head>\n<body>\n<div class=\"wrap\">\n");

        // ===== HEADER BANNER =====
        s.append("<div class=\"hdr\"><div class=\"brand\">Money<em>Flow</em></div>");
        s.append("<div class=\"meta\"><h2>Laporan Keuangan</h2><p>Dicetak: ").append(tgl).append("</p></div></div>\n");

        // ===== SUMMARY CARDS =====
        s.append("<div class=\"cards\">\n");
        s.append("<div class=\"card\"><div class=\"bar g\"></div><div><div class=\"clbl\">Total Pemasukan</div><div class=\"cval vg\">").append(formatRupiah(in)).append("</div></div></div>\n");
        s.append("<div class=\"card\"><div class=\"bar r\"></div><div><div class=\"clbl\">Total Pengeluaran</div><div class=\"cval vr\">").append(formatRupiah(out)).append("</div></div></div>\n");
        s.append("<div class=\"card\"><div class=\"bar b\"></div><div><div class=\"clbl\">Saldo</div><div class=\"cval vb\">").append(formatRupiah(bal)).append("</div></div></div>\n");
        s.append("</div>\n");

        // ===== TABLE SECTION =====
        s.append("<div class=\"sec\">\n");

        // Toolbar
        s.append("<div class=\"tb\">\n");
        s.append("  <div class=\"tb-left\"><h3>Riwayat Transaksi</h3>");
        s.append("  <div class=\"rc\" id=\"rc\">Menampilkan <b>").append(total).append("</b> dari <b>").append(total).append("</b> transaksi</div></div>\n");
        s.append("  <div class=\"tb-right\">\n");
        // Search
        s.append("    <div class=\"sw\">\n");
        s.append("      <svg width=\"14\" height=\"14\" fill=\"none\" stroke=\"#bbb\" stroke-width=\"2\" viewBox=\"0 0 24 24\"><circle cx=\"11\" cy=\"11\" r=\"8\"/><line x1=\"21\" y1=\"21\" x2=\"16.65\" y2=\"16.65\"/></svg>\n");
        s.append("      <input id=\"si\" type=\"text\" placeholder=\"Cari transaksi...\">\n    </div>\n");
        // Filter pills
        s.append("    <div class=\"pills\">\n");
        s.append("      <div class=\"pill pa\" onclick=\"sf(this,'all')\">Semua</div>\n");
        s.append("      <div class=\"pill\" onclick=\"sf(this,'Pemasukan')\">&#8593; Pemasukan</div>\n");
        s.append("      <div class=\"pill\" onclick=\"sf(this,'Pengeluaran')\">&#8595; Pengeluaran</div>\n");
        s.append("    </div>\n");
        s.append("  </div>\n</div>\n"); // end toolbar

        if (txList.isEmpty()) {
            s.append("<div class=\"empty\" style=\"display:block\">Belum ada transaksi yang tercatat.</div>\n");
        } else {
            s.append("<table><thead><tr><th>No</th><th>Tanggal</th><th>Keterangan</th><th>Jenis</th><th>Nominal</th></tr></thead>\n<tbody id=\"tb\">\n");
            int no = 1;
            for (Transaction t : txList) {
                boolean isMasuk = t.getJenis().equals("Pemasukan");
                s.append("<tr data-j=\"").append(t.getJenis()).append("\">");
                s.append("<td class=\"nc\">").append(no++).append("</td>");
                s.append("<td class=\"dc\">").append(t.getTanggal()).append("</td>");
                String kesc = escapeHtml(t.getKeterangan());
                s.append("<td class=\"kc\" data-raw=\"").append(escapeHtml(t.getKeterangan().toLowerCase())).append("\">").append(kesc).append("</td>");
                s.append("<td><span class=\"badge ").append(isMasuk ? "bi" : "bo").append("\">").append(t.getJenis()).append("</span></td>");
                s.append("<td class=\"nom ").append(isMasuk ? "ni" : "no2").append("\">").append(isMasuk ? "+" : "-").append(formatRupiah(t.getNominal())).append("</td>");
                s.append("</tr>\n");
            }
            s.append("</tbody></table>\n");
            s.append("<div class=\"empty\" id=\"ne\">\n");
            s.append("  <svg width=\"38\" height=\"38\" fill=\"none\" stroke=\"#bbb\" stroke-width=\"1.5\" viewBox=\"0 0 24 24\"><circle cx=\"11\" cy=\"11\" r=\"8\"/><line x1=\"21\" y1=\"21\" x2=\"16.65\" y2=\"16.65\"/></svg>\n");
            s.append("  Tidak ada transaksi yang cocok dengan pencarian.\n</div>\n");
        }
        s.append("</div>\n"); // end sec

        // ===== FOOTER =====
        s.append("<div class=\"ftr\">Dibuat oleh <strong>MoneyFlow</strong> &mdash; Personal Finance Tracker &nbsp;|&nbsp; ").append(tgl).append("</div>\n");

        // ===== JAVASCRIPT =====
        s.append("<script>\n");
        s.append("var cf='all',cs='',tot=").append(total).append(";\n");
        s.append("function sf(el,v){cf=v;");
        s.append("document.querySelectorAll('.pill').forEach(function(p){p.className='pill';});");
        s.append("el.classList.add(v==='all'?'pa':v==='Pemasukan'?'pi':'po');af();}\n");
        s.append("document.getElementById('si').addEventListener('input',function(){cs=this.value.trim().toLowerCase();af();});\n");
        s.append("function hl(t,q){if(!q)return t;");
        s.append("return t.replace(new RegExp('('+q.replace(/[.*+?^${}()|[\\]\\\\]/g,'\\\\$&')+')','gi'),'<mark>$1</mark>');}\n");
        s.append("function af(){\n");
        s.append("  var rows=document.querySelectorAll('#tb tr'),vis=0;\n");
        s.append("  rows.forEach(function(r){\n");
        s.append("    var j=r.getAttribute('data-j'),kc=r.querySelector('.kc'),raw=kc?kc.getAttribute('data-raw'):'';\n");
        s.append("    var mf=cf==='all'||j===cf, ms=!cs||raw.indexOf(cs)!==-1;\n");
        s.append("    if(mf&&ms){r.style.display='';if(kc)kc.innerHTML=hl(kc.getAttribute('data-raw').replace(/(^|\\s)\\S/g,function(c){return c.toUpperCase();}),cs);vis++;}\n");
        s.append("    else r.style.display='none';\n");
        s.append("  });\n");
        s.append("  var rc=document.getElementById('rc');\n");
        s.append("  if(rc)rc.innerHTML='Menampilkan <b>'+vis+'</b> dari <b>'+tot+'</b> transaksi';\n");
        s.append("  var ne=document.getElementById('ne');\n");
        s.append("  if(ne)ne.style.display=vis===0?'block':'none';\n");
        s.append("}\n");
        s.append("</script>\n");

        s.append("</div>\n</body>\n</html>");
        return s.toString();
    }

    // ===================== DIALOG TRANSAKSI (ADD/EDIT) =====================
    private void showTransaksiDialog(Integer editIndex) {
        boolean isEdit = editIndex != null;
        Transaction existing = isEdit ? manager.getTransactions().get(editIndex) : null;

        JDialog dialog = new JDialog(this, isEdit ? "Edit Transaksi" : "Tambah Transaksi", true);
        dialog.setSize(440, 520);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(Color.WHITE);
        dialog.setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 10, 30));
        JLabel dlgTitle = new JLabel(isEdit ? "Edit Transaksi" : "Tambah Transaksi");
        dlgTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dlgTitle.setForeground(TEXT_DARK);
        
        JButton btnBatalHead = new JButton("Batal");
        btnBatalHead.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnBatalHead.setBorderPainted(false); btnBatalHead.setFocusPainted(false);
        btnBatalHead.setBackground(Color.WHITE);
        btnBatalHead.setForeground(new Color(120, 120, 150));
        btnBatalHead.addActionListener(e -> dialog.dispose());
        
        headerPanel.add(dlgTitle, BorderLayout.WEST);
        headerPanel.add(btnBatalHead, BorderLayout.EAST);

        // Form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        form.setBackground(Color.WHITE);

        Font fLabel = new Font("Segoe UI", Font.PLAIN, 13);
        Font fInput = new Font("Segoe UI", Font.PLAIN, 14);
        Color borderCol = new Color(210, 210, 230);

        // Helper to create modern inputs
        class InputFactory {
            JTextField createTF(String text) {
                JTextField tf = new JTextField(text);
                tf.setFont(fInput);
                tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                tf.setAlignmentX(Component.LEFT_ALIGNMENT);
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderCol, 1, true),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));
                return tf;
            }
            JLabel createLabel(String text) {
                JLabel l = new JLabel(text);
                l.setFont(fLabel);
                l.setForeground(new Color(80, 80, 110));
                l.setAlignmentX(Component.LEFT_ALIGNMENT);
                l.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
                return l;
            }
        }
        InputFactory factory = new InputFactory();

        // 1. Tipe Transaksi (Segmented Control)
        class SegmentedControl extends JPanel {
            private String selectedValue;
            private JButton btnPengeluaran = new JButton("Pengeluaran");
            private JButton btnPemasukan = new JButton("Pemasukan");
            private java.util.List<Runnable> listeners = new ArrayList<>();
            
            public SegmentedControl(String defaultVal) {
                setLayout(new GridLayout(1, 2));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                setAlignmentX(Component.LEFT_ALIGNMENT);
                
                setupBtn(btnPengeluaran);
                setupBtn(btnPemasukan);
                
                btnPengeluaran.addActionListener(e -> select("Pengeluaran"));
                btnPemasukan.addActionListener(e -> select("Pemasukan"));
                
                add(btnPengeluaran);
                add(btnPemasukan);
                select(defaultVal);
            }
            public void addChangeListener(Runnable r) { listeners.add(r); }
            private void setupBtn(JButton b) {
                b.setFont(new Font("Segoe UI", Font.BOLD, 13));
                b.setFocusPainted(false);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.setBorder(BorderFactory.createLineBorder(borderCol));
            }
            public void select(String val) {
                selectedValue = val;
                boolean isOut = val.equals("Pengeluaran");
                btnPengeluaran.setBackground(isOut ? new Color(231, 76, 60) : new Color(248, 248, 252));
                btnPengeluaran.setForeground(isOut ? Color.WHITE : new Color(120, 120, 140));
                btnPemasukan.setBackground(!isOut ? new Color(46, 204, 113) : new Color(248, 248, 252));
                btnPemasukan.setForeground(!isOut ? Color.WHITE : new Color(120, 120, 140));
                for(Runnable r : listeners) r.run();
            }
            public String getSelectedValue() { return selectedValue; }
        }
        SegmentedControl cbTipe = new SegmentedControl(isEdit ? existing.getJenis() : "Pengeluaran");
        
        // 2. Kategori dengan Icon Renderer
        JComboBox<Category> cbKategori = new JComboBox<>();
        cbKategori.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cbKategori.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbKategori.setRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Category) {
                    Category c = (Category) value;
                    setText(c.getNama());
                    setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    setIcon(EmojiUtils.getIcon(c.getIcon(), 20, list));
                    setIconTextGap(8);
                }
                return this;
            }
        });
        styleComboBox(cbKategori);

        Runnable updateCategoryCombo = () -> {
            cbKategori.removeAllItems();
            String tipe = cbTipe.getSelectedValue();
            for (Category c : categoryManager.getAll()) {
                if(c.getTipe().equals(tipe)) {
                    cbKategori.addItem(c);
                }
            }
            if (cbKategori.getItemCount() == 0) {
                cbKategori.addItem(new Category("Lainnya", tipe, false, "📌"));
            }
        };
        cbTipe.addChangeListener(updateCategoryCombo);
        updateCategoryCombo.run();
        
        // Pre-select category if editing based on keterangan prefix
        String existingKet = isEdit ? existing.getKeterangan() : "";
        if (isEdit) {
            for (int i=0; i<cbKategori.getItemCount(); i++) {
                Category c = cbKategori.getItemAt(i);
                if (existingKet.startsWith(c.getIcon() + " " + c.getNama())) {
                    cbKategori.setSelectedItem(c);
                    existingKet = existingKet.replace(c.getIcon() + " " + c.getNama() + " - ", "");
                    break;
                }
            }
        }

        JTextField tfJumlah = factory.createTF(isEdit ? String.valueOf((long)existing.getNominal()) : "");
        JTextField tfKet = factory.createTF(existingKet);
        
        // 3. Tanggal dengan DatePicker
        class DatePickerField extends JPanel {
            private JTextField tf;
            private java.util.Calendar currentCal = java.util.Calendar.getInstance();
            private JLabel lblBulan;
            private JPanel grid;
            private JDialog d;
            private int viewState = 0; // 0=Days, 1=Months, 2=Years
            private int displayYear;

            public DatePickerField(String defaultDate) {
                setLayout(new BorderLayout());
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                setAlignmentX(Component.LEFT_ALIGNMENT);
                
                tf = factory.createTF(defaultDate);
                JButton btn = new JButton("📅");
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                btn.setBackground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(borderCol));
                btn.setFocusPainted(false);
                btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btn.addActionListener(e -> showCalendar());
                
                add(tf, BorderLayout.CENTER);
                add(btn, BorderLayout.EAST);
            }
            public String getText() { return tf.getText(); }
            private void showCalendar() {
                Window ancestor = SwingUtilities.getWindowAncestor(this);
                if (ancestor instanceof Dialog) d = new JDialog((Dialog) ancestor, "Pilih Tanggal", true);
                else d = new JDialog((Frame) ancestor, "Pilih Tanggal", true);
                
                d.setSize(300, 320);
                d.setLocationRelativeTo(this);
                d.setLayout(new BorderLayout());
                d.getContentPane().setBackground(Color.WHITE);
                
                JPanel header = new JPanel(new BorderLayout());
                header.setBackground(Color.WHITE);
                header.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
                
                RoundedButton btnPrev = new RoundedButton("<", 6);
                RoundedButton btnNext = new RoundedButton(">", 6);
                styleNavBtn(btnPrev); styleNavBtn(btnNext);
                btnPrev.addActionListener(e -> { 
                    if (viewState == 0) currentCal.add(java.util.Calendar.MONTH, -1); 
                    else if (viewState == 1) currentCal.add(java.util.Calendar.YEAR, -1);
                    else if (viewState == 2) displayYear -= 12;
                    updateGrid(); 
                });
                btnNext.addActionListener(e -> { 
                    if (viewState == 0) currentCal.add(java.util.Calendar.MONTH, 1); 
                    else if (viewState == 1) currentCal.add(java.util.Calendar.YEAR, 1);
                    else if (viewState == 2) displayYear += 12;
                    updateGrid(); 
                });
                
                lblBulan = new JLabel("", SwingConstants.CENTER);
                lblBulan.setFont(new Font("Segoe UI", Font.BOLD, 14));
                lblBulan.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                lblBulan.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (viewState == 0) {
                            viewState = 1; updateGrid();
                        } else if (viewState == 1) {
                            displayYear = currentCal.get(java.util.Calendar.YEAR);
                            viewState = 2; updateGrid();
                        }
                    }
                });
                
                header.add(btnPrev, BorderLayout.WEST);
                header.add(lblBulan, BorderLayout.CENTER);
                header.add(btnNext, BorderLayout.EAST);
                
                grid = new JPanel(new GridLayout(0, 7, 2, 2));
                grid.setBackground(Color.WHITE);
                grid.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
                
                d.add(header, BorderLayout.NORTH);
                d.add(grid, BorderLayout.CENTER);
                
                viewState = 0;
                updateGrid();
                d.setVisible(true);
            }
            
            private void styleNavBtn(JButton b) {
                b.setFont(new Font("Segoe UI", Font.BOLD, 12));
                b.setBackground(new Color(240, 240, 245));
                b.setBorderPainted(false); b.setFocusPainted(false);
            }
            
            private JButton createGridBtn(String txt) {
                RoundedButton b = new RoundedButton(txt, 6);
                b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                b.setMargin(new Insets(2, 2, 2, 2));
                b.setBackground(new Color(248, 248, 252));
                b.setBorderPainted(false); b.setFocusPainted(false);
                return b;
            }
            
            private void updateGrid() {
                grid.removeAll();
                if (viewState == 0) {
                    grid.setLayout(new GridLayout(0, 7, 2, 2));
                    String[] days = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
                    for(String day : days) {
                        JLabel l = new JLabel(day, SwingConstants.CENTER);
                        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                        grid.add(l);
                    }
                    
                    java.util.Calendar temp = (java.util.Calendar) currentCal.clone();
                    temp.set(java.util.Calendar.DAY_OF_MONTH, 1);
                    int startDay = temp.get(java.util.Calendar.DAY_OF_WEEK);
                    int maxDays = currentCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH);
                    
                    for(int i=1; i<startDay; i++) {
                        grid.add(new JLabel(""));
                    }
                    
                    String yyyyMM = new java.text.SimpleDateFormat("yyyy-MM-").format(currentCal.getTime());
                    lblBulan.setText(new java.text.SimpleDateFormat("MMMM yyyy").format(currentCal.getTime()));
                    
                    for(int i=1; i<=maxDays; i++) {
                        JButton b = createGridBtn(String.valueOf(i));
                        int day = i;
                        b.addActionListener(ev -> {
                            tf.setText(yyyyMM + String.format("%02d", day));
                            d.dispose();
                        });
                        grid.add(b);
                    }
                } else if (viewState == 1) {
                    grid.setLayout(new GridLayout(4, 3, 4, 4));
                    lblBulan.setText(String.valueOf(currentCal.get(java.util.Calendar.YEAR)));
                    String[] months = {"Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des"};
                    for(int i=0; i<12; i++) {
                        JButton b = createGridBtn(months[i]);
                        int m = i;
                        b.addActionListener(ev -> {
                            currentCal.set(java.util.Calendar.MONTH, m);
                            viewState = 0;
                            updateGrid();
                        });
                        grid.add(b);
                    }
                } else if (viewState == 2) {
                    grid.setLayout(new GridLayout(4, 3, 4, 4));
                    int startYear = displayYear - 5;
                    lblBulan.setText(startYear + " - " + (startYear + 11));
                    for(int i=0; i<12; i++) {
                        int y = startYear + i;
                        JButton b = createGridBtn(String.valueOf(y));
                        b.addActionListener(ev -> {
                            currentCal.set(java.util.Calendar.YEAR, y);
                            viewState = 1;
                            updateGrid();
                        });
                        grid.add(b);
                    }
                }
                grid.revalidate();
                grid.repaint();
            }
        }
        DatePickerField tfTanggal = new DatePickerField(isEdit ? existing.getTanggal() : new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));

        form.add(factory.createLabel("Tipe Transaksi")); form.add(cbTipe);
        form.add(factory.createLabel("Nominal (Rp)")); form.add(tfJumlah);
        form.add(factory.createLabel("Kategori")); form.add(cbKategori);
        form.add(factory.createLabel("Deskripsi Singkat")); form.add(tfKet);
        form.add(factory.createLabel("Tanggal")); form.add(tfTanggal);

        // Buttons Footer
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 25, 30));
        
        JButton btnSimpan = makeButton(isEdit ? "Simpan Perubahan" : "Tambah Transaksi", ACCENT_BLUE);
        btnSimpan.addActionListener(e -> {
            try {
                String ket = tfKet.getText().trim();
                String tanggal = tfTanggal.getText().trim();
                String jenis = cbTipe.getSelectedValue();
                double nominal = Double.parseDouble(tfJumlah.getText().trim());
                if (ket.isEmpty() || tanggal.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Semua field wajib diisi!");
                    return;
                }
                
                Category selectedCat = (Category) cbKategori.getSelectedItem();
                String finalKet = selectedCat.getIcon() + " " + selectedCat.getNama() + " - " + ket;
                
                Transaction t = new Transaction(tanggal, finalKet, jenis, nominal);
                if (isEdit) {
                    manager.updateTransaction(editIndex, t);
                } else {
                    manager.addTransaction(t);
                }
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Nominal harus berupa angka yang valid!");
            }
        });
        
        btnPanel.add(btnSimpan);

        dialog.add(headerPanel, BorderLayout.NORTH);
        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ===================== HELPER =====================
    private void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(null);
        if (scroll.getVerticalScrollBar() != null) {
            scroll.getVerticalScrollBar().setUnitIncrement(24);
            scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
            scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        }
        if (scroll.getHorizontalScrollBar() != null) {
            scroll.getHorizontalScrollBar().setUnitIncrement(24);
            scroll.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
            scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
        }
    }

    public void refreshTransaksiPanel() {
        contentPanel.remove(1);
        contentPanel.add(buildTransaksiPanel(), "Transaksi", 1);
        cardLayout.show(contentPanel, "Transaksi");
    }

    public void refreshDashboardPanel() {
        contentPanel.remove(0);
        contentPanel.add(buildDashboardPanel(), "Dashboard", 0);
        cardLayout.show(contentPanel, "Dashboard");
    }

    private void refreshTableModel(DefaultTableModel model) {
        model.setRowCount(0);
        currentTableTransactions = getFilteredTransactions(transaksiFilterBulan, transaksiFilterTahun);
        for (Transaction t : currentTableTransactions) {
            boolean isPemasukan = t.getJenis().equals("Pemasukan");
            String nominal = (isPemasukan ? "+" : "-") + formatRupiah(t.getNominal());
            model.addRow(new Object[]{t.getTanggal(), t.getKeterangan(), t.getJenis(), nominal, "Hapus"});
        }
    }

    private List<Transaction> getFilteredTransactions(int bulan, String tahun) {
        List<Transaction> list = new ArrayList<>();
        for (Transaction t : manager.getTransactions()) {
            try {
                String yyyy = t.getTanggal().substring(0, 4);
                int mm = Integer.parseInt(t.getTanggal().substring(5, 7)) - 1; // 0-indexed
                
                boolean matchBulan = (bulan == -1 || mm == bulan);
                boolean matchTahun = (tahun.equals("Semua Tahun") || yyyy.equals(tahun));
                
                if (matchBulan && matchTahun) {
                    list.add(t);
                }
            } catch (Exception e) {
                if (bulan == -1 && tahun.equals("Semua Tahun")) {
                    list.add(t);
                }
            }
        }
        return list;
    }

    private int getGlobalIndex(Transaction target) {
        if (target == null) return -1;
        List<Transaction> global = manager.getTransactions();
        for (int i = 0; i < global.size(); i++) {
            Transaction g = global.get(i);
            if (g.getTanggal().equals(target.getTanggal()) &&
                g.getKeterangan().equals(target.getKeterangan()) &&
                g.getJenis().equals(target.getJenis()) &&
                Math.abs(g.getNominal() - target.getNominal()) < 0.0001) {
                return i;
            }
        }
        return -1;
    }

    private JButton makeButton(String text, Color bg) {
        RoundedButton btn = new RoundedButton(text, 12, true);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        return btn;
    }

    private String formatRupiah(double amount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.of("id", "ID"));
        return "Rp " + nf.format(amount);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ===================== ACTION PANEL (EDIT & HAPUS) =====================
    class ActionPanelRenderer extends JPanel implements TableCellRenderer {
        private RoundedButton btnEdit = new RoundedButton("Edit", 8);
        private RoundedButton btnHapus = new RoundedButton("Hapus", 8);

        public ActionPanelRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 4));
            setOpaque(true);
            styleActionBtn(btnEdit, ACCENT_BLUE);
            styleActionBtn(btnHapus, ACCENT_RED);
            add(btnEdit);
            add(btnHapus);
        }

        private void styleActionBtn(JButton b, Color c) {
            b.setBackground(c);
            b.setForeground(Color.WHITE);
            b.setFont(new Font("Segoe UI", Font.BOLD, 11));
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }

        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            setBackground(sel ? new Color(115, 72, 195, 35) : (row % 2 == 0 ? BG_CARD : new Color(248, 246, 252)));
            return this;
        }
    }

    class ActionPanelEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JButton btnEdit;
        private JButton btnHapus;
        private int currentRow;

        public ActionPanelEditor(MainFrame frame) {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 4));
            panel.setOpaque(true);

            btnEdit = new RoundedButton("Edit", 8);
            btnHapus = new RoundedButton("Hapus", 8);
            styleActionBtn(btnEdit, ACCENT_BLUE);
            styleActionBtn(btnHapus, ACCENT_RED);

            btnEdit.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < currentTableTransactions.size()) {
                    Transaction target = currentTableTransactions.get(currentRow);
                    int globalIndex = getGlobalIndex(target);
                    if (globalIndex != -1) {
                        showTransaksiDialog(globalIndex);
                        frame.refreshTransaksiPanel();
                        frame.refreshDashboardPanel();
                    }
                }
            });

            btnHapus.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < currentTableTransactions.size()) {
                    Transaction target = currentTableTransactions.get(currentRow);
                    int globalIndex = getGlobalIndex(target);
                    if (globalIndex != -1) {
                        int confirm = JOptionPane.showConfirmDialog(frame, "Yakin hapus transaksi ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            manager.deleteTransaction(globalIndex);
                            frame.refreshTransaksiPanel();
                            frame.refreshDashboardPanel();
                        }
                    }
                }
            });

            panel.add(btnEdit);
            panel.add(btnHapus);
        }

        private void styleActionBtn(JButton b, Color c) {
            b.setBackground(c);
            b.setForeground(Color.WHITE);
            b.setFont(new Font("Segoe UI", Font.BOLD, 11));
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        }

        public Component getTableCellEditorComponent(JTable t, Object val, boolean sel, int row, int col) {
            currentRow = row;
            panel.setBackground(t.getSelectionBackground());
            return panel;
        }
        public Object getCellEditorValue() { return ""; }
    }

    private JPanel buildAboutPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_MAIN);

        // Content panel inside scroll pane
        JPanel content = new JPanel();
        content.setBackground(BG_MAIN);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // 1. Header Card (App Brand)
        RoundedPanel headerCard = new RoundedPanel(16, 6);
        headerCard.setBackground(BG_CARD);
        headerCard.setLayout(new BorderLayout(15, 0));
        headerCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        headerCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandIcon = new JLabel("✨");
        brandIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        brandIcon.setForeground(ACCENT_BLUE);

        JPanel brandTextPanel = new JPanel();
        brandTextPanel.setOpaque(false);
        brandTextPanel.setLayout(new BoxLayout(brandTextPanel, BoxLayout.Y_AXIS));

        JLabel appTitle = new JLabel("MoneyFlow Pro");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        appTitle.setForeground(TEXT_DARK);

        JLabel appTagline = new JLabel("A simple, premium personal finance tracker built to manage incomes, expenses, and statistics.");
        appTagline.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        appTagline.setForeground(new Color(110, 110, 130));

        brandTextPanel.add(appTitle);
        brandTextPanel.add(Box.createVerticalStrut(4));
        brandTextPanel.add(appTagline);

        headerCard.add(brandIcon, BorderLayout.WEST);
        headerCard.add(brandTextPanel, BorderLayout.CENTER);

        content.add(headerCard);
        content.add(Box.createVerticalStrut(25));

        // 2. Team Section Label
        JLabel teamLabel = new JLabel("Anggota Tim / Developers");
        teamLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        teamLabel.setForeground(TEXT_DARK);
        teamLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(teamLabel);
        content.add(Box.createVerticalStrut(12));

        // Team Grid Card Container (2x2 grid, gap 15)
        JPanel teamGrid = new JPanel(new GridLayout(2, 2, 20, 15));
        teamGrid.setOpaque(false);
        teamGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        teamGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create 4 developer cards (2 guys, 2 girls)
        teamGrid.add(createMemberCard("👩‍💻", "Anisa Aulia", "NIM: 3.34.25.1.04"));
        teamGrid.add(createMemberCard("👨‍💻", "Jizdan Yuflikh R", "NIM: 3.34.25.1.12"));
        teamGrid.add(createMemberCard("👩‍💻", "Kafka Nafisa", "NIM: 3.34.25.1.13"));
        teamGrid.add(createMemberCard("👨‍💻", "Nazriel Farras Khairiya P S", "NIM: 3.34.25.1.20"));

        content.add(teamGrid);
        content.add(Box.createVerticalStrut(25));

        // 3. Project Context Label
        JLabel projectLabel = new JLabel("Informasi Proyek");
        projectLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        projectLabel.setForeground(TEXT_DARK);
        projectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(projectLabel);
        content.add(Box.createVerticalStrut(12));

        // Project Info Card
        RoundedPanel projectCard = new RoundedPanel(16, 6);
        projectCard.setBackground(BG_CARD);
        projectCard.setLayout(new BorderLayout());
        projectCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        projectCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        projectCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel projectDetails = new JPanel(new GridLayout(3, 2, 10, 10));
        projectDetails.setOpaque(false);

        addInfoRow(projectDetails, "📚", "Mata Kuliah:", "Pemrograman Berorientasi Objek (PBO)");
        addInfoRow(projectDetails, "👨‍🏫", "Dosen Pengampu:", "Bapak Arif Fitra Setyawan S,Pd., M.Kom.");
        addInfoRow(projectDetails, "🛠️", "Tech Stack:", "Java Swing (GUI), File I/O (CSV Storage), OOP Design");

        projectCard.add(projectDetails, BorderLayout.CENTER);
        content.add(projectCard);

        content.add(Box.createVerticalGlue());

        // 4. Footer Wrapper to cleanly center text without BoxLayout issues
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setOpaque(false);
        footerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        footerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel footerLabel = new JLabel("© 2026 MoneyFlow Team. All Rights Reserved.");
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footerLabel.setForeground(new Color(140, 140, 160));
        
        footerPanel.add(footerLabel);
        content.add(footerPanel);

        // Put everything inside a JScrollPane
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBackground(BG_MAIN);
        scroll.getViewport().setBackground(BG_MAIN);
        styleScrollPane(scroll);

        mainPanel.add(scroll, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createMemberCard(String avatar, String name, String nim) {
        RoundedPanel memberCard = new RoundedPanel(14, 4);
        memberCard.setBackground(BG_CARD);
        memberCard.setLayout(new BorderLayout(12, 0));
        memberCard.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel avatarLabel = new JLabel(avatar);
        avatarLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(Box.createVerticalGlue());

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(TEXT_DARK);

        JLabel nimLabel = new JLabel(nim);
        nimLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nimLabel.setForeground(new Color(110, 110, 130));

        details.add(nameLabel);
        details.add(Box.createVerticalStrut(4));
        details.add(nimLabel);
        details.add(Box.createVerticalGlue());

        memberCard.add(avatarLabel, BorderLayout.WEST);
        memberCard.add(details, BorderLayout.CENTER);

        return memberCard;
    }

    private void addInfoRow(JPanel container, String icon, String labelText, String valueText) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        rowPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(" " + icon + "  ");
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));

        JLabel textLabel = new JLabel(labelText);
        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        textLabel.setForeground(TEXT_DARK);

        rowPanel.add(iconLabel);
        rowPanel.add(textLabel);

        JLabel value = new JLabel(valueText);
        value.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        value.setForeground(new Color(60, 60, 80));

        container.add(rowPanel);
        container.add(value);
    }

    private <T> void styleComboBox(JComboBox<T> cb) {
        cb.setBackground(BG_CARD);
        cb.setForeground(TEXT_DARK);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 192, 235), 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 4)
        ));
        
        ListCellRenderer<? super T> existingRenderer = cb.getRenderer();
        cb.setRenderer(new ListCellRenderer<T>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c;
                if (existingRenderer != null) {
                    c = existingRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                } else {
                    DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
                    c = defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
                
                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                    label.setOpaque(true);
                    if (isSelected) {
                        label.setBackground(new Color(115, 72, 195));
                        label.setForeground(Color.WHITE);
                    } else {
                        label.setBackground(Color.WHITE);
                        label.setForeground(TEXT_DARK);
                    }
                }
                return c;
            }
        });

        cb.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton() {
                    @Override
                    public void paint(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(115, 72, 195));
                        int[] xPoints = {getWidth() / 2 - 5, getWidth() / 2, getWidth() / 2 + 5};
                        int[] yPoints = {getHeight() / 2 - 2, getHeight() / 2 + 3, getHeight() / 2 - 2};
                        g2.fillPolygon(xPoints, yPoints, 3);
                        g2.dispose();
                    }
                };
                btn.setBorderPainted(false);
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setOpaque(false);
                btn.setPreferredSize(new Dimension(24, 0));
                return btn;
            }

            @Override
            protected javax.swing.plaf.basic.ComboPopup createPopup() {
                javax.swing.plaf.basic.BasicComboPopup popup = new javax.swing.plaf.basic.BasicComboPopup(comboBox) {
                    @Override
                    protected JScrollPane createScroller() {
                        JScrollPane scroller = super.createScroller();
                        styleScrollPane(scroller);
                        return scroller;
                    }
                };
                popup.setBorder(BorderFactory.createLineBorder(new Color(210, 192, 235), 1));
                return popup;
            }
        });
    }
}

// ===================== ROUNDED BUTTON =====================
class RoundedButton extends JButton {
    private int radius;
    private boolean useGradient = false;

    public RoundedButton(String text, int radius) {
        super(text);
        this.radius = radius;
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public RoundedButton(String text, int radius, boolean useGradient) {
        this(text, radius);
        this.useGradient = useGradient;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = getBackground();
        if (useGradient) {
            Color c1 = new Color(120, 80, 200);
            Color c2 = new Color(90, 60, 160);
            if (getModel().isPressed()) {
                c1 = c1.darker();
                c2 = c2.darker();
            } else if (getModel().isRollover()) {
                c1 = c1.brighter();
                c2 = c2.brighter();
            }
            g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
        } else {
            if (getModel().isPressed()) {
                g2.setColor(bg.darker());
            } else if (getModel().isRollover()) {
                g2.setColor(bg.brighter());
            } else {
                g2.setColor(bg);
            }
        }

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

        if (isBorderPainted()) {
            g2.setColor(getForeground());
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}

// ===================== ROUNDED PANEL =====================
class RoundedPanel extends JPanel {
    private int radius;
    private int shadowSize;
    private Color shadowColor = new Color(0, 0, 0, 15);
    private int headerHeight = 0;
    private Color headerColor = null;

    public RoundedPanel(int radius, int shadowSize) {
        super();
        this.radius = radius;
        this.shadowSize = shadowSize;
        setOpaque(false);
    }

    public void setShadowColor(Color color) { this.shadowColor = color; }

    public void setHeader(int height, Color color) {
        this.headerHeight = height;
        this.headerColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - shadowSize * 2;
        int h = getHeight() - shadowSize * 2 - 4; // offset bottom shadow

        if (shadowSize > 0) {
            for (int i = 0; i < shadowSize; i++) {
                int alpha = (int) (shadowColor.getAlpha() * ((float) i / shadowSize));
                g2.setColor(new Color(shadowColor.getRed(), shadowColor.getGreen(), shadowColor.getBlue(), alpha));
                g2.fillRoundRect(shadowSize - i, shadowSize - i + 2, w + i * 2, h + i * 2, radius + i, radius + i);
            }
        }

        g2.setColor(getBackground());
        g2.fillRoundRect(shadowSize, shadowSize, w, h, radius, radius);

        // Draw a clean divider line under the header area
        if (headerHeight > 0 && headerColor != null) {
            g2.setColor(headerColor);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new Line2D.Double(shadowSize + 18, shadowSize + headerHeight - 4, shadowSize + w - 18, shadowSize + headerHeight - 4));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}

// ===================== MENU BUTTON =====================
class MenuButton extends JPanel {
    private JLabel label;
    private Color normalColor = new Color(28, 14, 56);
    private Color hoverColor = new Color(46, 23, 105);
    private Color activeColor = new Color(82, 50, 143);
    private Color currentColor = normalColor;
    private javax.swing.Timer timer;
    private boolean isHovered = false;
    private boolean isActive = false;

    public MenuButton(String text) {
        super(new FlowLayout(FlowLayout.LEFT, 22, 11));
        setMaximumSize(new Dimension(220, 48));
        setBackground(normalColor);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        label = new JLabel(text);
        label.setForeground(new Color(180, 160, 210));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        add(label);

        timer = new javax.swing.Timer(15, e -> {
            Color target = isActive ? activeColor : (isHovered ? hoverColor : normalColor);
            float step = 0.2f;
            int r = (int) (currentColor.getRed() + (target.getRed() - currentColor.getRed()) * step);
            int g = (int) (currentColor.getGreen() + (target.getGreen() - currentColor.getGreen()) * step);
            int b = (int) (currentColor.getBlue() + (target.getBlue() - currentColor.getBlue()) * step);
            
            if (Math.abs(target.getRed() - currentColor.getRed()) < 2 &&
                Math.abs(target.getGreen() - currentColor.getGreen()) < 2 &&
                Math.abs(target.getBlue() - currentColor.getBlue()) < 2) {
                currentColor = target;
                timer.stop();
            } else {
                currentColor = new Color(r, g, b);
            }
            setBackground(currentColor);
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { isHovered = true; timer.start(); }
            public void mouseExited(MouseEvent e)  { isHovered = false; timer.start(); }
        });
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (active) {
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        } else {
            label.setForeground(new Color(180, 160, 210));
            label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        }
        timer.start();
    }

    public JLabel getLabel() { return label; }
}

// ===================== DONUT CHART =====================
class DonutChartPanel extends JPanel {
    private Map<String, Double> data;
    private Color[] colors = {
        new Color(231, 76, 60), new Color(46, 204, 113),
        new Color(52, 152, 219), new Color(155, 89, 182),
        new Color(241, 196, 15), new Color(26, 188, 156)
    };
    private double currentSweep = 0.0;
    private javax.swing.Timer timer;

    public DonutChartPanel(Map<String, Double> data) {
        this.data = data;
        setOpaque(false);
        timer = new javax.swing.Timer(15, e -> {
            currentSweep += 0.05;
            if (currentSweep >= 1.0) {
                currentSweep = 1.0;
                timer.stop();
            }
            repaint();
        });
        timer.start();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (data.isEmpty()) {
            g2.setColor(new Color(150, 150, 170));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("Tidak ada data", getWidth() / 2 - 40, getHeight() / 2);
            return;
        }

        double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
        int cx = getWidth() / 2, cy = getHeight() / 2 - 15;
        int r = Math.min(cx, cy) - 15;
        int inner = r / 2;

        double startAngle = 90; // Start at top
        int i = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double sweep = (entry.getValue() / total * 360.0) * currentSweep;
            g2.setColor(colors[i % colors.length]);
            g2.fill(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, startAngle, -sweep, Arc2D.PIE));
            startAngle -= sweep;
            i++;
        }

        g2.setColor(getBackground());
        g2.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);

        int lx = 5, ly = getHeight() - 20;
        i = 0;
        for (String key : data.keySet()) {
            g2.setColor(colors[i % colors.length]);
            g2.fillRect(lx, ly, 10, 10);
            g2.setColor(new Color(80, 80, 100));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.drawString(key, lx + 14, ly + 10);
            lx += g2.getFontMetrics().stringWidth(key) + 35;
            i++;
        }
    }
}

// ===================== BAR CHART =====================
class BarChartPanel extends JPanel {
    private List<Transaction> transactions;

    public BarChartPanel(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double pemasukan = transactions.stream()
            .filter(t -> t.getJenis().equals("Pemasukan"))
            .mapToDouble(Transaction::getNominal).sum();
        double pengeluaran = transactions.stream()
            .filter(t -> t.getJenis().equals("Pengeluaran"))
            .mapToDouble(Transaction::getNominal).sum();

        double max = Math.max(pemasukan, pengeluaran);
        if (max == 0) return;

        int h = getHeight() - 60;
        int barW = 60;
        int gap = 40;
        int baseY = getHeight() - 30;

        int ph = (int) (pemasukan / max * h);
        g2.setColor(new Color(46, 204, 113));
        g2.fillRoundRect(60, baseY - ph, barW, ph, 8, 8);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(60, 60, 80));
        g2.drawString("Pemasukan", 55, baseY + 15);

        int exh = (int) (pengeluaran / max * h);
        g2.setColor(new Color(231, 76, 60));
        g2.fillRoundRect(60 + barW + gap, baseY - exh, barW, exh, 8, 8);
        g2.drawString("Pengeluaran", 60 + barW + gap - 5, baseY + 15);
    }
}

// ===================== MODERN SCROLLBAR UI =====================
class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
    private final Color THUMB_COLOR = new Color(180, 160, 210, 150);
    private final Color THUMB_HOVER_COLOR = new Color(115, 72, 195, 200);
    private final Color TRACK_COLOR = new Color(243, 240, 250);

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(TRACK_COLOR);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isThumbRollover() ? THUMB_HOVER_COLOR : THUMB_COLOR);
        
        int x = thumbBounds.x + 2;
        int y = thumbBounds.y + 2;
        int w = thumbBounds.width - 4;
        int h = thumbBounds.height - 4;
        g2.fillRoundRect(x, y, w, h, 6, 6);
        g2.dispose();
    }
}