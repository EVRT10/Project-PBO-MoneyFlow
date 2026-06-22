import java.io.*;
import java.util.*;

public class CategoryManager {
    private List<Category> categories = new ArrayList<>();
    private String filePath;

    // Default kategori bawaan beserta iconnya
    private static final String[][] DEFAULT_CATEGORIES = {
        {"Makanan",      "Pengeluaran", "\uD83C\uDF54"}, // 🍔
        {"Transportasi", "Pengeluaran", "\uD83D\uDE97"}, // 🚗
        {"Belanja",      "Pengeluaran", "\uD83D\uDED2"}, // 🛒
        {"Tagihan",      "Pengeluaran", "\uD83D\uDCCB"}, // 📋
        {"Hiburan",      "Pengeluaran", "\uD83C\uDFAC"}, // 🎬
        {"Kesehatan",    "Pengeluaran", "\uD83D\uDC8A"}, // 💊
        {"Pendidikan",   "Pengeluaran", "\uD83D\uDCDA"}, // 📚
        {"Gaji",         "Pemasukan",   "\uD83D\uDCBC"}, // 💼
        {"Bonus",        "Pemasukan",   "\uD83C\uDF81"}, // 🎁
        {"Investasi",    "Pemasukan",   "\uD83D\uDCC8"}, // 📈
    };

    public CategoryManager(String filePath) {
        this.filePath = filePath;
        loadFromCSV();
    }

    private void loadFromCSV() {
        File file = new File(filePath);
        if (!file.exists()) {
            // Inisialisasi dengan data default
            for (String[] d : DEFAULT_CATEGORIES) {
                categories.add(new Category(d[0], d[1], true, d[2]));
            }
            saveToCSV();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    categories.add(Category.fromCSV(line));
                }
            }
        } catch (IOException e) {
            System.out.println("Gagal membaca kategori: " + e.getMessage());
        }

        // Jika kosong setelah load, isi default
        if (categories.isEmpty()) {
            for (String[] d : DEFAULT_CATEGORIES) {
                categories.add(new Category(d[0], d[1], true, d[2]));
            }
            saveToCSV();
        }
    }

    private void saveToCSV() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), java.nio.charset.StandardCharsets.UTF_8))) {
            for (Category c : categories) {
                bw.write(c.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Gagal menyimpan kategori: " + e.getMessage());
        }
    }

    public List<Category> getAll() {
        return new ArrayList<>(categories);
    }

    public List<Category> getByTipe(String tipe) {
        List<Category> result = new ArrayList<>();
        for (Category c : categories) {
            if (c.getTipe().equals(tipe)) {
                result.add(c);
            }
        }
        return result;
    }

    public void addCategory(Category c) {
        categories.add(c);
        saveToCSV();
    }

    public void updateCategory(int index, String namaBaru, String tipeBaru, String iconBaru) {
        if (index >= 0 && index < categories.size()) {
            categories.get(index).setNama(namaBaru);
            categories.get(index).setTipe(tipeBaru);
            categories.get(index).setIcon(iconBaru);
            saveToCSV();
        }
    }

    public void deleteCategory(int index) {
        if (index >= 0 && index < categories.size()) {
            categories.remove(index);
            saveToCSV();
        }
    }

    public String[] getNamaByTipe(String tipe) {
        return getByTipe(tipe).stream().map(Category::getNama).toArray(String[]::new);
    }

    public String[] getAllNama() {
        return categories.stream().map(Category::getNama).toArray(String[]::new);
    }
}
