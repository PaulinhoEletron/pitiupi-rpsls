# RPSLS — Pedra, Papel, Tesoura, Lagarto, Spock (plugin do Pitiupi)

🇺🇸 *English version: [README.md](README.md)*

Plugin de **jogo 1×1 em rede local** para o [Pitiupi](https://github.com/GabrielFSSantos/pitiupi), a plataforma P2P desenvolvida pelo Prof. Flávio (UFSJ). Autor: **Paulo Felipe**.

- Protocolo de aplicação próprio, **RPSLS/1.0**, sobre a **conexão TCP privada (unicast)** do Pitiupi: convite, rodada justa com **commit‑reveal (SHA‑256)**, revanche e prazos. Detalhes em [PROTOCOLO.md](PROTOCOLO.md) (🇺🇸 [PROTOCOL.md](PROTOCOL.md)).
- O jogo é uma **aba na janela principal do Pitiupi**. Ela tem lobby, tela de convite, contagem 3‑2‑1 animada, barra de tempo e ícones 3D das 5 jogadas. A tela de resultado mostra as duas jogadas lado a lado com a regra, o placar e a revanche Yep/Nope, e a tela de regras está sempre disponível.

## Requisitos
- O Pitiupi atual (a versão com `plugins.registry.json` e `scripts/pitiupi.sh`).
- JDK 17+ e Apache Ant, os mesmos do próprio Pitiupi.

## Começo rápido (testar localmente)

Precisa de Git, JDK 17+ e `make` (e Docker para o `make up`):

```sh
git clone https://github.com/GabrielFSSantos/pitiupi.git          # Pitiupi
git clone https://github.com/PaulinhoEletron/pitiupi-rpsls.git    # este plugin
cp -r pitiupi-rpsls/plugins/rpsls pitiupi/plugins/
cd pitiupi/plugins/rpsls

make run    # compila o Pitiupi + o plugin e abre o Pitiupi com o jogo instalado
make up     # ou: dois jogadores em Docker (veja "Desenvolvimento" abaixo)
```

## Instalar

### Opção 1 — só o `.jar` (sem compilar)

1. Abra o Pitiupi como de costume: `./scripts/pitiupi.sh run --all` (Linux/WSL) ou `scripts\pitiupi.cmd run --all` (Windows).
2. Vá na aba **Configurações** → **Instalar JAR…** → escolha o `pitiupi.rpsls.jar`.
3. Reinicie o Pitiupi quando ele pedir.

O Pitiupi copia o jar para a pasta de plugins e o registra no `plugins.registry.json`, então ele continua instalado nas próximas execuções. A aba **Pedra, Papel, Tesoura, Lagarto, Spock** aparece então na galeria **Plugins**.

### Opção 2 — pelo código, igual aos outros plugins

Coloque esta pasta em `plugins/rpsls` num clone do Pitiupi e acrescente esta entrada no `plugins.registry.json` da raiz:

```json
{
  "id": "rpsls",
  "dir": "plugins/rpsls",
  "build": "ant",
  "jar": "dist/pitiupi.rpsls.jar",
  "menuCategory": "Jogos"
}
```

Depois compile e rode tudo com o script oficial:

```sh
./scripts/pitiupi.sh run --all            # ou só este plugin: --plugins rpsls
```

O `./scripts/pitiupi.sh discover` também lista `plugins/rpsls` como módulo que ainda não está no catálogo. Para compilar só o plugin: `cd plugins/rpsls && ant jar` → `dist/pitiupi.rpsls.jar`.

## Jogar na mesma rede (dois computadores)

1. Os dois abrem o Pitiupi com **a mesma porta** (na sala, em geral `5000`) e nomes diferentes.
2. Em alguns segundos cada um aparece na lista **Usuários nesta rede** do outro.
3. Um deles clica no nome do outro → **Pedra, Papel, Tesoura, Lagarto, Spock** → **Desafiar para Pedra, Papel, Tesoura, Lagarto, Spock**.
4. A aba do jogo abre sozinha para o outro jogador com "*fulano* te desafiou! E aí, bora jogar…?" → **Bora!**
5. Contagem 3, 2, 1, Vai! → cada um clica numa jogada. São 10 s; senão o jogo escolhe uma aleatória.
6. A tela de resultado mostra as duas jogadas, a regra ("Spock vaporiza Pedra"), quem venceu e o placar.
7. **Jogar de novo?** → se os dois disserem **Yep**, nova rodada; um **Nope** encerra. **Voltar** retorna ao lobby.

**Desafiar pelo IP.** Se o outro jogador não aparecer na lista (algumas redes Wi‑Fi bloqueiam multicast, que é como o Pitiupi descobre os colegas), abra a aba do jogo pela galeria **Plugins**. O lobby mostra **o seu IP** e tem um campo para **desafiar pelo IP**, por conexão TCP direta.

**Se nem pelo IP conectar**, confira se os dois usam a mesma porta, se o firewall permite o Java (no Windows, em redes privadas *e* públicas) e se a Wi‑Fi não isola os aparelhos. O hotspot de um celular é um bom plano B.

| Situação | O que o jogo mostra |
|---|---|
| IP errado / firewall / Wi‑Fi isolando | "Não foi possível conectar em …" no lobby |
| O outro jogador não tem o plugin | depois de 30 s: "Convite expirou … Ele tem o plugin RPSLS instalado?" |
| O outro jogador já está numa partida | "Convite recusado (jogador ocupado em outra partida)" |

## Desenvolvimento

```sh
cd plugins/rpsls
ant jar       # ou: make           -> dist/pitiupi.rpsls.jar
make test     # 20 testes: regras, commit-reveal, partidas simuladas, prazos
make run      # copia o jar para ../../.pitiupi/plugins e abre o Pitiupi
```

**Dois jogadores numa máquina só (containers).** `make up` sobe "alice" (172.28.0.11) e "bob" (172.28.0.12) numa rede bridge do Docker:
- Alice: http://localhost:6080/vnc.html, Bob: http://localhost:6081/vnc.html → login na porta 5000 e jogue como acima.
- `make logs` mostra cada mensagem do protocolo enviada (`->`) e recebida (`<-`). Parar com `make down`.

Para capturar o tráfego para análise (por exemplo, no Wireshark):
```sh
docker compose -f docker/compose.yml exec alice tcpdump -i eth0 -w /tmp/rpsls.pcap tcp port 5000
```

## Créditos
- Pitiupi — Prof. Flávio, UFSJ ([GitHub](https://github.com/GabrielFSSantos/pitiupi)).
- Imagens das jogadas: [Fluent Emoji](https://github.com/microsoft/fluentui-emoji) © Microsoft, licença MIT — ver [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
