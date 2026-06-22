import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
public class EmojiUtils {
    
    public static class AsyncIcon extends ImageIcon {
        private Icon delegate;
        public AsyncIcon(Icon blank) { this.delegate = blank; }
        public void setDelegate(Icon loaded) { this.delegate = loaded; }
        @Override public int getIconWidth() { return delegate.getIconWidth(); }
        @Override public int getIconHeight() { return delegate.getIconHeight(); }
        @Override public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
            delegate.paintIcon(c, g, x, y);
        }
    }
    private static final Map<String, ImageIcon> cache = new HashMap<>();
    private static final Path CACHE_DIR = Paths.get("emojis");

    static {
        try {
            if (!Files.exists(CACHE_DIR)) {
                Files.createDirectories(CACHE_DIR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ImageIcon getIcon(String emoji, int size) {
        return getIcon(emoji, size, null);
    }

    public static ImageIcon getIcon(String emoji, int size, Component repaintTarget) {
        if (emoji == null || emoji.isEmpty()) return null;
        
        // Strip variation selectors or spaces if any
        emoji = emoji.trim();
        if (emoji.isEmpty()) return null;

        String hex = getHex(emoji);
        if (hex.isEmpty()) return null;

        String key = hex + "_" + size;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        // Return blank immediately, load in background
        ImageIcon blankImg = new ImageIcon(new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB));
        AsyncIcon asyncIcon = new AsyncIcon(blankImg);
        cache.put(key, asyncIcon);

        new Thread(() -> {
            try {
                Path localPath = CACHE_DIR.resolve(hex + ".png");
                if (!Files.exists(localPath)) {
                    String urlStr = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/" + hex + ".png";
                    try (InputStream in = new URL(urlStr).openStream()) {
                        Files.copy(in, localPath);
                    } catch (Exception e) {
                        System.err.println("Failed to download emoji: " + hex);
                    }
                }
                
                if (Files.exists(localPath)) {
                    Image img = Toolkit.getDefaultToolkit().getImage(localPath.toString());
                    MediaTracker tracker = new MediaTracker(new JPanel());
                    tracker.addImage(img, 0);
                    tracker.waitForID(0);
                    
                    Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    tracker = new MediaTracker(new JPanel());
                    tracker.addImage(scaled, 1);
                    tracker.waitForID(1);
                    
                    ImageIcon loaded = new ImageIcon(scaled);
                    asyncIcon.setDelegate(loaded);
                    cache.put(key, loaded); // Replace async with real icon in cache
                    
                    if (repaintTarget != null) {
                        SwingUtilities.invokeLater(repaintTarget::repaint);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return asyncIcon;
    }

    private static String getHex(String emoji) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < emoji.length(); i++) {
            int cp = emoji.codePointAt(i);
            if (cp == 0xFE0F) { // Skip variation selector
                continue;
            }
            if (sb.length() > 0) sb.append("-");
            sb.append(Integer.toHexString(cp));
            if (Character.isSupplementaryCodePoint(cp)) {
                i++;
            }
        }
        return sb.toString();
    }
}
