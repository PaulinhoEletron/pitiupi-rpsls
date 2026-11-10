RPSLS - Rock, Paper, Scissors, Lizard, Spock (Pitiupi plugin)
Author: Paulo Felipe
(Versão em português: LEIA-ME.txt)

PLAN B kit: a prebuilt Pitiupi with the plugin already installed, for
someone who cannot build Pitiupi. The normal way is to
install pitiupi.rpsls.jar in your own Pitiupi through
Configurações -> Instalar JAR… (see HOW-TO-INSTALL.txt in the plugin kit).

How to play
-----------
1. Requires Java 17 or newer (check with: java -version).
2. Run:  Windows -> jogar.bat     Linux/macOS -> jogar.sh
3. Login: pick a name and use port 5000 (EVERYONE must use the same port).
4. If Windows asks about the firewall, tick "Private networks" AND
   "Public networks" and click Allow.
5. After a few seconds the other player shows up in the "Usuários nesta rede" list.
6. Click their name -> Pedra, Papel, Tesoura, Lagarto, Spock
   -> Desafiar para Pedra, Papel, Tesoura, Lagarto, Spock.
7. They click "Bora!", countdown 3-2-1-Vai!, each one picks a move,
   the winner is shown. "Jogar de novo?" -> Yep on both sides = new round.

If nobody shows up in the list
------------------------------
The Wi‑Fi may block multicast (automatic discovery). Use the IP instead:
  - open the game tab from the Plugins gallery;
  - the lobby shows "Seu IP" (your IP): tell it to the other player;
  - the other player types it in "Ou desafie pelo IP" -> Desafiar.

If even that does not connect, the Wi‑Fi isolates devices (common on
campus networks): connect both notebooks to a phone hotspot. Turning off
VPN and closing Docker/virtual machines also helps.
