package pitiupi.rpsls.host;

import pitiupi.models.Menus.Menu;
import pitiupi.models.Menus.MenuCategory;
import pitiupi.net.tcp.TcpConnection;
import pitiupi.plugin.Pitiupi;

import java.net.InetAddress;

/** Facade API ({@code pitiupi.plugin.Pitiupi}). Only loaded when the core has that API. */
public final class FacadeHost implements Host {
    private final Pitiupi pitiupi;

    public FacadeHost(Pitiupi pitiupi) { this.pitiupi = pitiupi; }

    @Override public TcpConnection connectTcp(InetAddress a, String s, String r) { return pitiupi.connectTcp(a, s, r); }
    @Override public String userName()  { return pitiupi.getUserName(); }
    @Override public int port()         { return pitiupi.getPort(); }
    @Override public void createMenu(MenuCategory c, Menu m) { pitiupi.createMenu(c, m); }
    @Override public String apiName()   { return "facade (createPlugin(Pitiupi))"; }
}
