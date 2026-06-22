public class Category {
    private String nama;
    private String tipe; // "Pengeluaran" atau "Pemasukan"
    private boolean isDefault;
    private String icon; // emoji icon

    public Category(String nama, String tipe, boolean isDefault, String icon) {
        this.nama = nama;
        this.tipe = tipe;
        this.isDefault = isDefault;
        this.icon = (icon == null || icon.isEmpty()) ? "\uD83D\uDCCC" : icon;
    }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getTipe() { return tipe; }
    public void setTipe(String tipe) { this.tipe = tipe; }

    public boolean isDefault() { return isDefault; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = (icon == null || icon.isEmpty()) ? "\uD83D\uDCCC" : icon; }

    // Format CSV: nama,tipe,isDefault,icon
    public String toCSV() {
        return nama + "," + tipe + "," + isDefault + "," + icon;
    }

    public static Category fromCSV(String line) {
        // Support format lama (3 kolom) dan baru (4 kolom)
        String[] parts = line.split(",", 4);
        String icon = (parts.length >= 4) ? parts[3] : "\uD83D\uDCCC";
        return new Category(parts[0], parts[1], Boolean.parseBoolean(parts[2]), icon);
    }

    @Override
    public String toString() {
        return nama;
    }
}
