package beast;

import java.util.*;
import java.util.logging.*;
import javax.swing.text.*;

// Olio, joka käsittelee muistiinpanokorttien pakkaa 
public class Deck {

    private ArrayList<Card> deck;                           // Pakka toimii listana
    private Vector<Card> browseHistory;                     // Korttien käsittelyhistoria
    private int browseIndex = -1;                           // Paikka käsittelyhistoriassa
    private Card onTop;

    // Pakan konstruktori
    public Deck() {

        // Alustetaan pakka
        deck = new ArrayList<>();

        // Alustetaan historialista
        browseHistory = new Vector<Card>();

        // Lisätään kortteja, jotta toimii näin demovaiheessakin
        addExamples();
        onTop = this.getFirst();
    }

    // Lisää uuden tyhjän kortin pakkaan
    public Card addCard() {
        return addCard("", "");
    }

    // Lisää uuden kortin pakkaan, parametreinä kortin otsikko ja teksti
    public Card addCard(String noteTitle, String noteText) {
        Card newCard;
        if (deck.isEmpty()) {
            newCard = new Card(0);
        } else {
            newCard = new Card((this.getLast().getId() + 1));
        }

        Style style = newCard.getContent().addStyle("notestyle", null);

        try {
            if (noteTitle.length() > 0) {
                newCard.setTitle(noteTitle);
            } else {
                newCard.setTitle("Kortti " + newCard.getId());
            }
            newCard.getContent().insertString(0, (noteTitle + noteText), style);
        } catch (Exception ex) {
            System.out.println("Kortin luonti ei onnistunut");
        }

        deck.add(newCard);

        // Päivitetään selaushistoria - eteenpäin menevä historia tuhotaan
        if (browseHistory.size() > (browseIndex + 1)) {
            browseHistory.subList(browseIndex + 1, browseHistory.size()).clear();
        }
        browseHistory.add(newCard);
        browseIndex++;
        printHistory();
        onTop = newCard;
        return newCard;
    }

    // Poistaa kortin pakasta
    public Card removeCard(Card removedCard) {
        System.out.println("Poistetaan kortti nr. " + removedCard.getId());
        Card newCard;
        if (deck.size() == 1) {
            newCard = addCard();
            deck.remove(removedCard);
            browseHistory.clear();
            browseHistory.add(newCard);
        } else if (removedCard == getFirst()) {
            newCard = nextCard(removedCard);
            deck.remove(removedCard);
        } else {
            newCard = previousCard(removedCard);
            deck.remove(removedCard);

        }
        browseHistory.removeIf(i -> i.getId() == removedCard.getId());
        onTop = newCard;
        return newCard;
    }

    // Palauttaa pakan seuraavan kortin
    public Card nextCard(Card currentCard) {
        ListIterator<Card> it = deck.listIterator(deck.indexOf(currentCard) + 1);
        if (it.hasNext()) {
            onTop = it.next();
            // Päivitetään selaushistoria - eteenpäin menevä historia tuhotaan
            if (browseHistory.size() > (browseIndex + 1)) {
                browseHistory.subList(browseIndex + 1, browseHistory.size()).clear();
            }
            browseHistory.add(onTop);
            browseIndex++;
            printHistory();
            return onTop;

        } else {
            System.out.println("Palautetaan null");
            return null;
        }

    }

    // Palauttaa pakan edellisen kortin
    public Card previousCard(Card currentCard) {

        ListIterator<Card> it = deck.listIterator(deck.indexOf(currentCard));
        if (it.hasPrevious()) {
            onTop = it.previous();
            updateHistory();
            
            return onTop;
        } else {
            System.out.println("Palautetaan null");
            return null;
        }

    }
    
    // Päivittää historian geneerisessä tapauksessa,
    // pl. kortin poistaminen, tai selaus historian sisällä
    private void updateHistory() {
            // Päivitetään selaushistoria - eteenpäin menevä historia tuhotaan
            if (browseHistory.size() > (browseIndex + 1)) {
                browseHistory.subList(browseIndex + 1, browseHistory.size()).clear();
            }
            browseHistory.add(onTop);
            browseIndex++;
            printHistory();
    }
    
