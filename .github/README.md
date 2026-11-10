# RPSLS — Rock, Paper, Scissors, Lizard, Spock for Pitiupi

*🇧🇷 Resumo em português no final da página.*

A peer‑to‑peer, 1‑vs‑1 game plugin for **[Pitiupi](https://github.com/GabrielFSSantos/pitiupi)**, the P2P networking platform developed by Prof. Flávio (UFSJ). Two players on the same local network challenge each other and play over a direct TCP connection, using a custom application‑layer protocol designed for this plugin.

![The game inside Pitiupi: invitation, move selection and result](assets/screenshots.png)

## Highlights

- **Custom application protocol (RPSLS/1.0)** over a unicast TCP connection: 7 message types, an explicit state machine and a deadline for every state, so a match never hangs.
- **Fair play without a server.** A commit–reveal scheme (SHA‑256 with a 128‑bit random nonce) makes sure neither player can see the other's move before committing to their own. Tampered reveals are detected and the match is terminated.
- **No result on the wire.** Both peers compute the outcome deterministically (`(a − b + 5) mod 5`) from the revealed moves.
- **Native host integration.** The game runs as a tab in Pitiupi's main window. Challenges start from the user list through peer actions, and the plugin uses the host's asynchronous private TCP connections.
- **Concurrency‑safe design.** The protocol logic is UI‑independent and thread‑safe (network, Swing and timer threads), and the UI only consumes events.
- **Resilient on real networks.** Players can challenge each other by IP when the Wi‑Fi blocks multicast discovery, and connection failures produce clear diagnostics.
- **Tested.** 20 automated tests cover all 25 move combinations through a simulated link, cheating detection, rematches and timeouts. End‑to‑end matches were run between Docker containers on a virtual LAN.

**Stack:** Java 17+, Swing / Java2D, TCP sockets (through Pitiupi), `java.security` (SHA‑256, `SecureRandom`), Ant / NetBeans, Docker.

## Documentation

| | English | Português |
|---|---|---|
| Overview, install and usage | [README.md](../plugins/rpsls/README.md) | [README.pt-BR.md](../plugins/rpsls/README.pt-BR.md) |
| Protocol specification | [PROTOCOL.md](../plugins/rpsls/PROTOCOL.md) | [PROTOCOLO.md](../plugins/rpsls/PROTOCOLO.md) |
| End‑user install guide | [HOW-TO-INSTALL.txt](../plugins/rpsls/kit/HOW-TO-INSTALL.txt) | [COMO-INSTALAR.txt](../plugins/rpsls/kit/COMO-INSTALAR.txt) |

## Repository layout

This repository mirrors the layout of the Pitiupi repository: the plugin lives in `plugins/rpsls/`, next to Pitiupi's own plugins (`plugins/chat`, `plugins/screencast`, …). There are no files at the root, so the plugin can be added to a Pitiupi checkout without conflicts.

```
plugins/rpsls/
├── src/pitiupi/rpsls/
│   ├── game/      protocol state machine, rules, commit–reveal, timeouts
│   ├── net/       protocol messages and binding to the TCP connection
│   ├── gui/       the game tab (Swing), icons
│   └── plugin/    plugin entry point (Pitiupi integration)
├── test/          automated tests
├── docker/        two‑player test environment
└── kit/           end‑user install guides
```

## Install and run

**Without compiling:** in Pitiupi, open **Configurações → Instalar JAR…**, choose `pitiupi.rpsls.jar` and restart.

**From source:** inside a Pitiupi clone, put the plugin in `plugins/rpsls`, add this entry to `plugins.registry.json` and use the official script:

```json
{ "id": "rpsls", "dir": "plugins/rpsls", "build": "ant", "jar": "dist/pitiupi.rpsls.jar", "menuCategory": "Jogos" }
```

```sh
./scripts/pitiupi.sh run --all          # Windows: scripts\pitiupi.cmd run --all
```

In Pitiupi, both players use the same port (e.g. 5000). Click the other player in **Usuários nesta rede** → **Pedra, Papel, Tesoura, Lagarto, Spock** → **Desafiar para…**. The game UI is in Portuguese, like Pitiupi itself.

## Adding the plugin to a Pitiupi repository

From a clone of the Pitiupi repository, either merge it with its history:

```sh
git remote add rpsls https://github.com/PaulinhoEletron/pitiupi-rpsls.git
git fetch rpsls
git merge rpsls/main --allow-unrelated-histories -m "Add RPSLS plugin"
git rm -r -q .github && git commit -m "Remove the plugin repository's GitHub page"   # optional
```

or copy only the plugin folder:

```sh
git remote add rpsls https://github.com/PaulinhoEletron/pitiupi-rpsls.git
git fetch rpsls
git checkout rpsls/main -- plugins/rpsls
git commit -m "Add RPSLS plugin"
```

Either way, only `plugins/rpsls/` is added and no existing file is changed. Then add the `rpsls` entry shown above to `plugins.registry.json` (`./scripts/pitiupi.sh discover` lists it as a module that is not in the registry yet).

## Credits

- **Pitiupi** — Prof. Flávio, UFSJ ([repository](https://github.com/GabrielFSSantos/pitiupi)).
- **Move images** — [Fluent Emoji](https://github.com/microsoft/fluentui-emoji) © Microsoft, MIT license. See [THIRD-PARTY-NOTICES.md](../plugins/rpsls/THIRD-PARTY-NOTICES.md).
- **Plugin** — Paulo Felipe.

---

## 🇧🇷 Português

Plugin de jogo **1 contra 1, ponto a ponto**, para o **[Pitiupi](https://github.com/GabrielFSSantos/pitiupi)**, a plataforma P2P desenvolvida pelo Prof. Flávio (UFSJ). Dois jogadores na mesma rede local se desafiam e jogam por uma conexão TCP direta, com um protocolo de camada de aplicação próprio, o **RPSLS/1.0**.

- Jogo justo sem servidor: commit–reveal com SHA‑256 e nonce aleatório de 128 bits.
- Máquina de estados explícita, com prazo em todos os estados.
- O jogo é uma aba na janela principal do Pitiupi; o desafio sai da lista de usuários.
- Desafio pelo IP quando a rede bloqueia multicast.
- 20 testes automatizados.

Documentação em português: [README.pt-BR.md](../plugins/rpsls/README.pt-BR.md) e [PROTOCOLO.md](../plugins/rpsls/PROTOCOLO.md).

**Instalar sem compilar:** no Pitiupi, **Configurações → Instalar JAR…** → `pitiupi.rpsls.jar` → reiniciar.

**Para adicionar o plugin a um repositório do Pitiupi**, use um dos dois blocos de comandos da seção *Adding the plugin to a Pitiupi repository* acima e depois acrescente a entrada `rpsls` no `plugins.registry.json`. Nos dois casos, apenas a pasta `plugins/rpsls/` é adicionada.
