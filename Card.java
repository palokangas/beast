package beast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;

// Muistiinpanokortin toteuttava luokka
public class Card {

    private int id;                
    private String title;
    private StyledDocument content;
    private LocalDateTime created;
    private LocalDateTime modified;
    private LocalDateTime saved;

    public Card(int initId) {
        this(initId, "", null);
    }

    // Tulevissa versioissa tulee mahdollisuus luoda kortteja
    // esim. split- tai join-menetelmillä, joten tämä syytä olla
    // vaikka vielä tässä versiossa voisi mennä ilman
    public Card(int initId, String initTitle, StyledDocument initContent) {
        this.id = initId;
        this.title = initTitle;
        this.content = new DefaultStyledDocument();
        this.created = LocalDateTime.now();
        this.modified = LocalDateTime.now();
        this.saveCard();                            // Tallennetaan heti. Kortin luonti ei undoable.
    }

    // TODO: Tallennus on vielä täysin miettimättä.
    public void saveCard() {
        // Toteuta tallennus
        this.saved = LocalDateTime.now();
    }

    // Palauttaa kortin uniikin Idin
    public int getId() {
        return id;
    }

    // Palauttaa StyledDocumentin sisällön
    public StyledDocument getContent() {
        return content;
    }
    
    // TODO: Palauttaa aina päivämäärän, tätä voisi muokata sen mukaan milloin kortti luotu
    public String getCreated() {
        return created.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    // Jos tallennettu tänään, palauttaa kellonajan, jos aiemmin, palauttaa päivämäärän
    public String getSaved() {
        if (saved.format(DateTimeFormatter.BASIC_ISO_DATE).equals(LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE))) {
            return ("" + saved.getHour() + ":" + saved.getMinute());
        } else {
            return saved.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
    }

    // Title-setter
    public void setTitle(String newTitle) {
        this.title = newTitle;
    }
    
    // Title-getter
    public String getTitle() {
        return title;
    }
    
    //Title-getter rajatulle koolle, jonka voi antaa parametrinä
    public String getTitleShortened(int length) {
        if (title.length() < length) {
            length = title.length();
        }
        return title.substring(0, length);
    }
}