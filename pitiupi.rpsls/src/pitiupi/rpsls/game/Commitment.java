package pitiupi.rpsls.game;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Commit‑reveal scheme that makes a round fair without a server.
 *
 * <p>Problem: if a player sent their move in the clear, the other could wait
 * to receive it and only then pick the winning move.</p>
 *
 * <p>Two-phase solution:</p>
 * <ol>
 *   <li><b>COMMIT</b>: each player sends only
 *       {@code hash = SHA-256(sessionId | round | MOVE | nonce)}.
 *       The hash does not reveal the move (the random 128-bit nonce prevents
 *       trying the 5 possibilities).</li>
 *   <li><b>REVEAL</b>: after receiving the opponent's commit, each player
 *       sends the move and the nonce. The receiver recomputes the hash and
 *       checks it. Changing the move after committing breaks the hash.</li>
 * </ol>
 * <p>Including {@code sessionId} and the round in the hash prevents reusing a
 * commit from another match/round.</p>
 */
public final class Commitment {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private Commitment() {}

    /** Generates a random 128-bit nonce in hexadecimal. */
    public static String newNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    public static String hash(String sessionId, int round, Move move, String nonce) {
        String input = sessionId + "|" + round + "|" + move.name() + "|" + nonce;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(sha.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static boolean verify(String expectedHash, String sessionId, int round, Move move, String nonce) {
        if (expectedHash == null || move == null || nonce == null) return false;
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.US_ASCII),
                hash(sessionId, round, move, nonce).getBytes(StandardCharsets.US_ASCII));
    }
}
