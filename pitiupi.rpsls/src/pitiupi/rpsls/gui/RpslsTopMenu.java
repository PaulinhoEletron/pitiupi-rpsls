package pitiupi.rpsls.gui;

import pitiupi.models.Menus.MulticastMenus.MulticastActionMenu;
import pitiupi.models.Menus.MulticastMenus.MulticastMenu;
import pitiupi.models.Menus.MulticastMenus.MulticastSubMenu;
import pitiupi.rpsls.plugin.RpslsPlugin;

import javax.swing.JOptionPane;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * "Jogos → RPSLS" menu in Pitiupi's menu bar.
 *
 * <p>Plan B for Wi‑Fi networks that block multicast: without multicast the
 * heartbeat does not arrive and the "Online" list stays empty, but a direct
 * TCP (unicast) connection may still work. So:</p>
 * <ul>
 *   <li><b>Meus endereços IP</b> ("My IP addresses"): shows this machine's IP
 *       for the other player to type;</li>
 *   <li><b>Desafiar por IP...</b> ("Challenge by IP"): challenges by typing
 *       the other player's IP.</li>
 * </ul>
 */
public class RpslsTopMenu extends MulticastSubMenu {

    private final RpslsPlugin plugin;

    public RpslsTopMenu(RpslsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getText() {
        return "RPSLS";
    }

    @Override
    public List<MulticastMenu> getMenus() {
        return List.of(
                action("Desafiar por IP...", this::challengeByIp),
                action("Meus endereços IP", RpslsTopMenu::showMyAddresses));
    }

    private void challengeByIp() {
        String ip = JOptionPane.showInputDialog(null,
                "IP do adversário (ele vê em Jogos → RPSLS → Meus endereços IP):",
                "Desafiar por IP", JOptionPane.QUESTION_MESSAGE);
        if (ip == null || ip.isBlank()) return;
        plugin.challengeAddress(ip.trim(), ip.trim());
    }

    private static void showMyAddresses() {
        List<String> lines = localAddresses();
        String text = lines.isEmpty()
                ? "Nenhum endereço de rede encontrado. Você está conectado ao Wi‑Fi?"
                : "Endereços desta máquina:\n\n" + String.join("\n", lines);
        JOptionPane.showMessageDialog(null, text, "Meus endereços IP", JOptionPane.INFORMATION_MESSAGE);
    }

    /** IPv4 of the active interfaces, without loopback and virtual interfaces (Docker etc.). */
    public static List<String> localAddresses() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                String n = ni.getName();
                if (n.startsWith("docker") || n.startsWith("br-") || n.startsWith("veth")
                        || n.startsWith("vbox") || n.startsWith("vmnet")) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        out.add(a.getHostAddress() + "   (" + ni.getDisplayName() + ")");
                    }
                }
            }
        } catch (SocketException ignored) {
            // no interfaces: empty list
        }
        return out;
    }

    private static MulticastActionMenu action(String text, Runnable onClick) {
        return new MulticastActionMenu() {
            @Override public String getText() { return text; }
            @Override public void onClick()   { onClick.run(); }
        };
    }
}
