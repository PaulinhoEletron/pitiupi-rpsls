package pitiupi.rpsls.host;

import pitiupi.control.ConnectionManager;
import pitiupi.control.GuiManager;
import pitiupi.models.Menus.Menu;
import pitiupi.models.Menus.MenuCategory;
import pitiupi.net.tcp.TcpConnection;

import java.net.InetAddress;

/** Legacy API: {@code createPlugin(ConnectionManager, GuiManager)}. */
public final class LegacyHost implements Host {
    private final ConnectionManager connections;
    private final GuiManager gui;

    public LegacyHost(ConnectionManager connections, GuiManager gui) {
        this.connections = connections;
        this.gui = gui;
    }

    @Override public TcpConnection connectTcp(InetAddress a, String s, String r) { return connections.connectPluginTcp(a, s, r); }
    @Override public String userName()  { return connections.getUserName(); }
    @Override public int port()         { return connections.getPort(); }
    @Override public void createMenu(MenuCategory c, Menu m) { gui.createMenu(c, m); }
    @Override public String apiName()   { return "legacy (createPlugin(ConnectionManager, GuiManager))"; }
}
