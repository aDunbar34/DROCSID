package client;

import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Plays video files in a frame
 *
 * @author Euan Gilmour
 */
public class VideoPlayer {

    private final JFrame frame;
    private final EmbeddedMediaPlayerComponent mediaPlayer;

    public VideoPlayer() {
        // Check for VLC native libraries
        new NativeDiscovery().discover();
        mediaPlayer = new EmbeddedMediaPlayerComponent();

        frame = new JFrame();
        frame.setSize(800, 600);
        frame.setContentPane(mediaPlayer);
        frame.setTitle("Video Player");
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mediaPlayer.release();
            }
        });
    }

    /**
     * Shows the frame and plays a video
     *
     * @param videoPath The path of the video file
     *
     * @author Euan Gilmour
     */
    public void playVideo(String videoPath) {
        frame.setVisible(true);
        mediaPlayer.mediaPlayer().media().play(videoPath);
    }

}
