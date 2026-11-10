package pitiupi.rpsls.plugin;

import pitiupi.api.plugin.PeerActionsRegistrar;
import pitiupi.api.plugin.PluginWorkspace;
import pitiupi.application.peeractions.PeerAction;
import pitiupi.application.peeractions.PeerActionTarget;
import pitiupi.application.peeractions.PeerTarget;
import pitiupi.application.presence.UserPeerView;
import pitiupi.control.ConnectionManager;
import pitiupi.models.Messages.Message;
import pitiupi.models.Plugin;
import pitiupi.net.tcp.TcpAttemptListener;
import pitiupi.net.tcp.TcpConnection;
import pitiupi.presentation.shell.LocalNetworkInfo;
import pitiupi.presentation.shell.PeerIpv4;
import pitiupi.rpsls.game.GameSession.Role;
import pitiupi.rpsls.gui.RpslsPanel;
import pitiupi.rpsls.net.Match;
import pitiupi.rpsls.net.RpslsMessage;

import javax.swing.SwingUtilities;
import java.io.IOException;
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
 * between the two plugin instances (identifier {@value #ID}) with
 * {@link ConnectionManager#connectPluginTcpAsync}.</p>
 *
 * <ul>
 *   <li>The game is a tab in Pitiupi's main window ({@link RpslsPanel},
 *       registered with {@code bindShellWorkspaceContent}).</li>
 *   <li>Peer action on a user in the side list: "Desafiar para Pedra, Papel,
 *       Tesoura, Lagarto, Spock" opens the tab and sends INVITE. The tab's
 *       lobby also allows challenging by IP.</li>
 *   <li>{@link #receiveBytes}: hands the bytes to that connection's match; an
 *       INVITE on a new connection creates the match on the invited side and
 *       opens the tab.</li>
 *   <li>One match at a time: invitations received during a match are
 *       declined with {@code DECLINE("busy")}.</li>
 * </ul>
 *
 * <p>Player-facing text is in Portuguese, like Pitiupi itself.</p>
 */
public class RpslsPlugin extends Plugin {

    /** Identifier used as sender and receiver of the private connection. */
    public static final String ID = "pitiupi.rpsls";

    private static final String DISPLAY_NAME = "Pedra, Papel, Tesoura, Lagarto, Spock";
    private static final Logger LOG = Logger.getLogger(RpslsPlugin.class.getName());

    private ConnectionManager connections;
    private PluginWorkspace workspace;
    private RpslsPanel panel;

    /** One thread for deadlines and player actions (off the Swing and network threads). */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rpsls-timer");
        t.setDaemon(true);
        return t;
    });

    private final Map<TcpConnection, Match> matches = new ConcurrentHashMap<>();
    private Match active;         // guarded by "this"
    private boolean connecting;   // guarded by "this"

    @Override public String getName()        { return "RPSLS - " + DISPLAY_NAME; }
    @Override public String getDisplayName() { return DISPLAY_NAME; }
    @Override public String getAuthor()      { return "Paulo Felipe"; }
    @Override public String getVersion()     { return "1.0.0"; }
    @Override public String getIdentifier()  { return ID; }

    @Override
    public void createPlugin(ConnectionManager connectionManager, PluginWorkspace workspace) {
        this.connections = connectionManager;
        this.workspace = workspace;
        this.panel = new RpslsPanel(this::challenge, LocalNetworkInfo::bestLocalIpv4);
        bindShellWorkspaceContent(panel);
        installPeerActions(workspace.peerActions());
        LOG.info("[RPSLS] plugin loaded. Port " + connectionManager.getPort()
                + ", local IP " + LocalNetworkInfo.bestLocalIpv4());
    }

    @Override
    public void installPeerActions(PeerActionsRegistrar registrar) {
        registrar.register(PeerAction.of(
                "rpsls.challenge",
                "Desafiar para " + DISPLAY_NAME,
                PeerActionTarget.SINGLE_PEER,
                target -> {
                    openTab();
                    if (target instanceof PeerTarget.Single single) challengePeer(single.peer());
                }
        ));
        registrar.register(PeerAction.of(
                "rpsls.open",
                "Abrir " + DISPLAY_NAME,
                PeerActionTarget.ALL_PEERS,
                target -> openTab()
        ));
    }

    /** The game does not use public messages (multicast / public TCP). */
    @Override
    public List<Class<? extends Message>> getMessages() {
        return List.of();
    }

    @Override
    public void receiveMessage(Message message) { /* not used */ }

    /** The player closed the plugin tab: leave the match in progress. */
    @Override
    public void onShellTabClosed() {
        Match m;
        synchronized (this) { m = active; }
        if (m != null) m.act(s -> s.quit("fechou a aba do jogo"));
    }

    // ------------------------------------------------------------------
    // Challenging (the inviting side)
    // ------------------------------------------------------------------

    private void challengePeer(UserPeerView peer) {
        if (peer.isSelf() || peer.isBroadcastTarget()) return;
        String ip = peer.getInetAddress().map(PeerIpv4::fromInetAddress).orElse(null);
        if (ip == null) {
            panel.showLobbyStatus("Não foi possível obter o IP de " + peer.getDisplayName() + ".", true);
            return;
        }
        challenge(ip, peer.getDisplayName());
    }

    /** Challenges by IP (peer action or the lobby's "Desafiar" button). Runs on the Swing thread. */
    public void challenge(String ipText, String label) {
        synchronized (this) {
            if (active != null || connecting) {
                panel.showLobbyStatus("Você já está em uma partida.", true);
                return;
            }
            connecting = true;
        }
        String ip = PeerIpv4.normalize(ipText);
        connections.connectPluginTcpAsync(ip, ID, ID, new TcpAttemptListener() {
            @Override
            public void onConnecting(String endpoint) {
                panel.showLobbyStatus("Conectando a " + label + " (" + endpoint + ")...", false);
            }

            @Override
            public void onSuccess(TcpConnection conn) {
                Match match;
                synchronized (RpslsPlugin.this) {
                    connecting = false;
                    if (active != null) {
                        panel.showLobbyStatus("Você já está em uma partida.", true);
                        return;
                    }
                    match = register(conn, Role.INVITER);
                }
                match.setView(panel.attach(match, connections.getUserName(), label));
                timer.execute(() -> match.session().invite());
            }

            @Override
            public void onFailure(String message) {
                synchronized (RpslsPlugin.this) { connecting = false; }
                LOG.warning("[RPSLS] failed to connect to " + ip + ": " + message);
                panel.showLobbyStatus("Não foi possível conectar em " + label + " (" + ip
                        + ", porta " + connections.getPort() + ").\n"
                        + "Confira se o Pitiupi dele está aberto, na mesma porta, com o plugin instalado,\n"
                        + "e se o firewall dos dois permite o Java na rede.", true);
            }
        });
    }

    // ------------------------------------------------------------------
    // Receiving (Pitiupi's network threads)
    // ------------------------------------------------------------------

    @Override
    public boolean receivePrivateConnection(TcpConnection connection) {
        // Called on both sides. The match is only created on INVITE (receiver)
        // or after connecting (inviter).
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
            if (active != null || connecting) {
                LOG.info("[RPSLS] invitation from " + msg.getSenderName() + " declined: busy");
                sendRaw(connection, RpslsMessage.decline(msg.getSessionId(), "jogador ocupado em outra partida"));
                return true;
            }
            match = register(connection, Role.INVITEE);
        }
        Match invited = match;
        String from = msg.getSenderName();
        onSwing(() -> {
            invited.setView(panel.attach(invited, connections.getUserName(), from));
            openTab();
            workspace.setStatusMessage(from + " te desafiou para " + DISPLAY_NAME + "!");
        });
        onSwing(() -> invited.onBytes(bytes));   // INVITE → state INVITED → "Bora?" screen
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
        Match match = new Match(conn, role, connections.getUserName(), timer, this::ended);
        matches.put(conn, match);
        active = match;
        return match;
    }

    private synchronized void ended(Match match) {
        matches.remove(match.connection());
        if (active == match) active = null;
        // The match ends at the application level with BYE; the idle private
        // connection is closed by Pitiupi's own timeout.
    }

    private void openTab() {
        onSwing(() -> workspace.openOrFocusTab(getIdentifier(), getDisplayName()));
    }

    private static void onSwing(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    private static void sendRaw(TcpConnection conn, RpslsMessage msg) {
        try {
            conn.send(msg.toByteArray());
        } catch (IOException e) {
            LOG.log(Level.WARNING, "[RPSLS] failed to send " + msg.getType(), e);
        }
    }
}
