<div align="center">

# Rastreado

**Acompanhe encomendas do AliExpress até a sua porta, no Brasil.**

[![CI](https://github.com/ArturoManzoli/rastreado/actions/workflows/ci.yml/badge.svg)](https://github.com/ArturoManzoli/rastreado/actions/workflows/ci.yml)
[![Licença: GPL v3](https://img.shields.io/badge/licen%C3%A7a-GPL--3.0-blue.svg)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/android-8.0%2B-3ddc84.svg)](#requisitos)

[English](README.md)

</div>

O Rastreado é um app Android para o problema de quem compra no AliExpress e espera no Brasil: a
encomenda sai da China com um código da Cainiao, ganha um código dos Correios quando chega, e as
duas metades do trajeto costumam aparecer em lugares diferentes. O Rastreado acompanha as duas em
um único histórico e toca um alarme de verdade quando a encomenda sai para entrega, para você estar
em casa na hora.

Funciona offline, não tem conta, não tem análise de uso nem anúncios, e conversa com um único
servidor.

## O que faz

- **As duas metades do trajeto** em um só histórico, do armazém na China até a transportadora
  brasileira, incluindo alfândega e o código nacional de rastreio
- **Leitura de código de barras** para pegar o código direto da etiqueta
- **Foto da tela do pedido**, com OCR no próprio aparelho para extrair o código de rastreio e o
  nome do produto
- **Consultas automáticas** no intervalo que você escolher, com opção de usar somente Wi-Fi
- **Notificações** a cada mudança de status, em um canal separado para problemas de entrega
- **Alarme em tela cheia** quando a encomenda sai para entrega, no volume de alarme mesmo com o
  telefone no silencioso, com parar e soneca
- **Português do Brasil e inglês**, alternáveis dentro do app, independente do idioma do sistema
- **Tema claro e escuro** seguindo o sistema, ou fixo em um dos dois, com as cores do papel de
  parede opcionais no Android 12+

## Capturas de tela

Ainda não feitas. Contribuições são bem-vindas, veja o [CONTRIBUTING.md](CONTRIBUTING.md).

## De onde vêm os dados

O Rastreado lê o rastreio público da Cainiao, o mesmo que o AliExpress mostra ao comprador. Para
uma encomenda destinada ao Brasil, esse rastreio repassa também os eventos dos Correios, e é isso
que torna a etapa nacional visível sem precisar de uma segunda fonte.

A API própria dos Correios não serve para um app como este: desde 2023 ela é restrita a quem tem
contrato e autoriza o **remetente** do objeto, não o destinatário. Os detalhes, incluindo o
vocabulário de eventos e como ele é normalizado, estão em [docs/TRACKING.md](docs/TRACKING.md).

O Rastreado não tem vínculo com AliExpress, Alibaba, Cainiao ou Correios.

## Privacidade

Tudo o que o app sabe fica no telefone, em um banco local e um arquivo de preferências. Não existe
servidor, conta nem telemetria. A única requisição que sai é a consulta de rastreio em si, que envia
um código para a Cainiao e nada mais. A leitura de código de barras e o OCR rodam no próprio
aparelho, via ML Kit, então nenhuma foto sai do telefone.

## Requisitos

- Android 8.0 (API 26) ou mais recente
- Uma câmera, para o código de barras e o OCR; sem ela o app funciona digitando o código

## Compilando

```bash
git clone https://github.com/ArturoManzoli/rastreado.git
cd rastreado
./gradlew assembleDebug          # APK em app/build/outputs/apk/debug/
./gradlew test                   # testes unitários
```

São necessários o JDK 21 e o SDK do Android com a plataforma 37; o Gradle baixa o resto. Indique o
SDK ao Gradle pela variável `ANDROID_HOME` ou por um `local.properties` com
`sdk.dir=/caminho/para/Android/Sdk`.

## Arquitetura

App Kotlin de módulo único: Jetpack Compose e Material 3 na interface, Room para armazenamento,
WorkManager para as consultas em segundo plano, ML Kit para a leitura, e uma camada de provedores
plugável para acrescentar outra fonte de rastreio sem mexer no resto. O passo a passo está em
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Contribuindo

Relatos de bug, traduções e novos provedores são todos bem-vindos. Comece pelo
[CONTRIBUTING.md](CONTRIBUTING.md); o projeto segue um
[Código de Conduta](CODE_OF_CONDUCT.md).

## Licença

[GPL-3.0-or-later](LICENSE). Copyright (C) 2026 Arturo Manzoli e contribuidores.

Os ícones inclusos vêm do [Material Design Icons](https://pictogrammers.com/library/mdi/) (Apache-2.0).
