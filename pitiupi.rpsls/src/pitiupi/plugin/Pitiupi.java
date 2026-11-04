package pitiupi.plugin;

import pitiupi.models.Menus.Menu;
import pitiupi.models.Menus.MenuCategory;
import pitiupi.models.Messages.Message;
import pitiupi.net.tcp.TcpConnection;

import java.net.InetAddress;

/**
 * COMPILE-ONLY STUB — not packaged in the .jar.
 *
 * <p>The {@code pitiupi.plugin.Pitiupi} facade exists in the newer Pitiupi
 * versions (official tutorial, branch update/mpChat) but not in the
 * {@code main} branch. The plugin supports both versions and needs this
 * signature to compile in any clone. At runtime the core's real class is
 * always used: this file is excluded from the jar
 * ({@code dist.archive.excludes} in NetBeans/ant, and in the Makefile).</p>
 */
public class Pitiupi {
    private Pitiupi() {}
    public boolean sendUdpMulticast(Message message) { throw new UnsupportedOperationException(); }
    public boolean sendUdpUnicast(Message message, InetAddress address) { throw new UnsupportedOperationException(); }
    public boolean sendTcp(Message message, InetAddress address) { throw new UnsupportedOperationException(); }
    public TcpConnection connectTcp(InetAddress address, String senderId, String receiverId) { throw new UnsupportedOperationException(); }
    public String getUserName() { throw new UnsupportedOperationException(); }
    public int getPort() { throw new UnsupportedOperationException(); }
    public void createMenu(MenuCategory category, Menu menu) { throw new UnsupportedOperationException(); }
}