    // Siirtyy mihin tahansa toiseen korttiin pakassa
    // TODO: tämän optimointi täytyy miettiä, hoitaako sen tietokanta
    // vai ylläpidetäänkö kortissa listaa sen linkkikohteista?
//    public Card moveToCard(Card currentCard, Card newCard) {
//        onTop = newCard;
//        return onTop;
//    }

    // Selaa historiassa eteenpäin
    public Card browseForward() {
        if ((browseIndex + 1) < browseHistory.size()) {
            browseIndex++;
            //           System.out.println("browseForward palauttaa kortin: " + browseHistory.elementAt(browseIndex).id);
            printHistory();
            return browseHistory.elementAt(browseIndex);
        } else {
            System.out.println("Historiassa ei ole uudempia kohteita");
            return getLast();
        }
    }

    // Selaa historiassa taaksepäin
    public Card browseBack() {
        if (browseIndex > 0) {
            browseIndex--;
            printHistory();
            onTop = browseHistory.elementAt(browseIndex);
            return onTop;
        } else {
            System.out.println("Historiassa ei ole aiempia kohteita");
            onTop = getFirst();
            return onTop;
        }
    }

    // Asettaa parametrinä annetun kortin "pakan päälle"
    public void setCardOnTop(Card newCard, PakkaWindow pakka) {
        onTop = newCard;
        if (browseHistory.size() > (browseIndex + 1)) {
            browseHistory.subList(browseIndex + 1, browseHistory.size()).clear();
        }
        browseHistory.add(onTop);
        browseIndex++;
        printHistory();
        pakka.switchCard(newCard);
    }

    // Palauttaa pakan ensimmäisen kortin
    public Card getFirst() {
        onTop = deck.get(0);
        //updateHistory();
        return onTop;
    }

    // Palauttaa pakan viimeisen kortin
    public Card getLast() {
        onTop = deck.get(deck.size() - 1);
       //updateHistory();
        return onTop;
    }

    // Palauttaa pakan koon
    public int size() {
        return deck.size();
    }

    // Palauttaa true, jos korttipakka on tyhjä
    boolean isEmpty() {
        return deck.isEmpty();
    }

    // Asettaa selaushistorian indeksin
    public void setBrowseIndex(int newIndex) {
        browseIndex = newIndex;
    }

    // Palauttaa selaushistorian indeksin
    public int getBrowseIndex() {
        return browseIndex;
    }

    // Palauttaa true, jos ollaan selaushistorian lopussa
    public boolean lastIndex() {
        return (browseHistory.lastElement() == browseHistory.elementAt(browseIndex));
    }

    // Tyhjentää selaushistorian
    public void clearHistory() {
        browseHistory.clear();
        browseIndex = -1;
    }
    
    // Selaushistorian debuggaus, kun joku +-1-ongelma vaivaa
    private void printHistory() {
        if (browseHistory.size() > 0) {
            for (int i = 0; i < browseHistory.size(); i++) {
                System.out.print(browseHistory.elementAt(i).getId() + " ");
            }
        }
        System.out.println(" index: " + browseIndex + " size " + browseHistory.size());
    }

    // Haetaan hakusanaa vastaavat kortit ja palautetaan ne ArrayListinä
    public ArrayList<Card> getSearchResults(String searchString) {
        ArrayList<Card> searchResults = new ArrayList<>();
        System.out.println("Hakusana on tässä vaiheessa:" + searchString + ":");
        for (Card card : deck) {
            try {
                System.out.print("Haetaan kortista : " + card.getId());
                if (card.getContent().getText(0, card.getContent().getLength()).toLowerCase().contains(searchString.toLowerCase())) {
                    if (card != onTop) {
                        searchResults.add(card);
                    }
                    System.out.print(" löytyi: " + card.getId());
                }
                System.out.println("");
            } catch (Exception e) {
                System.out.println("Virhe: hakusana on null");
            }
        }
        System.out.println("");
        return searchResults;
    }

    // Palauttaa kortin, jos parametrinä annettu merkkijono täsmään kortin otsikkoon
    public Card getCardByName(String searchName) {
        for (Card card : deck) {
            if (card.getTitle().equals(searchName)) {
                onTop = card;
                return onTop;
            }
        }
        return null;
    }

