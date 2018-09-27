package beast;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

// Hakuikkunan toteutus
public class SearchWindow extends JFrame {

    private String searchString = "";
    private ArrayList<Card> searchMatches;
    private Object[][] searchResults;
    private DefaultTableModel searchTableModel;

    // Toolbar
    private JToolBar searchToolBar = new JToolBar();
    private JButton searchHelp = new JButton("?");
    private JTextField searchField;
    private JButton searchButton = new JButton("Hae");

    // Tämä tietää pakan ulkoasun ja varaston
    private PakkaWindow pakka;
    private Deck deck;

    // Hakutulosten taulukko
    private JTable searchTable;
    private JScrollPane scrollPane;
    private boolean allowSelection = true;

    // Konstruktori, jos ei hakusanaa
    public SearchWindow(Deck deck, PakkaWindow pakka) {
        this(deck, pakka, "");
    }

    // Konstruktori, joka tekee lopuksi haun annetulla hakusanalla
    // TODO: On hyvä, että hakuikkunoita voi olla useampi auki, mutta
    // pitäisikö laittaa joku katto?
    public SearchWindow(Deck deck, PakkaWindow pakka, String searchWord) {
        this.deck = deck;
        this.pakka = pakka;
        this.searchString = searchWord;
        this.setTitle("Haku");
        this.setIconImage(null);

        // Hakuikkunan sulkemisen oletustoiminto
        // TODO: Voisiko haku olla aina auki, vaikka ikkuna ei olisikaan?
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Annetaan sarakkeille nimet
        String[] columnNames = {"Otsikko", "Luontipvm", "Korttinro"};

        // Luodaan tablemodel ja tehdään siitä muokkaamaton
        // TODO: Jatkossa järkevää luoda oma tablemodel defaultin sijaan
        searchTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Luodaan table ja sen asetukset
        searchTable = new JTable(searchTableModel);
        searchTable.setDragEnabled(false);
        searchTable.setShowHorizontalLines(false);
        searchTable.setShowVerticalLines(false);
        searchTable.getTableHeader().setReorderingAllowed(false);
        searchTable.getColumnModel().getColumn(0).setPreferredWidth(410);
        searchTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        searchTable.getColumnModel().getColumn(2).setPreferredWidth(30);

        // Rivin valinta vaihtaa kortin pakkanäkymässä
        // TODO: Tämä toteutustapa aiheuttaa ongelmia! Mieti vaihtoehtoja.
        searchTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                try {
                if (!e.getValueIsAdjusting() && allowSelection == true) {
                    int selected = searchTable.getSelectedRow();
                    deck.setCardOnTop(searchMatches.get(selected), pakka);
                }
                } catch (ArrayIndexOutOfBoundsException aex) {
                    System.out.println("Haun päivitys sotkee taulukon");
                }
            }
        });
        scrollPane = new JScrollPane(searchTable);
        searchTable.setFillsViewportHeight(true);

        // Luodaan sisällölle asettelut
        Container windowContents = this.getContentPane();
        windowContents.setLayout(new BorderLayout());
        windowContents.add(searchToolBar, BorderLayout.PAGE_START);
        windowContents.add(scrollPane, BorderLayout.CENTER);

        // Luodaan työkalupalkki eli tässä tapauksessa hakupalkki
        searchToolBar.setLayout(new BoxLayout(searchToolBar, BoxLayout.X_AXIS));
        searchToolBar.setFloatable(false);

        // Asetetaan työkalupalkin toiminnot
        searchHelp.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                helpDialog();
            }
        });
        searchToolBar.add(searchHelp);

        searchField = new JTextField(searchString);

        // Ohjataan hakukentän enterin painallus haun tekemiseen
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSearch();
            }
        });
        searchToolBar.add(searchField);

        // Ohjataan napin painallus haun tekemiseen
        // TODO: Olisiko syytä tehdä tästä ja ylemmästä yksi abstractaction ja kutsua sitä?
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSearch();

            }
        });

        searchToolBar.add(searchButton);
        if (searchString.length() > 0) {
            doSearch();
        }
        searchField.requestFocusInWindow(); // TODO: Miksi tämä ei toimi tässä, vaikka toisaalla toimii??
    }

    // Metodi, joka toteuttaa haun ja päivittää taulukon sen mukaiseksi
    public void doSearch() {
        allowSelection = false;         // Ei anneta taulukon valinnan päivittyä, kun lista on tyhjä
        searchString = searchField.getText();
        searchTableModel.setRowCount(0);
        System.out.println("Tehdään haku sanalla \"" + searchString + "\"");
        try {
            searchMatches = new ArrayList<>(deck.getSearchResults(searchString));
        } catch (NullPointerException ex) {
            System.out.println("Palautui null");
        }
        
        if (searchMatches.size() > 0) {
            allowSelection = true;
        }
        for (Card card : searchMatches) {
            searchTableModel.addRow(new Object[]{card.getTitle(), card.getCreated(), card.getId()});
            searchTableModel.fireTableDataChanged();
        }

    }

    // Haun ohjedialogi
    // TODO: Luontipäivämäärän mukaan tehtävä haku ei toimi vielä
    private void helpDialog() {
        JOptionPane.showMessageDialog(searchToolBar,
                "- Erota hakusanat toisistaan välilyönnillä\n"
                + "- Voit hakea myös #avainsana:lla\n",
                "Hakuohjeita", JOptionPane.INFORMATION_MESSAGE);
    }

    // TODO: Tee oma tablemodel.
//    class SearchTableModel extends AbstractTableModel {
//        Object[][] data = {};
//        String[] columnTitles = {"Col1", "col2", "Col3"};
//        
//        public int getRowCount() {
//            return data.length;
//        }
//        
//        public int getColumnCount() {
//            return columnTitles.length;
//        }
//
//        public Object getValueAt(int row, int column) {
//            return data[row][column];
//        }
//        
//        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
//            data[rowIndex][columnIndex] = aValue;
//            fireTableCellUpdated(rowIndex, columnIndex);
//        }
//        
//        public void add(Object[] rivi) {
//            for (Object column : rivi) {
//                
//            } 
//            
//        }
//
//    }
}
