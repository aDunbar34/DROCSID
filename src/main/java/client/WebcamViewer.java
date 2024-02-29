package client;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import javax.swing.*;
import java.awt.*;

public class WebcamViewer extends JFrame {

    public WebcamViewer() {
        setTitle("Webcam Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 480);

        // Create a panel to display webcam feed
        Webcam webcam = Webcam.getDefault();
        webcam.setViewSize(WebcamResolution.VGA.getSize());
        WebcamPanel webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFillArea(true);

        // Add the webcam panel to the JFrame
        add(webcamPanel, BorderLayout.CENTER);
    }

    /*
    public WebcamViewer(InputStream inputStream) {
        setTitle("Webcam Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 480);

        // Create a panel to display webcam feed from InputStream
        WebcamPanel webcamPanel = new WebcamPanel(inputStream);
        webcamPanel.setFillArea(true);

        // Add the webcam panel to the JFrame
        add(webcamPanel, BorderLayout.CENTER);
    }
    */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new WebcamViewer().setVisible(true);
        });
    }
}
