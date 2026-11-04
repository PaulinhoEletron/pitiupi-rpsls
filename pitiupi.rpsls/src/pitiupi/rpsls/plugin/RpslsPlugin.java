package pitiupi.rpsls.plugin;

import pitiupi.models.Menus.MenuCategory;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.models.User;
import pitiupi.net.tcp.TcpConnection;
import pitiupi.control.ConnectionManager;
import pitiupi.control.GuiManager;
import pitiupi.plugin.Pitiupi;
import pitiupi.rpsls.host.FacadeHost;
import pitiupi.rpsls.host.Host;
import pitiupi.rpsls.host.LegacyHost;
import pitiupi.rpsls.host.PrivateConnectionFix;
import pitiupi.rpsls.game.GameSession.Role;
import pitiupi.rpsls.gui.ChallengeMenu;
import pitiupi.rpsls.gui.MatchWindow;
import pitiupi.rpsls.gui.RpslsTopMenu;
import pitiupi.rpsls.net.Match;
import pitiupi.rpsls.net.RpslsMessage;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point of the Rock, Paper, Scissors, Lizard, Spock (RPSLS) plugin.
 *
 * <p>This is the only class coupled to Pitiupi's plugin API. All game
 * traffic goes through a <b>private</b> (unicast) TCP connection opened
 * between the two plugin instances (identifier {@value #ID}).</p>
 *
 * <p><b>Installation:</b> {@code ant jar} produces {@code dist/pitiupi.rpsls.jar},
 * which must be copied to {@code <repository>/pitiupi/plugins/} (a folder to
 * create). Pitiupi must be started from {@code <repository>/pitiupi}, because
 * it looks for {@code plugins/} relative to the working directory.</p>
 *
 * <ul>
 *   <li>Menu <i>Jogos → Desafiar para RPSLS</i>: opens the connection and sends INVITE.</li>
 *   <li>{@link #receiveBytes}: hands the bytes to that connection's match; an
 *       INVITE on a new connection creates the match on the invited side.</li>
 *   <li>One match at a time: invitations received during a match are
 *       declined with {@code DECLINE("busy")}.</li>
 * </ul>
 *
 * <p>Player-facing text (menus, dialogs) is in Portuguese, like Pitiupi itself.</p>
 */
public class RpslsPlugin extends Plugin {

    /** Identifier used as sender and receiver of the private connection. */
    public static final String ID = "Pitiupi.RPSLS";

    private static final Logger LOG = Logger.getLogger(RpslsPlugin.class.getName());

    private Host pitiupi;

    /** true when the core does not deliver private-connection data (see {@link PrivateConnectionFix}). */
    private boolean readOwnConnections;

    /** One thread for deadlines and player actions (off the Swing and network threads). */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rpsls-timer");
        t.setDaemon(true);
        return t;
    });

    private final Map<TcpConnection, Match> matches = new ConcurrentHashMap<>();
    private Match active; // guarded by "this"

    @Override public String getName()       { return "RPSLS - Pedra, Papel, Tesoura, Lagarto, Spock"; }
    @Override public String getAuthor()     { return "Paulo Felipe"; }
    @Override public String getVersion()    { return "0.6.0"; }
    @Override public String getIdentifier() { return ID; }

    /**
     * Facade API (official tutorial, branch update/mpChat). No
     * {@code @Override}, so it also compiles against the main branch.
     */
    public void createPlugin(Pitiupi pitiupi) {
        init(new FacadeHost(pitiupi));
    }

    /**
     * Legacy API (Pitiupi's current main branch). No {@code @Override},
     * so it also compiles against the facade versions.
     */
    public void createPlugin(ConnectionManager connectionManager, GuiManager guiManager) {
        init(new LegacyHost(connectionManager, guiManager));
    }

    private void init(Host host) {
        this.pitiupi = host;
        this.readOwnConnections = PrivateConnectionFix.coreIsBroken(new RpslsPlugin());
        pitiupi.createMenu(MenuCategory.Jogos, new ChallengeMenu(this));   // Online panel
        pitiupi.createMenu(MenuCategory.Jogos, new RpslsTopMenu(this));    // menu bar (plan B)
        LOG.info("[RPSLS] plugin loaded. API " + host.apiName()
                + (readOwnConnections ? "; core has the private-connection bug: plugin reads connections itself" : "")
                + ". Port " + host.port() + ", addresses: " + RpslsTopMenu.localAddresses()
                + ". Loaded from: " + jarLocation());
    }

    /** The game does not use public messages (multicast / public TCP). */
    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of();
    }

    @Override
    public void receiveMessage(Message message) { /* não usado */ }

    // ------------------------------------------------------------------
    // Challenging (the inviting side)
    // ------------------------------------------------------------------

    /** Called by the Online panel menu. */
    public void challenge(User opponent) {
        challengeAddress(opponent.getAddress().getHostAddress(), opponent.getUserName());
    }

    /** Challenges by IP (Online panel or "Desafiar por IP..."). */
    public void challengeAddress(String host, String label) {
        synchronized (this) {
            if (active != null) {
                info("Você já está em uma partida.");
                return;
            }
        }
        // Connecting blocks (the core sets no timeout; it can take ~20 s if
        // the Wi‑Fi drops the packets): run it off the GUI thread and show a
        // notice meanwhile.
        JDialog connecting = nonModal("Conectando a " + label + " (" + host + ")...");
        timer.execute(() -> {
            TcpConnection conn;
            try {
                conn = pitiupi.connectTcp(InetAddress.getByName(host), ID, ID);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "[RPSLS] failed to connect to " + host, e);
                conn = null;
            } finally {
                SwingUtilities.invokeLater(connecting::dispose);
            }
            if (conn == null) {
                info("Não foi possível conectar em " + label + " (" + host + ", porta " + pitiupi.port() + ").\n\n"
                        + "Verifique se:\n"
                        + " • o Pitiupi dele está aberto e na mesma porta (" + pitiupi.port() + ");\n"
                        + " • o firewall dos dois permite o Java na rede (no Windows: redes públicas também);\n"
                        + " • a Wi‑Fi não isola os clientes (se isolar, usem o hotspot de um celular).");
                return;
            }
            Match match;
            synchronized (this) {
                if (active != null) {
                    info("Você já está em uma partida.");
                    return;
                }
                match = register(conn, Role.INVITER);
            }
            attachWindow(match, label);
            match.session().invite();
        });
    }

    // ------------------------------------------------------------------
    // Receiving (Pitiupi's network threads)
    // ------------------------------------------------------------------

    @Override
    public boolean receivePrivateConnection(TcpConnection connection) {
        if (connection == null) return true; // PrivateConnectionFix probe
        // Called on both sides. The match is only created on INVITE (receiver)
        // or in challenge() (inviter).
        if (readOwnConnections) {
            PrivateConnectionFix.startReader(connection, this::receiveBytes, this::onSocketClosed);
        }
        LOG.info("[RPSLS] private connection with " + connection.getAddress().getHostAddress());
        return true;
    }

    @Override
    public boolean receiveBytes(TcpConnection connection, byte[] bytes) {
        Match match = matches.get(connection);
        if (match != null) {
            match.onBytes(bytes);
            return true;
        }

        // Connection without a match: only INVITE is accepted
        RpslsMessage msg = RpslsMessage.fromBytes(bytes);
        if (msg == null || msg.getType() != RpslsMessage.Type.INVITE) {
            LOG.info("[RPSLS] message ignored (no match): " + msg);
            return true;
        }

        synchronized (this) {
            if (active != null) {
                LOG.info("[RPSLS] invitation from " + msg.getSenderName() + " declined: busy");
                sendRaw(connection, RpslsMessage.decline(msg.getSessionId(), "jogador ocupado em outra partida"));
                return true;
            }
            match = register(connection, Role.INVITEE);
        }
        attachWindow(match, msg.getSenderName());
        match.onBytes(bytes); // INVITE → state INVITED → "Bora?" window
        return true;
    }

    @Override
    public boolean onSocketClosed(TcpConnection connection) {
        LOG.info("[RPSLS] connection closed with " + connection.getAddress().getHostAddress());
        Match match = matches.remove(connection);
        if (match != null) match.onSocketClosed();
        return true;
    }

    // ------------------------------------------------------------------

    private Match register(TcpConnection conn, Role role) {
        Match match = new Match(conn, role, pitiupi.userName(), timer, this::ended);
        matches.put(conn, match);
        active = match;
        return match;
    }

    /** Creates the window on the Swing thread and waits, so no notification is lost. */
    private void attachWindow(Match match, String peerName) {
        try {
            SwingUtilities.invokeAndWait(() ->
                    match.setView(new MatchWindow(match, pitiupi.userName(), peerName)));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[RPSLS] failed to create the window", e);
        }
    }

    private synchronized void ended(Match match) {
        matches.remove(match.connection());
        if (active == match) active = null;
        // Note: the plugin API cannot close the TcpConnection; BYE ends the
        // session at the application level and the connection stays idle
        // until Pitiupi's own timeout.
    }

    private static void sendRaw(TcpConnection conn, RpslsMessage msg) {
        try {
            conn.send(msg.toByteArray());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "[RPSLS] failed to send " + msg.getType(), e);
        }
    }

    /** Non-blocking notice (closes by itself when connecting finishes). */
    private static JDialog nonModal(String text) {
        JDialog[] d = new JDialog[1];
        Runnable create = () -> {
            d[0] = new JOptionPane(text, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                    null, new Object[]{}).createDialog("RPSLS");
            d[0].setModal(false);
            d[0].setVisible(true);
        };
        try {
            if (SwingUtilities.isEventDispatchThread()) create.run();
            else SwingUtilities.invokeAndWait(create);
        } catch (Exception e) {
            return new JDialog();
        }
        return d[0];
    }

    private static String jarLocation() {
        try {
            return new java.io.File(RpslsPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        } catch (Exception e) {
            return "?";
        }
    }

    private static void info(String text) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, text, "RPSLS", JOptionPane.INFORMATION_MESSAGE));
    }
}
