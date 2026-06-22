public class MainApp {
    public static void main(String[] args) {
        // Path file CSV (di folder MoneyFlow, luar src)
        String csvPath = "data.csv";
        String categoryPath = "categories.csv";

        TransactionManager manager = new TransactionManager(csvPath);
        CategoryManager categoryManager = new CategoryManager(categoryPath);

        javax.swing.SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(manager, categoryManager);
            frame.setVisible(true);
        });
    }
}