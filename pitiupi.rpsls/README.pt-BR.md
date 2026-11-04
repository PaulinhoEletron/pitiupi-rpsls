# RPSLS — Pedra, Papel, Tesoura, Lagarto, Spock (plugin do Pitiupi)

🇺🇸 *English version: [README.md](README.md)*

**Jogo 1×1 em rede local** para o [Pitiupi](https://git.alice.ufsj.edu.br/fls/pitiupi), a ferramenta P2P do Prof. Flávio (UFSJ).
Trabalho da disciplina de Redes de Computadores (mestrado) — **Paulo Felipe**.

- Protocolo de aplicação próprio, **RPSLS/1.0**, sobre a **conexão TCP privada (unicast)** do Pitiupi: convite, rodada justa com **commit‑reveal (SHA‑256)**, revanche e prazos. Detalhes em [PROTOCOLO.md](PROTOCOLO.md) (🇺🇸 [PROTOCOL.md](PROTOCOL.md)).
- Tela de convite, contagem 3‑2‑1 animada, barra de tempo, ícones 3D das 5 jogadas, resultado lado a lado com a regra, placar, revanche Yep/Nope e tela de regras.

## Requisitos
- JDK 17+ (testado com 21) e `ant` (ou NetBeans). O `make` é opcional.
- Docker + Docker Compose, só para o teste local com 2 jogadores.

## Compilar
```sh
cd pitiupi.rpsls
ant jar       # ou: make  (compila o núcleo e o plugin -> dist/pitiupi.rpsls.jar)
make test     # testes: regras, commit-reveal e partidas simuladas
```

## Instalar no Pitiupi do professor

O plugin segue o mesmo esquema dos outros plugins do repositório (`pitiupi.chat`, `pitiupi.mpChat`): é um projeto NetBeans/ant na raiz do repositório e gera um `.jar` que vai em **`pitiupi/plugins/`**.

```
<repositório>/
├── pitiupi/                 o programa (projeto NetBeans)
│   ├── dist/pitiupi.jar
│   └── plugins/             CRIAR (não vem no git): os .jar dos plugins vão aqui
│       └── pitiupi.rpsls.jar
├── pitiupi.chat/
├── pitiupi.mpChat/
└── pitiupi.rpsls/           este projeto
```

```sh
cd pitiupi && ant jar                          # 1. compila o Pitiupi (ou NetBeans: Clean and Build)
cd ../pitiupi.rpsls && ant jar                 # 2. compila o plugin -> dist/pitiupi.rpsls.jar
mkdir -p ../pitiupi/plugins                    # 3. cria a pasta de plugins
cp dist/pitiupi.rpsls.jar ../pitiupi/plugins/  #    e copia o plugin (ou: make install)
cd ../pitiupi && java -jar dist/pitiupi.jar    # 4. roda A PARTIR de pitiupi/ (ou NetBeans: Run)
```

O Pitiupi procura `plugins/` **relativo à pasta de onde é executado**. O NetBeans já executa a partir de `pitiupi/`. No terminal, rode de dentro de `pitiupi/`; um duplo clique no `dist/pitiupi.jar` não acha o plugin. O console confirma: `Discovered plugin package: pitiupi.rpsls.jar`.

Para os colegas que só querem jogar: `make kit` gera `dist/rpsls-plugin.zip` com o `pitiupi.rpsls.jar`, o `COMO-INSTALAR.txt` e o `HOW-TO-INSTALL.txt`.

**Compatível com as duas APIs de plugin do Pitiupi.** O mesmo `pitiupi.rpsls.jar` funciona, e o projeto compila com `ant jar` em qualquer uma delas:

| Versão do Pitiupi | API | Conexão privada | Testado |
|---|---|---|---|
| `main` (atual) | `createPlugin(ConnectionManager, GuiManager)` | com bug → o plugin lê a conexão sozinho | ✔ |
| `release/facade.v2.0.0` | `createPlugin(Pitiupi)` | com bug → o plugin lê a conexão sozinho | ✔ |
| `update/mpChat` | `createPlugin(Pitiupi)` | ok | ✔ |
| um jogador em `main`, outro em `update/mpChat` | | | ✔ |

Detalhes do bug e do contorno: [PROTOCOLO.md § 9](PROTOCOLO.md#9-integração-com-o-pitiupi).

Plano B, para quem não conseguir compilar o Pitiupi: `make kit-completo` gera `dist/pitiupi-com-rpsls.zip` (Pitiupi já compilado + plugin + `jogar.sh`/`jogar.bat`).

## Jogar na mesma Wi‑Fi (notebooks diferentes)

É assim que o plugin é avaliado: cada pessoa no seu notebook, todos na mesma rede, cada um com o `pitiupi.rpsls.jar` em `pitiupi/plugins/` do seu Pitiupi.

1. Todos na mesma Wi‑Fi, login com a **mesma porta (8000)**.
2. Firewall: no Windows, ao abrir pela 1ª vez, permitir o Java em **redes privadas e públicas** (Wi‑Fi de faculdade costuma ser "pública"). No Linux com `ufw`: `sudo ufw allow 8000`.
3. Em alguns segundos os colegas aparecem na lista **Online** → clique no nome → **Jogos → Desafiar para RPSLS**.
4. O outro recebe "E aí, bora jogar...?" → **Bora!**
5. Contagem 3, 2, 1, Vai! → cada um clica numa jogada (10 s, senão o jogo escolhe por você).
6. Tela de resultado com as duas jogadas, a regra ("Spock vaporiza Pedra"), quem venceu e o placar.
7. **Jogar de novo?** → se os dois disserem **Yep**, nova rodada; um **Nope** encerra.

**Se não aparecer ninguém na lista Online** (a Wi‑Fi bloqueia multicast, que é a descoberta automática):
- um jogador abre **Jogos → RPSLS → Meus endereços IP** e fala o IP;
- o outro usa **Jogos → RPSLS → Desafiar por IP...** e digita esse IP (conexão TCP direta, unicast).

**Se nem pelo IP conectar**, a Wi‑Fi isola os aparelhos (*client isolation*, comum em redes de faculdade): liguem o **hotspot de um celular** e conectem os notebooks nele. Também ajuda desligar VPN e parar o Docker (`make down`).

Mensagens que o jogo mostra nesses casos:
| Situação | O que aparece |
|---|---|
| IP errado / Wi‑Fi isolando / firewall | "Não foi possível conectar em … Verifique se: …" |
| Colega sem o plugin RPSLS | depois de 30 s: "Convite expirou … Ele tem o plugin RPSLS instalado?" |
| Colega já está jogando | "Convite recusado (jogador ocupado em outra partida)" |

## Testar sozinho com 2 jogadores (containers)
```sh
make up       # sobe "alice" (172.28.0.11) e "bob" (172.28.0.12) na mesma rede bridge
```
- Alice: http://localhost:6080/vnc.html → Connect → login (porta 8000)
- Bob:   http://localhost:6081/vnc.html → Connect → login (porta 8000)
- Na janela de um deles, clique no outro na lista **Online** → **Jogos → Desafiar para RPSLS** e jogue como acima.

O log (`make logs`) mostra cada mensagem do protocolo enviada (`->`) e recebida (`<-`).
Depois de alterar o código: `make && make up` (os .jar são montados como volume). Parar: `make down`.

Capturar o tráfego (para o relatório):
```sh
docker compose -f docker/compose.yml exec alice tcpdump -i eth0 -w /tmp/rpsls.pcap tcp port 8000
```

## Rodar sem Docker (1 instância)
```sh
make run      # instala em ../pitiupi/plugins/ e abre o Pitiupi a partir de ../pitiupi
```

## Créditos
Imagens das jogadas: [Fluent Emoji](https://github.com/microsoft/fluentui-emoji) © Microsoft, licença MIT — ver [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
