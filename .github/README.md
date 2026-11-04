# RPSLS — Rock, Paper, Scissors, Lizard, Spock (Pitiupi plugin)

🇧🇷 *Português abaixo / Portuguese below.*

A **1‑vs‑1 local‑network game** plugin for [Pitiupi](https://git.alice.ufsj.edu.br/fls/pitiupi), the P2P networking tool by Prof. Flávio (UFSJ).
Computer Networks course assignment (Master's) — **Paulo Felipe**.

- Custom application protocol **RPSLS/1.0** over Pitiupi's **private TCP connection (unicast)**: invitation, a fair round with **commit‑reveal (SHA‑256)**, rematch and timeouts.
- Works with both Pitiupi plugin APIs: the legacy one on `main` and the `Pitiupi` facade.
- Docs: [`pitiupi.rpsls/README.md`](../pitiupi.rpsls/README.md) (install and play) and [`pitiupi.rpsls/PROTOCOL.md`](../pitiupi.rpsls/PROTOCOL.md) (the protocol). Portuguese: [`README.pt-BR.md`](../pitiupi.rpsls/README.pt-BR.md), [`PROTOCOLO.md`](../pitiupi.rpsls/PROTOCOLO.md).
- The game screens are in Portuguese, like Pitiupi itself.

## Layout

This repository mirrors the Pitiupi repository layout. The plugin lives in `pitiupi.rpsls/`, next to where `pitiupi.chat/`, `pitiupi.mpChat/` etc. live:

```
pitiupi.rpsls/        the plugin project (NetBeans/ant)
.github/README.md     this page
```

There are no files at the root, so nothing conflicts with the `README.md` and `.gitignore` of the professor's repository.

## Merging into the Pitiupi repository (for the TAs)

Inside a clone of the Pitiupi repository:

**Option A — merge, keeping the plugin's history**
```sh
git remote add rpsls https://github.com/PaulinhoEletron/pitiupi-rpsls.git
git fetch rpsls
git merge rpsls/main --allow-unrelated-histories -m "Adiciona plugin RPSLS (Pedra, Papel, Tesoura, Lagarto, Spock)"
git rm -r --cached .github && rm -rf .github && git commit -m "Remove the plugin's GitHub page"   # optional
```

**Option B — copy just the folder, without history**
```sh
git remote add rpsls https://github.com/PaulinhoEletron/pitiupi-rpsls.git
git fetch rpsls
git checkout rpsls/main -- pitiupi.rpsls
git commit -m "Adiciona plugin RPSLS (Pedra, Papel, Tesoura, Lagarto, Spock)"
```

In both options no existing file is changed: only the `pitiupi.rpsls/` folder is added.

## Build and install

The plugin project depends on the `pitiupi/` project next to it (like the other plugins), so build it **inside the Pitiupi repository**:

```sh
cd pitiupi.rpsls
ant jar                                   # -> dist/pitiupi.rpsls.jar
mkdir -p ../pitiupi/plugins
cp dist/pitiupi.rpsls.jar ../pitiupi/plugins/
cd ../pitiupi
java -Djava.net.preferIPv4Stack=true -jar dist/pitiupi.jar
```

In Pitiupi: log in on port 8000, then click the other player in the **Online** list → **Jogos → Desafiar para RPSLS**.

## Credits

Move images: [Fluent Emoji](https://github.com/microsoft/fluentui-emoji) © Microsoft, MIT license. Details in [`pitiupi.rpsls/THIRD-PARTY-NOTICES.md`](../pitiupi.rpsls/THIRD-PARTY-NOTICES.md).

---

## 🇧🇷 Português

Plugin de jogo **1 contra 1 em rede local** para o Pitiupi (Prof. Flávio, UFSJ) — trabalho de Redes de Computadores (mestrado), **Paulo Felipe**. Protocolo próprio **RPSLS/1.0** sobre a conexão TCP privada (unicast) do Pitiupi, com commit‑reveal (SHA‑256), revanche e prazos. Documentação em português: [`README.pt-BR.md`](../pitiupi.rpsls/README.pt-BR.md) e [`PROTOCOLO.md`](../pitiupi.rpsls/PROTOCOLO.md).

**Para os monitores — juntar ao repositório do Pitiupi:** use a **Option A** (merge com histórico) ou a **Option B** (só copiar a pasta) acima. Nas duas, apenas a pasta `pitiupi.rpsls/` é adicionada e nenhum arquivo existente é alterado. Depois, para compilar e instalar: `cd pitiupi.rpsls && ant jar`, criar `pitiupi/plugins/` e copiar `dist/pitiupi.rpsls.jar` para lá; o Pitiupi deve ser executado a partir da pasta `pitiupi/`.
