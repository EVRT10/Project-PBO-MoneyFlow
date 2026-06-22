public class Transaction {
    private String tanggal;
    private String keterangan;
    private String jenis; // "Pemasukan" atau "Pengeluaran"
    private double nominal;

    public Transaction(String tanggal, String keterangan, String jenis, double nominal) {
        this.tanggal = tanggal;
        this.keterangan = keterangan;
        this.jenis = jenis;
        this.nominal = nominal;
    }

    public String getTanggal() { return tanggal; }
    public String getKeterangan() { return keterangan; }
    public String getJenis() { return jenis; }
    public double getNominal() { return nominal; }

    // Untuk simpan ke CSV
    public String toCSV() {
        return tanggal + "," + keterangan + "," + jenis + "," + nominal;
    }

    // Untuk baca dari CSV
    public static Transaction fromCSV(String line) {
        String[] parts = line.split(",");
        return new Transaction(parts[0], parts[1], parts[2], Double.parseDouble(parts[3]));
    }
}