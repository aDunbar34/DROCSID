package client;

import javax.swing.*;
import java.awt.*;

/**
 * Displays an image in a frame
 */
public class ImageViewer {

    private final JFrame frame;

    public ImageViewer() {
        frame = new JFrame();
        frame.setTitle("Image Viewer");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    /**
     * Displays an image in the frame.
     *
     * @param imagePath The path to the image file.
     */
    public void viewImage(String imagePath) {

        // Load image
        ImageIcon imageIcon = new ImageIcon(imagePath);
        Image image = imageIcon.getImage();

        // Create JLabel for image
        JLabel label = new JLabel(new ImageIcon(image));

        // Add label to frame
        frame.add(label);

        // Set frame size based on image dimensions
        frame.setSize(image.getWidth(null), image.getHeight(null));

        // Center frame
        frame.setLocationRelativeTo(null);

        // Show frame
        frame.setVisible(true);

    }

}
