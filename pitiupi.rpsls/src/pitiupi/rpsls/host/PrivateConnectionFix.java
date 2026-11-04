package pitiupi.rpsls.host;

import pitiupi.control.plugins.PluginManager;
import pitiupi.models.Plugin;
import pitiupi.net.tcp.TcpConnection;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Workaround for a core bug in some Pitiupi versions.
 *
 * <p><b>The bug:</b> in {@code PluginManager.handleConnection} the core calls
 * {@code plugin.receivePrivateConnection(conn)} and then always returns
 * {@code false} (branches {@code main} and {@code release/facade.v2.0.0}).
 * {@code SocketTCP} then thinks the plugin does not exist, replies
 * {@code RECEIVER_NOT_FOUND} and <b>never starts reading</b> the private
 * connection: the plugin never gets {@code receiveBytes}. The fix (returning
 * {@code true}) is in branch {@code update/mpChat}.</p>
 *
 * <p><b>Detection:</b> {@link #coreIsBroken} creates an isolated
 * {@code PluginManager}, registers a test plugin in it and calls
 * {@code handleConnection}. If the core returns {@code false} even with the
 * plugin present, it is the buggy version. The real application is never
 * touched.</p>
 *
 * <p><b>Workaround:</b> if the core has the bug, the plugin reads the private
 * connection itself in a thread ({@link #startReader}), calling
 * {@code TcpConnection.receive()} (the same 4-byte length framing the core
 * uses). On a fixed core none of this is enabled.</p>
 */
public final class PrivateConnectionFix {

    private static final Logger LOG = Logger.getLogger(PrivateConnectionFix.class.getName());

    private PrivateConnectionFix() {}

    /**
     * @param probe a throwaway plugin instance whose
     *              {@code receivePrivateConnection(null)} returns {@code true}.
     */
    public static boolean coreIsBroken(Plugin probe) {
        try {
            PluginManager pm = new PluginManager();
            Class<?> loaded = Class.forName("pitiupi.control.plugins.LoadedPlugin");
            Constructor<?> ctor = loaded.getDeclaredConstructor(Plugin.class, ClassLoader.class);
            ctor.setAccessible(true);
            Field list = PluginManager.class.getDeclaredField("loadedPlugins");
            list.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> plugins = (List<Object>) list.get(pm);
            plugins.add(ctor.newInstance(probe, probe.getClass().getClassLoader()));
            boolean delivered = pm.handleConnection(null, probe.getIdentifier());
            return !delivered;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.log(Level.WARNING, "[RPSLS] could not probe the core; assuming it is fine", e);
            return false;
        }
    }

    /** Reads the private connection instead of the core (only used when {@link #coreIsBroken}). */
    public static void startReader(TcpConnection conn,
                                   BiConsumer<TcpConnection, byte[]> onBytes,
                                   Consumer<TcpConnection> onClosed) {
        Method receive;
        try {
            receive = TcpConnection.class.getDeclaredMethod("receive");
            receive.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            LOG.log(Level.SEVERE, "[RPSLS] cannot read the private connection", e);
            return;
        }
        Thread t = new Thread(() -> {
            while (!conn.isClosed()) {
                try {
                    onBytes.accept(conn, (byte[]) receive.invoke(conn));
                } catch (InvocationTargetException e) {
                    if (e.getCause() instanceof SocketTimeoutException) continue; // idle
                    if (!(e.getCause() instanceof IOException)) {
                        LOG.log(Level.WARNING, "[RPSLS] error reading the connection", e.getCause());
                    }
                    break;
                } catch (IllegalAccessException e) {
                    break;
                }
            }
            onClosed.accept(conn);
        }, "rpsls-reader-" + conn.getAddress().getHostAddress());
        t.setDaemon(true);
        t.start();
    }
}
