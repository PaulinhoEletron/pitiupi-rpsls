# RPSLS — Rock, Paper, Scissors, Lizard, Spock (Pitiupi plugin)

🇧🇷 *Versão em português: [README.pt-BR.md](README.pt-BR.md)*

A **1‑vs‑1 local‑network game** plugin for [Pitiupi](https://github.com/GabrielFSSantos/pitiupi), the P2P networking platform developed by Prof. Flávio (UFSJ). Author: **Paulo Felipe**.

- Custom application protocol **RPSLS/1.0** over Pitiupi's **private TCP connection (unicast)**: invitation, a fair round with **commit‑reveal (SHA‑256)**, rematch and timeouts. Details in [PROTOCOL.md](PROTOCOL.md) (🇧🇷 [PROTOCOLO.md](PROTOCOLO.md)).
- The game is a **tab in Pitiupi's main window**. It has a lobby, an invitation screen, an animated 3‑2‑1 countdown, a time bar and 3D icons for the 5 moves. The result screen shows both moves side by side with the rule that applied, the score and a Yep/Nope rematch, and a rules screen is always available.
- The game screens are in **Portuguese**, like Pitiupi itself (e.g. *Pedra* = Rock, *Papel* = Paper, *Tesoura* = Scissors, *Lagarto* = Lizard, *Bora!* = "Let's go!", *Jogar de novo?* = "Play again?").

## Requirements
- The current Pitiupi (the version with `plugins.registry.json` and `scripts/pitiupi.sh`).
- JDK 17+ and Apache Ant, the same as Pitiupi itself.

## Quick start (try it locally)

Requires Git, JDK 17+ and `make`:

```sh
git clone https://github.com/GabrielFSSantos/pitiupi.git          # Pitiupi
git clone https://github.com/PaulinhoEletron/pitiupi-rpsls.git    # this plugin
cp -r pitiupi-rpsls/plugins/rpsls pitiupi/plugins/
cd pitiupi/plugins/rpsls

make run    # builds Pitiupi + the plugin and opens Pitiupi with the game installed
```

## Install

### Option 1 — just the `.jar` (no compiling)

1. Open Pitiupi as usual: `./scripts/pitiupi.sh run --all` (Linux/WSL) or `scripts\pitiupi.cmd run --all` (Windows).
2. Go to the **Configurações** tab → **Instalar JAR…** → choose `pitiupi.rpsls.jar`.
3. Restart Pitiupi when asked.

Pitiupi copies the jar into its plugin folder and records it in `plugins.registry.json`, so it stays installed across runs. The tab **Pedra, Papel, Tesoura, Lagarto, Spock** then shows up in the **Plugins** gallery.

### Option 2 — from source, like the other plugins

Put this folder at `plugins/rpsls` in a Pitiupi clone and add this entry to the root `plugins.registry.json`:

```json
{
  "id": "rpsls",
  "dir": "plugins/rpsls",
  "build": "ant",
  "jar": "dist/pitiupi.rpsls.jar",
  "menuCategory": "Jogos"
}
```

Then build and run everything with the official script:

```sh
./scripts/pitiupi.sh run --all            # or only this plugin: --plugins rpsls
```

`./scripts/pitiupi.sh discover` also lists `plugins/rpsls` as a module that is not in the registry yet. To build only the plugin: `cd plugins/rpsls && ant jar` → `dist/pitiupi.rpsls.jar`.

## Play on the same network (two computers)

1. Both players open Pitiupi with **the same port** (in class, usually `5000`) and different names.
2. After a few seconds each one shows up in the other's **Usuários nesta rede** list.
3. One of them clicks the other's name → **Pedra, Papel, Tesoura, Lagarto, Spock** → **Desafiar para Pedra, Papel, Tesoura, Lagarto, Spock** ("Challenge to …").
4. The game tab opens by itself for the other player with "*X* te desafiou! E aí, bora jogar…?" ("X challenged you! Let's play?") → **Bora!**
5. Countdown 3, 2, 1, Vai! ("Go!") → each player clicks a move. There are 10 s; otherwise the game picks one at random.
6. The result screen shows both moves, the rule ("Spock vaporiza Pedra" = "Spock vaporizes Rock"), the winner and the score.
7. **Jogar de novo?** ("Play again?") → **Yep** on both sides starts a new round; a **Nope** ends the match. **Voltar** returns to the lobby.

**Challenge by IP.** If the other player does not show up in the list (some Wi‑Fi networks block multicast, which is how Pitiupi discovers peers), open the game tab from the **Plugins** gallery. The lobby shows **your IP** and has a field to **challenge by IP** over a direct TCP connection.

**If even the IP does not connect**, check that both use the same port, that the firewall allows Java (on Windows, on private *and* public networks), and that the Wi‑Fi does not isolate clients. A phone hotspot is a good fallback.

| Situation | What the game shows |
|---|---|
| Wrong IP / firewall / isolating Wi‑Fi | "Não foi possível conectar em …" (could not connect; check …) in the lobby |
| The other player lacks the plugin | after 30 s: "Convite expirou … Ele tem o plugin RPSLS instalado?" (invitation expired; does he have the plugin?) |
| The other player is in another match | "Convite recusado (jogador ocupado em outra partida)" (declined: busy) |

## Development

```sh
cd plugins/rpsls
ant jar       # or: make           -> dist/pitiupi.rpsls.jar
make test     # 20 tests: rules, commit-reveal, simulated matches, timeouts
make run      # copies the jar to ../../.pitiupi/plugins and starts Pitiupi
```

The Pitiupi console logs every protocol message sent (`->`) and received (`<-`). To capture the traffic for analysis (e.g. in Wireshark), run on either player's machine:
```sh
sudo tcpdump -i any -w rpsls.pcap tcp port 5000
```

## Credits
- Pitiupi — Prof. Flávio, UFSJ ([GitHub](https://github.com/GabrielFSSantos/pitiupi)).
- Move images: [Fluent Emoji](https://github.com/microsoft/fluentui-emoji) © Microsoft, MIT license — see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
