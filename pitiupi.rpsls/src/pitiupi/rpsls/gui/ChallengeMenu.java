package pitiupi.rpsls.gui;

import pitiupi.models.Menus.UnicastMenus.UnicastActionMenu;
import pitiupi.models.User;
import pitiupi.rpsls.plugin.RpslsPlugin;

/**
 * Context-menu item of the online users panel (category "Jogos").
 * Clicking it challenges the selected user. The label is in Portuguese,
 * like the rest of Pitiupi's UI.
 */
public class ChallengeMenu extends UnicastActionMenu {

    private final RpslsPlugin plugin;

    public ChallengeMenu(RpslsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getText() {
        return "Desafiar para RPSLS";
    }

    @Override
    public void onClick(User user) {
        plugin.challenge(user);
    }
}
