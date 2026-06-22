import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestEmoji {
    public static void main(String[] args) throws Exception {
        String[] emojis = {"🍔", "🚗", "❤️", "👨‍💻"};
        for (String e : emojis) {
            String hex = getHex(e);
            System.out.println(e + " -> " + hex);
            String urlStr = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/" + hex + ".png";
            try (InputStream in = new URL(urlStr).openStream()) {
                Files.copy(in, Paths.get(hex + ".png"));
                System.out.println("Downloaded " + hex);
            } catch (Exception ex) {
                System.out.println("Failed: " + urlStr + " - " + ex.getMessage());
            }
        }
    }

    private static String getHex(String emoji) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < emoji.length(); i++) {
            int cp = emoji.codePointAt(i);
            if (cp == 0xFE0F) { 
                continue;
            }
            if (sb.length() > 0) sb.append("-");
            sb.append(Integer.toHexString(cp));
            if (Character.isSupplementaryCodePoint(cp)) i++;
        }
        return sb.toString();
    }
}
