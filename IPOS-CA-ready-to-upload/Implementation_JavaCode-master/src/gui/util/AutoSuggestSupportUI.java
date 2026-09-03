package gui.util;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextField;

public class AutoSuggestSupportUI {

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
        textfield.repaint();
    }

    private final JTextField textfield;
    private int round = 15;
    private List<String> items = new ArrayList<>();

    public AutoSuggestSupportUI(JTextField textfield) {
        this.textfield = textfield;
        AutoCompleteDecorator.decorate(textfield, items, false);
    }
}