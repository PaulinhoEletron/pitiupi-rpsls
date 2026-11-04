# RPSLS — Rock, Paper, Scissors, Lizard, Spock for Pitiupi

*🇧🇷 Resumo em português no final da página.*

A peer‑to‑peer, 1‑vs‑1 game plugin for **[Pitiupi](https://git.alice.ufsj.edu.br/fls/pitiupi)**, the P2P networking platform developed by Prof. Flávio (UFSJ). Two players on the same local network challenge each other and play over a direct TCP connection, using a custom application‑layer protocol designed for this plugin.

![Invitation, move selection and result screens](assets/screenshots.png)

## Highlights

- **Custom application protocol (RPSLS/1.0)** over a unicast TCP connection: 7 message types, an explicit state machine and a deadline for every state, so a match never hangs.
- **Fair play without a server.** A commit–reveal scheme (SHA‑256 with a 128‑bit random nonce) makes sure neither player can see the other's move before committing to their own. Tampered reveals are detected and the match is terminated.
- **No result on the wire.** Both peers compute the outcome deterministically (`(a − b + 5) mod 5`) from the revealed moves.
- **Compatible with two host API versions.** Adapters support both of Pitiupi's plugin APIs with a single `.jar`. At load time the plugin also detects a host defect in private‑connection handling and works around it.
- **Concurrency‑safe design.** The protocol logic is UI‑independent and thread‑safe (network, Swing and timer threads), and the UI only consumes events.
- **Resilient on real networks.** Players can challenge each other by IP when the Wi‑Fi blocks multicast discovery, and connection failures produce clear diagnostics.
- **Tested.** 20 automated tests cover all 25 move combinations through a simulated link, cheating detection, rematches and timeouts. End‑to‑end matches were run between Docker containers on a virtual LAN, including mixed host versions.

**Stack:** Java 17+, Swing / Java2D, TCP sockets (through Pitiupi), `java.security` (SHA‑256, `SecureRandom`), Ant / NetBeans, Docker.

## Documentation

| | English | Português |
|---|---|---|
| Overview, build and usage | [README.md](../pitiupi.rpsls/README.md) | [README.pt-BR.md](../pitiupi.rpsls/README.pt-BR.md) |
| Protocol specification | [PROTOCOL.md](../pitiupi.rpsls/PROTOCOL.md) | [PROTOCOLO.md](../pitiupi.rpsls/PROTOCOLO.md) |
| End‑user install guide | [HOW-TO-INSTALL.txt](../pitiupi.rpsls/kit/HOW-TO-INSTALL.txt) | [COMO-INSTALAR.txt](../pitiupi.rpsls/kit/COMO-INSTALAR.txt) |

## Repository layout

This repository mirrors the layout of the Pitiupi repository: the plugin lives in `pitiupi.rpsls/`, next to where Pitiupi's other plugins live. There are no files at the root, so the plugin can be added to a Pitiupi checkout without conflicts.

```
pitiupi.rpsls/
├── src/pitiupi/rpsls/
│   ├── game/      protocol state machine, rules, commit–reveal, timeouts
│   ├── net/       protocol messages and binding to the TCP connection
│   ├── gui/       Swing UI (match window, menus, icons)
│   ├── host/      adapters for the two Pitiupi plugin APIs
│   └── plugin/    plugin entry point
├── test/          automated tests
├── docker/        two‑player test environment
└── kit/           end‑user install guides
```

## Build and run

The plugin project builds against the `pitiupi/` project next to it, so build it inside a Pitiupi checkout:

```sh
cd pitiupi.rpsls
ant jar                                   # -> dist/pitiupi.rpsls.jar
mkdir -p ../pitiupi/plugins
cp dist/pitiupi.rpsls.jar ../pitiupi/plugins/
cd ../pitiupi
java -Djava.net.preferIPv4Stack=true -jar dist/pitiupi.jar
```

In Pitiupi, log in (every player on the same port, e.g. 8000), then click another player in the **Online** list → **Jogos → Desafiar para RPSLS**. The game UI is in Portuguese, like Pitiupi itself.

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
git checkout rpsls/main -- pitiupi.rpsls
git commit -m "Add RPSLS plugin"
```

Either way, only `pitiupi.rpsls/` is added and no existing file is changed.

## Credits

- **Pitiupi** — Prof. Flávio, UFSJ ([repository](https://git.alice.ufsj.edu.br/fls/pitiupi)).
- **Move images** — [Fluent Emoji](https://github.com/microsoft/fluentui-emoji) © Microsoft, MIT license. See [THIRD-PARTY-NOTICES.md](../pitiupi.rpsls/THIRD-PARTY-NOTICES.md).
- **Plugin** — Paulo Felipe.

---

## 🇧🇷 Português

Plugin de jogo **1 contra 1, ponto a ponto**, para o **[Pitiupi](https://git.alice.ufsj.edu.br/fls/pitiupi)**, a plataforma P2P desenvolvida pelo Prof. Flávio (UFSJ). Dois jogadores na mesma rede local se desafiam e jogam por uma conexão TCP direta, com um protocolo de camada de aplicação próprio, o **RPSLS/1.0**.

- Jogo justo sem servidor: commit–reveal com SHA‑256 e nonce aleatório de 128 bits.
- Máquina de estados explícita, com prazo em todos os estados.
- Compatível com as duas versões da API de plugins do Pitiupi com um único `.jar`.
- Desafio por IP quando a rede bloqueia multicast.
- 20 testes automatizados.

Documentação em português: [README.pt-BR.md](../pitiupi.rpsls/README.pt-BR.md) e [PROTOCOLO.md](../pitiupi.rpsls/PROTOCOLO.md).

**Para adicionar o plugin a um repositório do Pitiupi**, use um dos dois blocos de comandos da seção *Adding the plugin to a Pitiupi repository* acima: o primeiro faz o merge com o histórico, e o segundo copia só a pasta. Nos dois casos, apenas a pasta `pitiupi.rpsls/` é adicionada.
