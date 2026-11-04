RPSLS - Rock, Paper, Scissors, Lizard, Spock (Pitiupi plugin)
Author: Paulo Felipe
(Versão em português: LEIA-ME.txt)

PLAN B kit: a prebuilt Pitiupi with the plugin already installed, for
someone who cannot build the professor's Pitiupi. The normal way is to
put pitiupi.rpsls.jar into the plugins folder of your own Pitiupi
(see HOW-TO-INSTALL.txt in the plugin kit).

How to play
-----------
1. Requires Java 17 or newer (check with: java -version).
2. Run:  Windows -> jogar.bat     Linux/macOS -> jogar.sh
3. Login: pick a name and use port 8000 (EVERYONE must use the same port).
4. If Windows asks about the firewall, tick "Private networks" AND
   "Public networks" and click Allow.
5. After a few seconds the other player shows up in the "Online" list.
6. Click their name -> Jogos -> Desafiar para RPSLS.
7. They click "Bora!", countdown 3-2-1-Vai!, each one picks a move,
   the winner is shown. "Jogar de novo?" -> Yep on both sides = new round.

If nobody shows up in the Online list
-------------------------------------
The Wi‑Fi may block multicast (automatic discovery). Use the IP instead:
  - one player:  Jogos -> RPSLS -> Meus endereços IP   (note the IP)
  - the other:   Jogos -> RPSLS -> Desafiar por IP...  (type that IP)

If even that does not connect, the Wi‑Fi isolates devices (common on
campus networks): connect both notebooks to a phone hotspot. Turning off
VPN and closing Docker/virtual machines also helps.
