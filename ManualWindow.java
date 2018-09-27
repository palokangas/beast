// Erittäin karkea viritelmä ohjeistukseksi...
package beast;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;

/**
 *
 * @author teemu
 */
public class ManualWindow extends JFrame {

    JTextArea manualText;
    JScrollPane scrollPane;
    
    public ManualWindow(PakkaWindow pakka) {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setPreferredSize(new Dimension(550, 440));
        this.setLocationRelativeTo(pakka);
        Container windowContents = this.getContentPane();
        windowContents.setLayout(new BorderLayout());
        
        manualText = new JTextArea("Hei käyttäjä,\n\n"
                + "Tällä ohjelmalla voit tehdä pieniä muistiinpanojen palasia, eli kortteja, jotka "
                + "muodostavat yhden ison pakan. Kortteja voi selata vanhemmista uudempiin järjestyksessä, "
                + "tai - mikä on hyödyllisempää ja nopeampaa - linkkien avulla.\n\n"
                + "Linkki luodaan laittamalla toisen muistiinpanokortin otsikko tupla-hakasulkeisiin näin [[linkki]]. "
                + "Jos linkin sisällä oleva sana(yhdistelmä) ei ole minkään kortin otsikko, tehdään sanalla haku."
                + "\n\nVoit myös luoda avainsanoja laittamalla sanan eteen risuaidan, esimerkiksi #avainsana. Myös avainsanaa "
                + "klikkaamalla avautuu haku, joka hakee kaikki kyseisen avainsanan sisältämät kortit. "
                + "Tämä on hyödyllistä esimerkiksi eri teemoihin tai ihmisiin liittyvien korttien löytämiseksi. "
                + "Jos kaikissa elokuvia käsittelevissä muistiinpanoissa on avainsana #elokuvat, löytyvät ne "
                + "helposti haulla. Tai lähdeviitteenä #Habermas1962-avainsana löytää kaikki ne kortit, joissa olet tehnyt "
                + "muistiinpanoja kyseisen kirjan sisällöstä."
                + "\n\nNäiden lisäksi ohjelmassa on tekstin muokkaamiseen perustoimintoja, kuten lihavointi, "
                + "kursivointi ja värien muuttaminen. Muuta muistiinpanoissa ei juuri tarvitakaan. ");
        manualText.setEditable(false);
        manualText.setLineWrap(true);
        manualText.setWrapStyleWord(true);
        manualText.setMargin(new Insets(10, 10, 10, 10));
        scrollPane = new JScrollPane(manualText);    
        
        windowContents.add(scrollPane, BorderLayout.CENTER);
    }
}