    // Lisää puhtaina merkkijonoina joukon kortteja pakkaan
    private void addExamples() {

        addCard("Tervetuloa",
                ("\n\nJos käytät ohjelmaa "
                + "ensimmäistä kertaa, kannattaa käydä alla olevasta linkistä käynnistyvä pieni "
                + "interaktiivinen opastus läpi. Opastus johdattaa sinut ohjelman peruskäyttöön. "
                + "Opastuksen läpikäynti vie pari minuuttia.\n\n"
                + "Jos et halua opastusta, voit luoda uuden tyhjän muistiinpanon klikkaamalla "
                + "työkalupalkin plus-ikonia tai näppäinyhdistelmällä Control-n. "
                + "\n\nAloita painamalla Control-näppäin pohjaan\n"
                + "ja klikkaamalla hiirellä alla olevaa linkkiä:\n\n"
                + "           [[Linkki uuteen korttiin]]"));

        addCard("Linkki uuteen korttiin",
                ("\n\nSiirryit juuri linkin kautta uuteen muistiinpanoon. Jos haluat"
                + " yrittää samaa uudestaan, klikkaa alla olevaa Tervetuloa-linkkiä Control-näppäin"
                + " pohjassa.\n\n[[Tervetuloa]]\n\n"
                + "Muistiinpanokortin ensimmäinen rivi on aina kortin otsikko. "
                + "Voit linkata mistä tahansa muistiinpanosta toiseen laittamalla "
                + " kohteena olevan muistiinpanon otsikon kaksinkertaisten hakasulkeiden väliin, "
                + "kuten edellä olleissa esimerkeissä on tehty. Jos korttia ei löydy, luodaan uusi"
                        + " kortti annetun tekstin perusteella. "
                + "\n\nKortteja voi selata myös järjestyksessä. Tee nyt niin klikkaamalla työkaluriviltä "
                + "nuolta ylöspäin siirtyäksesi pakassa seuraavaan korttiin."));

        addCard("Hakutoiminto",
                ("\n\nNuolet ylös ja alas liikuttavat sinua kortista toiseen yksi "
                + "kerrallaan. Jos kortteja on paljon, haku on paras tapa "
                + "etsiä muistiinpanoja.\n\nHelpoin tapa aloittaa haku on klikata "
                + "Control-näppäin pohjassa mitä tahansa muistiinpanossa olevaa sanaa."
                + " Ctrl-klikkaa seuraavaksi alla olevaa risuaidalla alkavaa sanaa: (ja sitten "
                + " hakuikkunassa olevaa tekstiä)\n\n"
                + "#hakuohjeita"));

        addCard("KLIKKAA MINUA SEURAAVAKSI!",
                ("\n\nKun klikkaat hiirellä Control-näppäin pohjassa mitä tahansa sanaa, "
                + "tehdään haku tuolla sanalla. Jos muistiinpanoja "
                + "on paljon, kannattaa käyttää myös avainsanoja, jotka alkavat"
                + " risuaidalla, näin: #hakuohjeita "
                + "\n\nVoit toki tehdä hakuja millä tahansa sanalla hakuikkunasta, joka "
                + "avautuu suurennuslasista tai näppäinyhdistelmällä Control-s.\n\n"
                + "Opastus on melkein ohi. Ctrl-klikkaa vielä seuraavaa linkkiä: \n\n"
                + "[[Yhteenveto]]"));

        addCard("Yhteenveto",
                ("\n\nOhjelma on siis suhteellisen yksinkertainen. Tee pieniä muistiinpanoja, "
                + "kytke niitä toisiinsa linkeillä ja #avainsanoilla ja käytä"
                + " hakutoimintoa löytääksesi haluamasi. Lopuksi vielä muutama toiminto: \n\n"
                + "- Nuolet vasemmalle ja oikealle liikkuvat selaushistoriassa eteen ja taakse\n"
                + "- Plus-merkki luo uuden muistiinpanon\n"
                + "- Voit vaihtaa tekstin väriä sekä käyttää korostusvärejä (testaa!)\n"
                + "- Voit myös lihavoida ja kursivoida tekstiä\n\n"
                + "Siinäpä se. Seuraavaksi voit vaikka poistaa nämä ohjekortit "
                + "tai klikata plussaa ja luoda uuden muistiinpanon."));
    }

}
