package pitiupi.rpsls.net;

import pitiupi.models.Messages.Message;
import pitiupi.rpsls.game.Move;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

/**
 * Protocol data unit (PDU) of the RPSLS/1.0 protocol.
 *
 * <p>Travels serialized (Java serialization, Pitiupi's default) inside a
 * private TCP connection. Pitiupi frames each message with a 4-byte length
 * prefix, so each {@code send} arrives whole in one {@code receiveBytes} on
 * the other side.</p>
 *
 * <p>Fields used by each type (the others are null/zero):</p>
 * <pre>
 *   type      sessionId  round  other fields
 *   INVITE        x        -    senderName
 *   ACCEPT        x        -    senderName
 *   DECLINE       x        -    reason
 *   COMMIT        x        x    hash
 *   REVEAL        x        x    move, nonce
 *   REMATCH       x        x    accept (Yep = true / Nope = false)
 *   BYE           x        -    reason
 * </pre>
 */
public final class RpslsMessage extends Message {

    private static final long serialVersionUID = 3L;

    /** Protocol version, sent in every message. */
    public static final String PROTOCOL = "RPSLS/1.0";

    public enum Type { INVITE, ACCEPT, DECLINE, COMMIT, REVEAL, REMATCH, BYE }

    private final String protocol = PROTOCOL;
    private final Type type;
    private final String sessionId;
    private final int round;
    private String senderName;
    private String hash;
    private Move move;
    private String nonce;
    private boolean accept;
    private String reason;

    private RpslsMessage(Type type, String sessionId, int round) {
        super(false); // private message (not for every plugin)
        this.type = type;
        this.sessionId = sessionId;
        this.round = round;
    }

    // ------------------------------------------------------------------
    // Factories — one per message type
    // ------------------------------------------------------------------

    public static RpslsMessage invite(String sessionId, String senderName) {
        RpslsMessage m = new RpslsMessage(Type.INVITE, sessionId, 0);
        m.senderName = senderName;
        return m;
    }

    public static RpslsMessage accept(String sessionId, String senderName) {
        RpslsMessage m = new RpslsMessage(Type.ACCEPT, sessionId, 0);
        m.senderName = senderName;
        return m;
    }

    public static RpslsMessage decline(String sessionId, String reason) {
        RpslsMessage m = new RpslsMessage(Type.DECLINE, sessionId, 0);
        m.reason = reason;
        return m;
    }

    public static RpslsMessage commit(String sessionId, int round, String hash) {
        RpslsMessage m = new RpslsMessage(Type.COMMIT, sessionId, round);
        m.hash = hash;
        return m;
    }

    public static RpslsMessage reveal(String sessionId, int round, Move move, String nonce) {
        RpslsMessage m = new RpslsMessage(Type.REVEAL, sessionId, round);
        m.move = move;
        m.nonce = nonce;
        return m;
    }

    public static RpslsMessage rematch(String sessionId, int round, boolean accept) {
        RpslsMessage m = new RpslsMessage(Type.REMATCH, sessionId, round);
        m.accept = accept;
        return m;
    }

    public static RpslsMessage bye(String sessionId, String reason) {
        RpslsMessage m = new RpslsMessage(Type.BYE, sessionId, 0);
        m.reason = reason;
        return m;
    }

    // ------------------------------------------------------------------

    public String getProtocol()   { return protocol; }
    public Type getType()         { return type; }
    public String getSessionId()  { return sessionId; }
    public int getRound()         { return round; }
    public String getSenderName() { return senderName; }
    public String getHash()       { return hash; }
    public Move getMove()         { return move; }
    public String getNonce()      { return nonce; }
    public boolean isAccept()     { return accept; }
    public String getReason()     { return reason; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(protocol).append(' ').append(type);
        if (sessionId != null) sb.append(" session=").append(sessionId, 0, Math.min(8, sessionId.length()));
        if (round > 0) sb.append(" round=").append(round);
        if (senderName != null) sb.append(" from=").append(senderName);
        if (hash != null) sb.append(" hash=").append(hash, 0, 12).append('…');
        if (move != null) sb.append(" move=").append(move);
        if (nonce != null) sb.append(" nonce=").append(nonce, 0, 8).append('…');
        if (type == Type.REMATCH) sb.append(accept ? " Yep" : " Nope");
        if (reason != null) sb.append(" reason=\"").append(reason).append('"');
        return sb.toString();
    }

    /**
     * Deserializes received bytes. Uses the plugin's ClassLoader, because
     * Pitiupi loads each plugin in its own URLClassLoader.
     *
     * @return the message, or {@code null} if the bytes are not an RpslsMessage
     *         or belong to another protocol version.
     */
    public static RpslsMessage fromBytes(byte[] bytes) {
        try (ObjectInputStream in = new PluginInput(new ByteArrayInputStream(bytes))) {
            Object o = in.readObject();
            if (o instanceof RpslsMessage m && PROTOCOL.equals(m.protocol)) return m;
            return null;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    private static final class PluginInput extends ObjectInputStream {
        PluginInput(InputStream in) throws IOException { super(in); }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
            return Class.forName(desc.getName(), false, RpslsMessage.class.getClassLoader());
        }
    }
}
