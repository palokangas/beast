/*
 * PakkaWindow.java 
 * Luokka määrittelee sovelluksen pääikkunan, eli "pakan"
 * Muokattu hyödyntäen Oraclen dokumentaation TextComponentDemo.java -mallia 
 */
package beast;

import java.awt.*;
import java.awt.event.*;
import static java.awt.event.ActionEvent.SHIFT_MASK;
import java.util.HashMap;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.Action.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;

public class PakkaWindow extends JFrame {

    // Asettelu Borderlayoutilla
    private BorderLayout borderLayout = new BorderLayout();

    // Valikot
    private JMenuBar menuBar;
    private JMenu cardMenu, editMenu, navigateMenu, helpMenu, windowMenu;

    // Useampaan kohtaan kiinnitettävät toiminnot
    private Action newCardAction, removeAction, exitAction;
    private Action navigateOlderAction, navigateNewerAction, navigateBackAction, navigateForwardAction;
    private Action searchAction, highlightAction, linkAction;
    private Action highlightColorActionRed, highlightColorActionBlue, highlightColorActionYellow, highlightColorActionClear;
    private int hlColorSelector; // 0 = yellow, 1 = red, 2 = blue

    // Kortti
    private JTextPane cardText;
    private StyledDocument doc;
    private StyleContext styles;
    private AttributeSet bgBlue, bgRed, bgYellow, bgClear;

    // Tilarivi
    private JPanel statusBar;
    private JLabel statusCardLabel, statusCreatedLabel, statusSavedLabel;

//   TODO: disabloi nappien tilat kun ne eivät ole käytössä 
//   JButton buttonUp, buttonDown, buttonForward, buttonBack;  		    // Tilarivin napit, jotka eivät aina aktiivisia
    //  boolean backActive, forwardActive, upActive, downActive = false;   // Ja samojen nappien tilat
    private HashMap<Object, Action> actions;

    private UndoAction undoAction;
    private RedoAction redoAction;
    private UndoManager undo = new UndoManager();
    private Deck deck;
    private Card cardOnTop;

    // Pääikkunan, eli "korttipakkanäkymän" konstruktori
    public PakkaWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Hypernote Beast");
        Container contentPane = this.getContentPane();
        contentPane.setLayout(borderLayout);
        setPreferredSize(new Dimension(500, 400));

        // Alustetaan actionit
        createActions();

        try {
            // Luodaan uusi pakka ja asetetaan päällimmäinen kortti esille
            deck = new Deck();
        } catch (Exception ex) {
        }
        cardOnTop = deck.getLast();
        cardText = new JTextPane();
        cardText.setMargin(new Insets(5, 5, 5, 5));

        hlColorSelector = 0; // Oletuksena keltainen korostusväri

        doc = cardOnTop.getContent();
        cardText.setDocument(doc);

        createStyles();
        JScrollPane scrollPane = new JScrollPane(cardText);
        add(scrollPane, BorderLayout.CENTER);

        // Asetetaan valikot, työkalurivi, statusrivi ja tekstikenttä
        this.setJMenuBar(createMenuBar());
        contentPane.add(createToolBar(), BorderLayout.NORTH);
        contentPane.add(createStatusBar(), BorderLayout.SOUTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        cardOnTop.getContent().addUndoableEditListener(new CardUndoListener());
        cardOnTop.getContent().addDocumentListener(new CardDocumentListener());
        addMouseListener();
        addCardListener(cardOnTop);
        deck.clearHistory();
        switchCard(deck.getFirst());

    }

    // Vaihtaa uuden kortin "pakan päälle" ja päivittää näkymän
    public void switchCard(Card newCard) {
        cardOnTop = newCard;
        doc = cardOnTop.getContent();
        cardText.setDocument(doc);
        statusCreatedLabel.setText("Luotu: " + cardOnTop.getCreated());
        statusCardLabel.setText("Kortin Id: " + cardOnTop.getId());
        statusSavedLabel.setText("Tallennettu: " + cardOnTop.getSaved());
        setTitle(cardOnTop.getTitleShortened(40));
        cardOnTop.getContent().addUndoableEditListener(new CardUndoListener());
        cardOnTop.getContent().addDocumentListener(new CardDocumentListener());
        cardText.requestFocusInWindow();
        cardText.setCaretPosition(doc.getLength());
    }

    // Luodaan korostustyylit
    private void createStyles() {
        styles = StyleContext.getDefaultStyleContext();
        bgYellow = styles.addAttribute(
                SimpleAttributeSet.EMPTY,
                StyleConstants.Background, Color.YELLOW);
        bgRed = styles.addAttribute(
                SimpleAttributeSet.EMPTY,
                StyleConstants.Background, Color.PINK);
        bgBlue = styles.addAttribute(
                SimpleAttributeSet.EMPTY,
                StyleConstants.Background, Color.CYAN);
        bgClear = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

    }

