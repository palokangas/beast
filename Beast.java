package beast;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author teemu
 */
public class Beast {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JFrame.setDefaultLookAndFeelDecorated(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        PakkaWindow pakkaWindow = new PakkaWindow();
        //pakkaWindow.createGUI();
    
        Runnable r = new Runnable() {
            @Override
            public void run() {
                pakkaWindow.pack();
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Dimension windowSize = pakkaWindow.getSize();
                pakkaWindow.setLocation((screenSize.width - windowSize.width) / 2, (screenSize.height -
                        windowSize.height) / 2);

                pakkaWindow.setVisible(true);
            }
        };
        
        try {
        SwingUtilities.invokeAndWait(r);
        } catch (Exception e) {
            System.out.println("Jokin meni vikaan pääikkunaa avatessa");
        }
    }
}
