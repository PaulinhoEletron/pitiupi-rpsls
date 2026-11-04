package pitiupi.rpsls.host;

import pitiupi.models.Menus.Menu;
import pitiupi.models.Menus.MenuCategory;
import pitiupi.net.tcp.TcpConnection;

import java.net.InetAddress;

/**
 * What the plugin uses from Pitiupi.
 *
 * <p>Pitiupi has two versions of its plugin API:</p>
 * <ul>
 *   <li><b>facade</b> (branches {@code release/facade.v2.0.0}, {@code update/mpChat}
 *       and the official tutorial): {@code createPlugin(Pitiupi)} → {@link FacadeHost};</li>
 *   <li><b>legacy</b> (current {@code main} branch):
 *       {@code createPlugin(ConnectionManager, GuiManager)} → {@link LegacyHost}.</li>
 * </ul>
 * The same {@code pitiupi.rpsls.jar} works with both.
 */
public interface Host {
    TcpConnection connectTcp(InetAddress address, String senderId, String receiverId);
    String userName();
    int port();
    void createMenu(MenuCategory category, Menu menu);
    String apiName();
}
