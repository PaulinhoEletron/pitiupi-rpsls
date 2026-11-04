# RPSLS — Rock, Paper, Scissors, Lizard, Spock (Pitiupi plugin)

🇧🇷 *Versão em português: [README.pt-BR.md](README.pt-BR.md)*

A **1‑vs‑1 local‑network game** for [Pitiupi](https://git.alice.ufsj.edu.br/fls/pitiupi), the P2P networking tool by Prof. Flávio (UFSJ).
Computer Networks course assignment (Master's) — **Paulo Felipe**.

- Custom application protocol **RPSLS/1.0** over Pitiupi's **private TCP connection (unicast)**: invitation, a fair round with **commit‑reveal (SHA‑256)**, rematch and timeouts. Details in [PROTOCOL.md](PROTOCOL.md) (🇧🇷 [PROTOCOLO.md](PROTOCOLO.md)).
- Invitation screen, animated 3‑2‑1 countdown, time bar, 3D icons for the 5 moves, side‑by‑side result with the rule that applied, score, Yep/Nope rematch and a rules screen.
- The game screens are in **Portuguese**, like Pitiupi itself (e.g. *Pedra* = Rock, *Papel* = Paper, *Tesoura* = Scissors, *Lagarto* = Lizard, *Bora!* = "Let's go!", *Jogar de novo?* = "Play again?").

## Requirements
- JDK 17+ (tested with 21) and `ant` (or NetBeans). `make` is optional.
- Docker + Docker Compose, only for the local 2‑player test.

## Build
```sh
cd pitiupi.rpsls
ant jar       # or: make   (builds the core and the plugin -> dist/pitiupi.rpsls.jar)
make test     # tests: rules, commit-reveal and simulated matches
```

## Install into the professor's Pitiupi

The plugin follows the same layout as the other plugins in the repository (`pitiupi.chat`, `pitiupi.mpChat`): a NetBeans/ant project at the repository root that produces a `.jar`, which goes into **`pitiupi/plugins/`**.

```
<repository>/
├── pitiupi/                 the program (NetBeans project)
│   ├── dist/pitiupi.jar
│   └── plugins/             CREATE it (not in git): plugin .jar files go here
│       └── pitiupi.rpsls.jar
├── pitiupi.chat/
├── pitiupi.mpChat/
└── pitiupi.rpsls/           this project
```

```sh
cd pitiupi && ant jar                          # 1. build Pitiupi (or NetBeans: Clean and Build)
cd ../pitiupi.rpsls && ant jar                 # 2. build the plugin -> dist/pitiupi.rpsls.jar
mkdir -p ../pitiupi/plugins                    # 3. create the plugins folder
cp dist/pitiupi.rpsls.jar ../pitiupi/plugins/  #    and copy the plugin (or: make install)
cd ../pitiupi && java -jar dist/pitiupi.jar    # 4. run FROM pitiupi/ (or NetBeans: Run)
```

Pitiupi looks for `plugins/` **relative to the folder it is started from**. NetBeans already runs from `pitiupi/`; in a terminal, run it from inside `pitiupi/` (double‑clicking `dist/pitiupi.jar` will not find the plugin). The console confirms it: `Discovered plugin package: pitiupi.rpsls.jar`.

For classmates who only want to play: `make kit` creates `dist/rpsls-plugin.zip` with `pitiupi.rpsls.jar`, `HOW-TO-INSTALL.txt` and `COMO-INSTALAR.txt`.

**Works with both Pitiupi plugin APIs.** The same `pitiupi.rpsls.jar` runs on all of them, and the project builds with `ant jar` on any of them:

| Pitiupi version | API | Private connection | Tested |
|---|---|---|---|
| `main` (current) | `createPlugin(ConnectionManager, GuiManager)` | buggy → the plugin reads the connection itself | ✔ |
| `release/facade.v2.0.0` | `createPlugin(Pitiupi)` | buggy → the plugin reads the connection itself | ✔ |
| `update/mpChat` | `createPlugin(Pitiupi)` | ok | ✔ |
| one player on `main`, the other on `update/mpChat` | | | ✔ |

Bug and workaround details: [PROTOCOL.md § 9](PROTOCOL.md#9-integration-with-pitiupi).

Plan B, for someone who cannot build Pitiupi: `make kit-full` creates `dist/pitiupi-com-rpsls.zip` (prebuilt Pitiupi + plugin + `jogar.sh`/`jogar.bat`).

## Playing on the same Wi‑Fi (different notebooks)

This is how the plugin is evaluated: each person on their own notebook, everyone on the same network, each with `pitiupi.rpsls.jar` in their own `pitiupi/plugins/`.

1. Everyone on the same Wi‑Fi, logging in with the **same port (8000)**.
2. Firewall: on Windows, the first time, allow Java on **private and public networks** (campus Wi‑Fi is usually "public"). On Linux with `ufw`: `sudo ufw allow 8000`.
3. After a few seconds the others show up in the **Online** list → click a name → **Jogos → Desafiar para RPSLS** ("Games → Challenge to RPSLS").
4. The other player gets "E aí, bora jogar...?" ("Let's play?") → **Bora!**
5. Countdown 3, 2, 1, Vai! ("Go!") → each player clicks a move (10 s, otherwise the game picks one at random).
6. Result screen with both moves, the rule ("Spock vaporiza Pedra" = "Spock vaporizes Rock"), the winner and the score.
7. **Jogar de novo?** ("Play again?") → **Yep** on both sides starts a new round; a **Nope** ends the match.

**If nobody shows up in the Online list** (the Wi‑Fi blocks multicast, which is the automatic discovery):
- one player opens **Jogos → RPSLS → Meus endereços IP** ("My IP addresses") and tells the IP;
- the other uses **Jogos → RPSLS → Desafiar por IP...** ("Challenge by IP") and types it (direct unicast TCP connection).

**If even the IP does not connect**, the Wi‑Fi isolates devices (*client isolation*, common on campus networks): connect the notebooks to a **phone hotspot**. Turning off VPN and stopping Docker (`make down`) also helps.

Messages the game shows in those cases:
| Situation | What appears |
|---|---|
| Wrong IP / isolating Wi‑Fi / firewall | "Não foi possível conectar em … Verifique se: …" (could not connect; check …) |
| The other player lacks the plugin | after 30 s: "Convite expirou … Ele tem o plugin RPSLS instalado?" (invitation expired; does he have the plugin?) |
| The other player is in another match | "Convite recusado (jogador ocupado em outra partida)" (declined: busy) |

## Testing alone with 2 players (containers)
```sh
make up       # starts "alice" (172.28.0.11) and "bob" (172.28.0.12) on the same bridge network
```
- Alice: http://localhost:6080/vnc.html → Connect → login (port 8000)
- Bob:   http://localhost:6081/vnc.html → Connect → login (port 8000)
- In one window, click the other player in the **Online** list → **Jogos → Desafiar para RPSLS**, and play as above.

The log (`make logs`) shows every protocol message sent (`->`) and received (`<-`).
After changing the code: `make && make up` (the .jar files are mounted as volumes). Stop: `make down`.

Capture the traffic (for the report):
```sh
docker compose -f docker/compose.yml exec alice tcpdump -i eth0 -w /tmp/rpsls.pcap tcp port 8000
```

## Running without Docker (1 instance)
```sh
make run      # installs into ../pitiupi/plugins/ and starts Pitiupi from ../pitiupi
```

## Credits
Move images: [Fluent Emoji](https://github.com/microsoft/fluentui-emoji) © Microsoft, MIT license — see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
