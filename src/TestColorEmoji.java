import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

public class TestColorEmoji {
    public static void main(String[] args) throws Exception {
        JLabel label = new JLabel("🍔🚗❤️👨‍💻");
        label.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        
        // Render to image
        BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 200, 100);
        
        // Enable anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        label.setSize(label.getPreferredSize());
        label.paint(g2);
        g2.dispose();
        
        ImageIO.write(img, "png", new File("emoji_test.png"));
    }
}
