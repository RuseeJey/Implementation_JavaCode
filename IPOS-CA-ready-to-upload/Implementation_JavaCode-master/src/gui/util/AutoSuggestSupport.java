package gui.util;

import javax.swing.JTextField;
import java.util.Collection;

public class AutoSuggestSupport extends JTextField {

    private AutoSuggestSupportUI textUI;

    public AutoSuggestSupport() {
        textUI = new AutoSuggestSupportUI(this);
    }

    public void addItemSuggestion(String text) {
        textUI.getItems().add(text);
    }

    public void removeItemSuggestion(String text) {
        textUI.getItems().remove(text);
    }

    public void clearItemSuggestion() {
        textUI.getItems().clear();
    }

    public void setRound(int round) {
        textUI.setRound(round);
    }

    public int getRound() {
        return textUI.getRound();
    }

    public static class TextFieldSuggestion extends AutoSuggestSupport {

        public TextFieldSuggestion() {
            super();
        }

        public void setItems(Collection<String> suggestions) {
            clearItemSuggestion();
            if (suggestions == null) {
                return;
            }
            for (String suggestion : suggestions) {
                if (suggestion != null && !suggestion.trim().isEmpty()) {
                    addItemSuggestion(suggestion.trim());
                }
            }
        }
    }

}