    // Jos kursorin kohdalla oleva sana tai sanojen joukko on linkki, palauttaa linkin hakasulkeineen.
    // Jos sana ei ole linkki, palauttaa tuon sanan
    private String getClickedWord(int caretPosition) {

        try {
            int start, end;
            String word = doc.getText(0, doc.getLength());
            word = word.replace(String.valueOf((char) 160), " ");

            // Ollaanko linkin sisällä, eli onko caret [[ ja ]] -merkkien sisässä
            start = word.lastIndexOf("[[", caretPosition - 1);
            if (start != -1) {                                  // Linkin alku löytyi
                word = word.substring(start);
                end = 0;
                while (end < (word.length()) && !(word.substring(end, end + 2)).equals("]]")
                        && !(word.substring(end, end + 1).equals("\n"))) {
                    end++;

                }
                if (word.substring(end, end + 2).equals("]]")) {
                    word = word.substring(0, end + 2);

                    return word;
                }

            } else {

                start = word.lastIndexOf(" ", caretPosition - 1);
                if (start < word.lastIndexOf("\n", caretPosition - 1)) {
                    start = word.lastIndexOf("\n", caretPosition - 1);
                }

                if (start == -1) {
                    start = 0;
                } else {
                    start++;
                }
                word = word.substring(start);
                end = 0;
                while (end < word.length() && !(word.substring(end, end + 1).equals(" "))
                        && !(word.substring(end, end + 1).equals("\n"))) {
                    end++;
                }
                word = word.substring(0, end);
                return word;
            }
        } catch (Exception ex) {
            System.out.println("Ei saada sanaa irti.");
        }
        return "";

    }

    // Rekisteröidään kuuntelija, joka tsekkaa, klikataanko
    // jotain sanaa ctrl-pohjassa. Jos, niin tehdään haku tällä sanalla
    private void addMouseListener() {
        cardText.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isControlDown()) {
                    String word = getClickedWord(cardText.getCaretPosition());
                    System.out.println("Klikattu sana: " + word);

                    // Jos klikattu sana on linkki, seurataan linkki
                    if (word.length() > 5 && word.substring(0, 2).equals("[[")
                            && word.substring(word.length() - 2, word.length()).equals("]]")) {
                        String searchWord = word.substring(2, word.length() - 2);
                        Card newCard = deck.getCardByName(searchWord);
                        if (newCard != null) {
                            switchCard(newCard);
                        } else {
                            // Jos linkkiä ei ole, luodaan uusi kortti annetun linkin perusteella
                            // Tämä on wiki-tyylinen ja parempi kuin alkuperäisessä suunnitelmassa
                            Card newLinkedCard = deck.addCard(word.substring(2, word.length() - 2), "");
                            switchCard(newLinkedCard);
                            System.out.println("Ei löytynyt korttia linkin nimellä.");
                        }

                        // Jos klikattu sana ei ole linkki, tehdään haku tällä sanalla
                    } else if (word.length() > 0) {
                        SearchWindow searchWindow = new SearchWindow(deck, PakkaWindow.this, word);
                        searchWindow.setPreferredSize(new Dimension(600, 400));
                        searchWindow.setLocationRelativeTo(searchWindow);
                        searchWindow.pack();
                        searchWindow.setVisible(true);
                    }

                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

    }

