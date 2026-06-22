import java.io.*;
import java.util.*;

public class TransactionManager {
    private List<Transaction> transactions = new ArrayList<>();
    private String filePath;

    public TransactionManager(String filePath) {
        this.filePath = filePath;
        loadFromCSV();
    }

    // Baca data dari CSV saat app dibuka
    private void loadFromCSV() {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    transactions.add(Transaction.fromCSV(line));
                }
            }
        } catch (IOException e) {
            System.out.println("Gagal membaca file: " + e.getMessage());
        }
    }

    // Simpan semua data ke CSV
    private void saveToCSV() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (Transaction t : transactions) {
                bw.write(t.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Gagal menyimpan file: " + e.getMessage());
        }
    }

    // Tambah transaksi baru
    public void addTransaction(Transaction t) {
        transactions.add(t);
        saveToCSV();
    }

    // Update transaksi
    public void updateTransaction(int index, Transaction t) {
        if (index >= 0 && index < transactions.size()) {
            transactions.set(index, t);
            saveToCSV();
        }
    }

    // Hapus transaksi berdasarkan index
    public void deleteTransaction(int index) {
        transactions.remove(index);
        saveToCSV();
    }

    // Ambil semua transaksi
    public List<Transaction> getTransactions() {
        return transactions;
    }

    // Hitung total pemasukan
    public double getTotalPemasukan() {
        return transactions.stream()
                .filter(t -> t.getJenis().equals("Pemasukan"))
                .mapToDouble(Transaction::getNominal)
                .sum();
    }

    // Hitung total pengeluaran
    public double getTotalPengeluaran() {
        return transactions.stream()
                .filter(t -> t.getJenis().equals("Pengeluaran"))
                .mapToDouble(Transaction::getNominal)
                .sum();
    }

    // Hitung saldo
    public double getSaldo() {
        return getTotalPemasukan() - getTotalPengeluaran();
    }
}