    // Kuunnellaan rivinvaihtoja ja vaihdetaan dokumentin
    // Otsikko samaksi kuin ensimmäisen rivin teksti
    // TODO: Varmistus, että jos otsikko muuttuu ja tähän otsikkoon
    // on olemassa linkki jossain, niin tarjotaan linkin nimen muuttamista
    private void addCardListener(Card card) {

        InputMap inputMap = cardText.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = cardText.getActionMap();
        String lineChange = "linechange";
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), lineChange);
        actionMap.put(lineChange, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    doc.insertString(cardText.getCaretPosition(), "\n", null);
                } catch (Exception ex) {
                    System.out.println("Rivinvaihdon lisäys ei onnistunut.");
                }
                if (cardText.getText().startsWith(cardOnTop.getTitle())) {
                    System.out.println("Otsikko muuttumaton");
                } else {
                    Element element = cardText.getDocument().getDefaultRootElement();
                    Element firstLine = element.getElement(0);
                    int lineLength = firstLine.getEndOffset() - firstLine.getStartOffset();
                    try {
                        cardOnTop.setTitle(cardText.getDocument().getText(firstLine.getStartOffset(), lineLength));
                        System.out.println("Muutettiin otsikoksi: " + cardOnTop.getTitle());
                        setTitle(cardOnTop.getTitleShortened(40));
                    } catch (Exception ex) {
                        System.out.println("Otsikon muuttaminen ei onnistunut.");
                    }
                }
            }
        });

    }

    // Undo-kuuntelija
    protected class CardUndoListener implements UndoableEditListener {

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            undo.addEdit(e.getEdit());
            undoAction.updateUndoState();
            redoAction.updateRedoState();
        }
    }

    // Document-listener-perustoteutus.
    // TODO: Kortin koon rajoitus + mahdollisesti automaattisetmuotoilut tätä kautta
    protected class CardDocumentListener implements DocumentListener {

        public void insertUpdate(DocumentEvent e) {
            displayEditInfo(e);
        }

        public void removeUpdate(DocumentEvent e) {
            displayEditInfo(e);
        }

        public void changedUpdate(DocumentEvent e) {
            displayEditInfo(e);
        }

        private void displayEditInfo(DocumentEvent e) {
            Document document = e.getDocument();

//            int changeLength = e.getLength();
//                       System.out.println(e.getType().toString() + ": "
//                               + changeLength + " character"
//                               + ((changeLength == 1) ? ". " : "s. ")
//                               + " Text length = " + document.getLength()
//                               + ".");
        }
    }

    // Luodaan toiminnot, jotka löytyvät nyt tai lähitulevaisuudessa
    // useammasta kuin yhdestä paikasta
    private void createActions() {

        newCardAction = new NewCardAction("Uusi kortti",
                null,
                "Luo uusi kortti",
                new Integer(KeyEvent.VK_U),
                KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));

        linkAction = new LinkAction("Linkitä",
                null,
                "Luo linkki",
                KeyEvent.VK_L,
                KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));

        highlightAction = new HighlightAction("Korosta",
                null,
                "Korosta valittu teksti",
                KeyEvent.VK_K,
                KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.CTRL_MASK));

        navigateOlderAction = new NavigateOlderAction("Vanhempi",
                null,
                "Navigoi pakassa yhtä vanhempaan korttiin",
                KeyEvent.VK_V,
                KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ActionEvent.CTRL_MASK));

        navigateBackAction = new NavigateBackAction("Edellinen",
                null,
                "Navigoi edelliseen katsomaasi korttiin",
                KeyEvent.VK_E,
                KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, ActionEvent.CTRL_MASK));

        navigateForwardAction = new NavigateForwardAction("Seuraava",
                null,
                "Navigoi seuraavaan katsomaasi korttiin",
                KeyEvent.VK_S,
                KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, ActionEvent.CTRL_MASK));

        navigateNewerAction = new NavigateNewerAction("Uudempi",
                null,
                "Navigoi pakassa yhtä uudempaan korttiin",
                KeyEvent.VK_U,
                KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.CTRL_MASK));

        searchAction = new SearchAction("Haku",
                null,
                "Hae",
                KeyEvent.VK_H,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));

        highlightColorActionYellow = new HighlightColorAction("Keltainen", null, "Keltainen",
                KeyEvent.VK_1, KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        highlightColorActionRed = new HighlightColorAction("Punainen", null, "Punainen",
                KeyEvent.VK_1, KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        highlightColorActionBlue = new HighlightColorAction("Sininen", null, "Sininen",
                KeyEvent.VK_1, KeyStroke.getKeyStroke(KeyEvent.VK_3, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        highlightColorActionClear = new HighlightColorAction("Puhdas", null, "Puhdas",
                KeyEvent.VK_1, KeyStroke.getKeyStroke(KeyEvent.VK_4, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));

    }

    // Luodaan valikko
    public JMenuBar createMenuBar() {
        actions = createActionTable(cardText);
        menuBar = new JMenuBar();
        menuBar.add(createCardMenu());
        menuBar.add(createEditMenu());
        menuBar.add(createNavigateMenu());
        menuBar.add(createWindowMenu());
        menuBar.add(createHelpMenu());
        return menuBar;
    }

    // Luodaan valikon kohta Kortti
    private JMenu createCardMenu() {
        cardMenu = new JMenu("Kortti");
        cardMenu.add(new JMenuItem(newCardAction));
        removeAction = new RemoveAction("Poista kortti",
                null, "Poista kortti pakasta",
                new Integer(KeyEvent.VK_P),
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, ActionEvent.CTRL_MASK));
        cardMenu.add(new JMenuItem(removeAction));
        exitAction = new ExitAction("Poistu",
                null,
                "Poistu sovelluksesta",
                KeyEvent.VK_P,
                null);
        cardMenu.add(new JMenuItem(exitAction));

        JMenuItem menuCardExit = new JMenuItem();

        return cardMenu;
    }

    // Luodaan Muokkaa-valikko
    private JMenu createEditMenu() {
        //Luodaan valikon kohta Muokkaa
        editMenu = new JMenu("Muokkaa");
        editMenu.setMnemonic(KeyEvent.VK_M);

        undoAction = new UndoAction();
        JMenuItem undoItem = new JMenuItem(undoAction);
        undoItem.setText("Peru");
        undoItem.setMnemonic(KeyEvent.VK_P);
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
        editMenu.add(undoItem);

        redoAction = new RedoAction();
        JMenuItem redoItem = new JMenuItem(redoAction);
        redoItem.setText("Tee uudelleen");
        redoItem.setMnemonic(KeyEvent.VK_T);
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
        editMenu.add(redoItem);

        editMenu.addSeparator();

        JMenuItem cut = new JMenuItem(getActionByName(DefaultEditorKit.cutAction));
        cut.setText("Leikkaa");
        cut.setMnemonic(KeyEvent.VK_L);
        cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        editMenu.add(cut);

        JMenuItem copy = new JMenuItem(getActionByName(DefaultEditorKit.copyAction));
        copy.setText("Kopioi");
        copy.setMnemonic(KeyEvent.VK_K);
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        editMenu.add(copy);

        JMenuItem paste = new JMenuItem(getActionByName(DefaultEditorKit.pasteAction));
        paste.setText("Liitä");
        paste.setMnemonic(KeyEvent.VK_I);
        paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        editMenu.add(paste);

        JMenuItem selectAll = new JMenuItem(getActionByName(DefaultEditorKit.selectAllAction));
        selectAll.setText("Valitse kaikki");
        selectAll.setMnemonic(KeyEvent.VK_V);
        selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.CTRL_MASK));
        editMenu.add(selectAll);

        editMenu.addSeparator();

        JMenuItem boldText = new JMenuItem(new StyledEditorKit.BoldAction());
        boldText.setText("Lihavoi");
        boldText.setMnemonic(KeyEvent.VK_L);
        boldText.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
        editMenu.add(boldText);

        JMenuItem italicText = new JMenuItem(new StyledEditorKit.ItalicAction());
        italicText.setText("Kursivoi");
        italicText.setMnemonic(KeyEvent.VK_K);
        italicText.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
        editMenu.add(italicText);

        JMenu colorChooser = new JMenu("Tekstin väri");
        editMenu.add(colorChooser);
        colorChooser.add(new StyledEditorKit.ForegroundAction("Musta", Color.black));
        colorChooser.add(new StyledEditorKit.ForegroundAction("Punainen", Color.red));
        colorChooser.add(new StyledEditorKit.ForegroundAction("Sininen", Color.blue));
        colorChooser.add(new StyledEditorKit.ForegroundAction("Syaani", Color.CYAN));
        colorChooser.add(new StyledEditorKit.ForegroundAction("Oranssi", Color.orange));
        colorChooser.add(new StyledEditorKit.ForegroundAction("Vihreä", Color.green));
        colorChooser.add(new StyledEditorKit.ForegroundAction("Violetti", Color.MAGENTA));

        JMenu highlightChooser = new JMenu("Korostusväri");
        editMenu.add(highlightChooser);
        highlightChooser.add(highlightColorActionYellow);
        highlightChooser.add(highlightColorActionRed);
        highlightChooser.add(highlightColorActionBlue);
        highlightChooser.add(highlightColorActionClear);
        editMenu.addSeparator();
        editMenu.add(new JMenuItem(linkAction));
        return editMenu;

    }

    // Luodaan navigointivalikko
    private JMenu createNavigateMenu() {
        navigateMenu = new JMenu("Navigoi");
        navigateMenu.setMnemonic(KeyEvent.VK_N);

        JMenuItem navigateBackItem = new JMenuItem(navigateBackAction);
        navigateMenu.add(navigateBackItem);

        JMenuItem navigateForwardItem = new JMenuItem(navigateForwardAction);
        navigateMenu.add(navigateForwardItem);

        JMenuItem navigateNewerItem = new JMenuItem(navigateNewerAction);
        navigateMenu.add(navigateNewerItem);

        JMenuItem navigateOlderItem = new JMenuItem(navigateOlderAction);
        navigateMenu.add(navigateOlderItem);

        JMenuItem navigateToMostRecent = new JMenuItem("Uusin");
        navigateToMostRecent.setMnemonic(KeyEvent.VK_I);
        navigateToMostRecent.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, ActionEvent.CTRL_MASK + SHIFT_MASK));
        navigateToMostRecent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Siirrytään viimeisimpään");
                switchCard(deck.getLast());
            }

        });
        navigateMenu.add(navigateToMostRecent);

        JMenuItem navigateToOldest = new JMenuItem("Vanhin");
        navigateToOldest.setMnemonic(KeyEvent.VK_A);
        navigateToOldest.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, ActionEvent.CTRL_MASK + SHIFT_MASK));
        navigateToOldest.addActionListener((ActionEvent e) -> {
            System.out.println("Siirrytään ekaan");
            if (deck.isEmpty() == false) {
                switchCard(deck.getFirst());
            }
        });

        navigateMenu.add(navigateToOldest);

        JMenuItem searchItem = new JMenuItem(new SearchAction("Haku",
                null,
                "Avaa hakuikkuna",
                KeyEvent.VK_H,
                KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK)));
        navigateMenu.add(searchItem);
        return navigateMenu;
    }

    // Luodaan Ikkuna-valikko
    private JMenu createWindowMenu() {
        windowMenu = new JMenu("Ikkuna");
        JMenuItem minimize = new JMenuItem("Pienennä");
        minimize.setMnemonic(KeyEvent.VK_P);
        minimize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, ActionEvent.CTRL_MASK));
        minimize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PakkaWindow.this.setExtendedState(JFrame.ICONIFIED);

            }
        });
        windowMenu.add(minimize);
        return windowMenu;
    }

    // Luodaan Tietoja-valikko
    private JMenu createHelpMenu() {
        helpMenu = new JMenu("Ohje");
        JMenuItem helpItem = new JMenuItem("Käyttöohje");
        helpItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ManualWindow manualWindow = new ManualWindow(PakkaWindow.this);
                manualWindow.pack();
                manualWindow.setVisible(true);
            }
        });
        helpMenu.add(helpItem);
        JMenuItem tietojaItem = new JMenuItem("Tietoja");
        tietojaItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(menuBar,
                        "HyperNote Beast -ohjelman erittäin varhainen demo.\n"
                        + "Käyttöliittymäohjelmoinnin harjoitustyö syksy 2017.",
                        "Tietoja", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        helpMenu.add(tietojaItem);
        return helpMenu;
    }

    // Hashmap, johon laitetaan JTextComponent-luokan valmiit editointikomennot nimen mukaan
    private HashMap<Object, Action> createActionTable(JTextComponent textComponent) {
        HashMap<Object, Action> actions = new HashMap<Object, Action>();
        Action[] actionsArray = textComponent.getActions();
        for (int i = 0; i < actionsArray.length; i++) {
            Action a = actionsArray[i];
            actions.put(a.getValue(Action.NAME), a);
        }
        return actions;
    }

    // Hakee tekstikomponentin mukanaan tuoman editointikomennon nimen mukaan
    private Action getActionByName(String name) {
        return actions.get(name);
    }

    // Action-sisäluokat toiminnoille, jotka löytyvät sekä valikoista että toimintoriviltä
    // Uuden kortin luonti
    class NewCardAction extends AbstractAction {

        NewCardAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            System.out.println("Creating new card");
            Card newCard;
            newCard = deck.addCard();
            switchCard(newCard);
        }
    }

    // Kortin poisto
    class RemoveAction extends AbstractAction {

        RemoveAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            JOptionPane removeDialog = new JOptionPane();
            removeDialog.setIcon(null);
            removeDialog.setSize(new Dimension(200, 100));
            Object[] optiot = {"Poista", "Säilytä"};

            int response = JOptionPane.showOptionDialog(removeDialog, "Poistetaanko kortti?", "Varoitus",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, optiot, optiot[1]);
            if (response == 0) {

                Card temp = deck.removeCard(cardOnTop);
                switchCard(temp);
            }
        }
    }

    // Ohjelmasta poistuminen
    class ExitAction extends AbstractAction {

        ExitAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            // TODO: Kun tallennus on implementoitu niin tähän
            // varmistukset ennen poistumista.
            System.exit(0);
        }
    }

    // Korostus. Korostuksen poisto toteutetaan tässä "korostamalla"
    // taustaväri oletustyylin mukaiseksi eli korostamattomaksi. Nokkelampaa olisi toki tutkia, onko
    // valitulla alueella korostusta ja jos on, poistaa se. Mutta mikä siinä
    // olisi looginen toimintamalli? Esim. jos vain osa korostuksesta valittuna? Tai useampaa?
    class HighlightAction extends AbstractAction {

        HighlightAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            int selStart = cardText.getSelectionStart();
            int selEnd = cardText.getSelectionEnd();
            System.out.println("Teksti: " + selStart + " " + selEnd);

            try {
                switch (hlColorSelector) {
                    case 0:
                        doc.setCharacterAttributes(selStart, (selEnd - selStart), bgYellow, true);
                        cardText.setSelectionStart(selEnd);
                        cardText.requestFocusInWindow();
                        break;
                    case 1:
                        doc.setCharacterAttributes(selStart, (selEnd - selStart), bgRed, true);
                        cardText.setSelectionStart(selEnd);
                        cardText.requestFocusInWindow();
                        break;
                    case 2:
                        doc.setCharacterAttributes(selStart, (selEnd - selStart), bgBlue, true);
                        cardText.setSelectionStart(selEnd);
                        cardText.requestFocusInWindow();
                        break;
                    case 3:
                        doc.setCharacterAttributes(selStart, (selEnd - selStart), bgClear, true);
                        cardText.setSelectionStart(selEnd);
                        cardText.requestFocusInWindow();
                }
            } catch (Exception ex) {
                System.out.println("Korostuksen asettamisessa meni jokin pieleen.");
            }

        }
    }

    // Korostusvärin vaihto
    class HighlightColorAction extends AbstractAction {

        HighlightColorAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            String colorID = e.getActionCommand();

            if ("Sininen".equals(colorID)) {
                System.out.println("Vaihdetaan korostusväriksi " + colorID);
                hlColorSelector = 2;
            } else if ("Punainen".equals(colorID)) {
                hlColorSelector = 1;
                System.out.println("Vaihdetaan korostusväriksi " + colorID);
            } else if ("Keltainen".equals(colorID)) {
                System.out.println("Vaihdetaan korostusväriksi " + colorID);
                hlColorSelector = 0;
            } else {
                System.out.println("Vaihdetaan korostusväriksi " + colorID);
                hlColorSelector = 3;
            }

        }
    }

    // Linkin luominen
    class LinkAction extends AbstractAction {

        LinkAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            String word = getClickedWord(cardText.getCaretPosition());

            // Luodaan valitusta sanasta linkki. Jos ei valintaa niin
            // lisätään linkkimerkit ja kursori niiden väliin
            if (word.length() > 0) {
                try {
                    int temp = cardText.getSelectionEnd();
                    doc.insertString(cardText.getSelectionStart(), "[[", null);
                    doc.insertString(temp + 2, "]]", null);
                    cardText.setSelectionEnd(temp + 4);
                    cardText.setSelectionStart(temp + 4);
                    cardText.requestFocusInWindow();
                } catch (BadLocationException ex) {
                    Logger.getLogger(PakkaWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                try {
                    doc.insertString(cardText.getCaretPosition(), "[[]]", null);
                    cardText.setCaretPosition(cardText.getCaretPosition() - 2);
                    cardText.requestFocusInWindow();
                } catch (BadLocationException ex) {
                    Logger.getLogger(PakkaWindow.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
    }

    // Navigointi historiassa taaksepäin
    class NavigateBackAction extends AbstractAction {

        NavigateBackAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                cardOnTop = deck.browseBack();
                switchCard(cardOnTop);
            } catch (Exception ex) {
                System.out.println("browseBack palautti nullin");
            }
        }
    }

    // Navigointi historiassa eteenpäin
    class NavigateForwardAction extends AbstractAction {

        NavigateForwardAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            try {
                cardOnTop = deck.browseForward();
                switchCard(cardOnTop);
            } catch (Exception ex) {
                System.out.println("browseForward palautti nullin");
            }
        }
    }

    // Navigointi pakassa uudempaan
    class NavigateNewerAction extends AbstractAction {

        NavigateNewerAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            if (cardOnTop == deck.getLast()) {
                System.out.println("Ollaan jo vikassa");
            } else {
                switchCard(deck.nextCard(cardOnTop));
            }
        }
    }

    // Navigointi pakassa vanhempaan
    class NavigateOlderAction extends AbstractAction {

        NavigateOlderAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            if (cardOnTop == deck.getFirst()) {
                System.out.println("Ollaan jo ekassa");
            } else {
                switchCard(deck.previousCard(cardOnTop));
            }
        }
    }

    // Haku
    class SearchAction extends AbstractAction {

        SearchAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke kbshortcut) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
            putValue(ACCELERATOR_KEY, kbshortcut);
        }

        public void actionPerformed(ActionEvent e) {
            SearchWindow searchWindow = new SearchWindow(deck, PakkaWindow.this);
            searchWindow.setPreferredSize(new Dimension(600, 400));
            searchWindow.setLocationRelativeTo(statusBar);
            searchWindow.pack();
            searchWindow.setVisible(true);
        }
    }

    // Undo-toiminto
    class UndoAction extends AbstractAction {

        public UndoAction() {
            super("Peru");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            undo.undo();
            updateUndoState();
            redoAction.updateRedoState();
        }

        protected void updateUndoState() {
            if (undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Peru");
            }
        }
    }

    // Redo-toiminto
    class RedoAction extends AbstractAction {

        public RedoAction() {
            super("Tee uudelleen");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            undo.redo();
            updateRedoState();
            undoAction.updateUndoState();
        }

        protected void updateRedoState() {
            if (undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Tee uudelleen");
            }
        }
    }

    // Luodaan työkalupalkki
    public JToolBar createToolBar() {
        // Luodaan ikonit ja skaalataan samalla liian isot vähän pienemmiksi
        ImageIcon iconBack = new ImageIcon(PakkaWindow.class
                .getResource("images/ic_arrow_back_black_24dp.png"));
        ImageIcon iconForward = new ImageIcon(PakkaWindow.class
                .getResource("images/ic_arrow_forward_black_24dp.png"));
        ImageIcon iconUp = new ImageIcon(PakkaWindow.class
                .getResource("images/ic_arrow_upward_black_24dp.png"));
        ImageIcon iconDown = new ImageIcon(PakkaWindow.class
                .getResource("images/ic_arrow_downward_black_24dp.png"));
        ImageIcon iconAdd = new ImageIcon(new ImageIcon(PakkaWindow.class
                .getResource("images/ic_add_box_black_24dp.png")).getImage().getScaledInstance(24, 24, Image.SCALE_DEFAULT));
        ImageIcon iconColor = new ImageIcon(new ImageIcon(PakkaWindow.class
                .getResource("images/ic_format_color_text_black_24dp.png")).getImage().getScaledInstance(24, 24, Image.SCALE_DEFAULT));
        ImageIcon iconHighlight = new ImageIcon(new ImageIcon(PakkaWindow.class
                .getResource("images/ic_border_color_black_24dp.png")).getImage().getScaledInstance(24, 24, Image.SCALE_DEFAULT));

        ImageIcon iconItalic = new ImageIcon(new ImageIcon(PakkaWindow.class
                .getResource("images/ic_format_italic_black_18dp.png")).getImage().getScaledInstance(24, 24, Image.SCALE_DEFAULT));
        ImageIcon iconBold = new ImageIcon(new ImageIcon(PakkaWindow.class
                .getResource("images/ic_format_bold_black_18dp.png")).getImage().getScaledInstance(24, 24, Image.SCALE_DEFAULT));
        ImageIcon iconLink = new ImageIcon(PakkaWindow.class
                .getResource("images/ic_link_black_18dp.png"));
        ImageIcon iconSearch = new ImageIcon(PakkaWindow.class
                .getResource("images/ic_search_black_18dp.png"));

        JToolBar toolBar = new JToolBar();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.PAGE_START);

        // Ensimmäinen nappiryhmä
        JButton buttonBack = new JButton(navigateBackAction);
        buttonBack.setIcon(iconBack);
        buttonBack.setBorder(null);
        buttonBack.setText("");
        buttonBack.setContentAreaFilled(false);
        buttonBack.setFocusPainted(false);
//        buttonBack.setEnabled(false);              // Alussa historia on tyhjä, joten disabled

        toolBar.add(buttonBack);

        JButton buttonForward = new JButton(navigateForwardAction);
        buttonForward.setIcon(iconForward);
        buttonForward.setText("");
        buttonForward.setBorder(null);
        //buttonBack.setContentAreaFilled(false);
        buttonForward.setFocusPainted(false);
        toolBar.add(buttonForward);
//        buttonForward.setEnabled(false);           // Alussa historia on tyhjä, joten disabled
        toolBar.add(Box.createHorizontalGlue());

        JButton buttonNewCard = new JButton(newCardAction);
        buttonNewCard.setIcon(iconAdd);
        buttonNewCard.setText("");
        buttonNewCard.setBorder(null);
        buttonNewCard.setOpaque(true);
        toolBar.add(buttonNewCard);

        JPopupMenu colorPopup = new JPopupMenu();
        colorPopup.setBorderPainted(true);
        colorPopup.setLabel("Tekstin väri");

        colorPopup.add(new JMenuItem(new StyledEditorKit.ForegroundAction("Musta", Color.black)));
        colorPopup.add(new JMenuItem(new StyledEditorKit.ForegroundAction("Punainen", Color.red)));
        colorPopup.add(new JMenuItem(new StyledEditorKit.ForegroundAction("Sininen", Color.blue)));
        colorPopup.add(new JMenuItem(new StyledEditorKit.ForegroundAction("Syaani", Color.CYAN)));
        colorPopup.add(new JMenuItem(new StyledEditorKit.ForegroundAction("Oranssi", Color.orange)));
        colorPopup.add(new JMenuItem(new StyledEditorKit.ForegroundAction("Vihreä", Color.green)));
        colorPopup.add(new JMenuItem(new StyledEditorKit.ForegroundAction("Violetti", Color.MAGENTA)));

        JButton buttonColor = new JButton();
        buttonColor.setIcon(iconColor);
        buttonColor.setText("");
        buttonColor.setBorder(null);
        buttonColor.setOpaque(true);
        buttonColor.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                colorPopup.show(buttonColor, 0, buttonColor.getBounds().y
                        + buttonColor.getBounds().height);
            }
        });

        toolBar.add(buttonColor);

        JButton buttonHighlight = new JButton(highlightAction);
        buttonHighlight.setIcon(iconHighlight);
        buttonHighlight.setText("");
        buttonHighlight.setBorder(null);
        buttonHighlight.setOpaque(true);
        toolBar.add(buttonHighlight);

        JButton buttonItalic = new JButton(new StyledEditorKit.ItalicAction());
        buttonItalic.setIcon(iconItalic);
        buttonItalic.setText("");
        buttonItalic.setBorder(null);
        buttonItalic.setOpaque(true);
        toolBar.add(buttonItalic);

        JButton buttonBold = new JButton(new StyledEditorKit.BoldAction());
        buttonBold.setIcon(iconBold);
        buttonBold.setText("");
        buttonBold.setBorder(null);
        buttonBold.setOpaque(true);
        toolBar.add(buttonBold);

        JButton buttonLink = new JButton(linkAction);
        buttonLink.setIcon(iconLink);
        buttonLink.setText("");
        buttonLink.setBorder(null);
        buttonLink.setOpaque(true);
        toolBar.add(buttonLink);

        toolBar.add(Box.createHorizontalGlue());

        JButton buttonSearch = new JButton(searchAction);
        buttonSearch.setIcon(iconSearch);
        buttonSearch.setText("");
        //buttonSearch.setContentAreaFilled(false);
        buttonSearch.setFocusPainted(true);
        buttonSearch.setBorder(null);
        buttonSearch.setOpaque(true);
        toolBar.add(buttonSearch);

        JButton buttonDown = new JButton(navigateOlderAction);
        buttonDown.setIcon(iconDown);
        buttonDown.setText("");
        //buttonDown.setContentAreaFilled(false);
        buttonDown.setFocusPainted(true);
        buttonDown.setBorder(null);
        buttonDown.setOpaque(true);
        //if (cardOnTop == deck.getFirst()) {                     // Jos näkyvillä eka kortti --> disabloi
        //     buttonDown.setEnabled(false);
        //    downActive = false;
        // }
        toolBar.add(buttonDown);

        JButton buttonUp = new JButton(navigateNewerAction);
        buttonUp.setIcon(iconUp);
        buttonUp.setText("");
        buttonUp.setBorder(null);
        //if (cardOnTop == deck.getLast()) {                      // Jos näkyvillä viimeinen kortti --> disabloi
        //    buttonUp.setEnabled(false);
        //    upActive = false;
        //}
        toolBar.add(buttonUp);

        return toolBar;

    }

    public JPanel createStatusBar() {
        statusBar = new JPanel(new GridLayout(1, 3));
//        JPanel statusBar = new JPanel();
        statusBar.setPreferredSize(new Dimension(450, 20));
        statusBar.setBorder(new EmptyBorder(0, 5, 0, 5));
        
        Font statusFont = new Font("Serif", Font.PLAIN, 11);

        add(statusBar, BorderLayout.PAGE_END);
        
        // Teksti, joka kertoo kortin id-numeron
        statusCardLabel = new JLabel("Kortin Id: " + cardOnTop.getId());
        statusCardLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusCardLabel.setFont(statusFont);

        // Teksti, joka kertoo, milloin muistiinpanokortti on luotu
        statusCreatedLabel = new JLabel("Luotu: " + cardOnTop.getCreated());
        statusCreatedLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusCreatedLabel.setFont(statusFont);

        // Teksti, joka kertoo, milloin kortti on tallennettu
        statusSavedLabel = new JLabel("tallennettu: ei ");
        statusSavedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusSavedLabel.setFont(statusFont);

        statusBar.add(statusCardLabel);
        statusBar.add(statusCreatedLabel);
        statusBar.add(statusSavedLabel);

        return statusBar;
    }

}
