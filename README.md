# PaymentSimulator

Aplicativo Android que simula um fluxo de pagamento, do catálogo ao comprovante. Todos os produtos, valores e cobranças são fictícios. O aplicativo não se conecta a um gateway real e não solicita nem armazena número de cartão, CVV ou senha.

## Objetivo

Demonstrar um fluxo financeiro em camadas, com regras de negócio isoladas da interface e do banco. O domínio valida valores, transições de status e idempotência. A interface observa o estado e não recalcula o pedido.

## Funcionalidades

- Catálogo fictício com nome, descrição e preço em reais: Café, Chá e Água.
- Carrinho com inclusão, aumento, redução e remoção de itens, além de subtotal e total.
- Checkout com resumo, quantidade e confirmação do pedido.
- Escolha entre PIX fictício e cartão fictício. O cartão exibe apenas dados mascarados e cenários predefinidos.
- Processamento e resultado: aprovado, recusado, falha de comunicação ou tempo esgotado com status desconhecido.
- Consulta novamente do status, sem tratar timeout como recusa.
- Comprovante com identificador, data e hora, valor, método, status e confirmação fictícia.
- Histórico local com filtro por status, detalhes da transação e abertura do comprovante.
- Tema claro e escuro no Material 3.

O carrinho existe só na memória do processo. O histórico de pagamentos concluídos ou já gravados permanece no banco local depois que o aplicativo é fechado.

## Tecnologias

| Item | Uso no projeto |
|---|---|
| Kotlin 2.2 | Linguagem |
| Android Gradle Plugin 9.4 | Build |
| Jetpack Compose e Material 3 | Interface |
| Navigation Compose 2.10 | Rotas |
| Coroutines e StateFlow | Concorrência e estado de tela |
| Koin 4.2 | Injeção de dependências |
| Room 2.8 | Histórico local |
| JUnit 4, MockK, Turbine e Robolectric | Testes |
| minSdk 24, compileSdk e targetSdk 37 | Plataforma |
| Java 11 | Alvo de compilação |

As versões ficam no catálogo `gradle/libs.versions.toml`.

## Arquitetura

O projeto tem três módulos. A dependência segue uma direção só: `app` usa `domain` e `data`; `data` usa `domain`; `domain` não conhece Android nem Room.

```text
app
├── interface Compose
├── ViewModels e UiState
└── módulo Koin de apresentação

domain
├── modelos, máquina de estados e casos de uso
└── contratos de repositório e da API

data
├── Room, mappers e repositórios
└── FakePaymentApi, implementação atual da API
```

Os casos de uso concentram as regras: carrinho vazio, valor não positivo, transição inválida, pagamento já aprovado e retry cego de status desconhecido. Os Composables recebem estado e eventos. Eles não chamam repositório nem calculam o total.

O valor monetário é um `Long` em centavos, na moeda BRL. Soma e multiplicação usam operações exatas. Na leitura do banco, o valor gravado precisa coincidir com a soma dos itens.

A troca da API fictícia por outro cliente fica restrita ao módulo `paymentApiModule`. Os casos de uso dependem do contrato `PaymentApi`, não da `FakePaymentApi`. Hoje o build de debug e o de release usam a mesma API fictícia.

## Estrutura de diretórios

```text
PaymentSimulator/
├── app/src/main/java/br/com/paymentsimulator/
│   ├── navigation/          rotas
│   ├── presentation/        ViewModels e UiState
│   └── ui/                  telas, tema e componentes
├── domain/src/main/kotlin/br/com/paymentsimulator/domain/
│   ├── model/               Money, Cart, Payment e erros
│   ├── policy/              máquina de estados
│   ├── usecase/             regras do fluxo
│   ├── repository/          contratos
│   ├── api/                 contrato da cobrança fictícia
│   └── simulation/          cenários selecionáveis
├── data/src/main/kotlin/br/com/paymentsimulator/data/
│   ├── local/               Room, DAO e catálogo
│   ├── remote/              FakePaymentApi
│   ├── repository/          implementações
│   ├── mapper/              entidade e DTO para o domínio
│   └── di/                  módulo de dados e da API
└── gradle/libs.versions.toml
```

## Fluxo de processamento

1. A home carrega o catálogo e permite incluir itens no carrinho.
2. O carrinho altera quantidades e exibe subtotal e total calculados no domínio.
3. O checkout confirma o pedido. Essa confirmação ainda não cria um pagamento, porque o método ainda não foi escolhido.
4. A tela de método grava o cenário da simulação, cria o pagamento pendente e o coloca em processamento.
5. A gravação em processamento envia a cobrança fictícia. O resultado remoto só é aplicado se a máquina de estados permitir a transição e se valor, método e chave coincidirem com o pagamento local.
6. A tela de processamento consulta o status. Quando há um desfecho, abre o resultado do mesmo identificador.
7. O comprovante e o histórico observam o Room por `Flow`. Essa observação não envia outra cobrança.

Cenários do cartão: aprovado, recusado, tempo esgotado, API indisponível, resposta duplicada e desconhecido. O PIX desta simulação usa o cenário aprovado.

Uma resposta de API indisponível mantém o pagamento em processamento e não abre cobrança. Uma resposta com valor diferente do pedido não aprova o pagamento local.

## Máquina de estados

| De | Para |
|---|---|
| `PENDING` | `PROCESSING` |
| `PROCESSING` | `APPROVED`, `DECLINED`, `FAILED` ou `UNKNOWN` |
| `UNKNOWN` | `APPROVED` ou `DECLINED` |

`APPROVED`, `DECLINED` e `FAILED` são terminais. Qualquer outra passagem devolve `InvalidStatusTransition` e o registro permanece como estava.

`UNKNOWN` não volta para `PROCESSING`. Por isso um timeout não é reprocessado automaticamente e não é exibido como recusa.

## Idempotência

Cada confirmação de método gera uma chave. A mesma chave, o mesmo carrinho e o mesmo método representam a mesma operação:

- criar de novo devolve o pagamento original, com o mesmo identificador;
- uma segunda gravação em processamento não abre outra cobrança se o pagamento já saiu de `PENDING`;
- a chave é única no Room. Outro identificador com a mesma chave não substitui o pagamento original;
- confirmar a tela duas vezes reutiliza a chave e o identificador;
- outra chave representa outra operação.

Gravações concorrentes do mesmo pagamento passam por um bloqueio no repositório e na API fictícia. O efeito observado é uma única cobrança, com o valor original.

## Timeout e consulta de status

Quando o envio termina por tempo esgotado, o pagamento gravado fica `UNKNOWN`. A consulta seguinte lê esse status. A tela informa que o resultado é desconhecido e oferece **Consultar status novamente**.

Essa consulta chama `GetPaymentStatusUseCase`. Ela não cria pagamento e não reenvia a cobrança. Se a cobrança fictícia não existir mais na memória da API, o registro local é mantido. Ele não é apagado nem recusado por suposição.

`RetryPaymentUseCase` recusa retry cego de `UNKNOWN` (`BlindRetryNotAllowed`) e também recusa retry de pagamento aprovado, recusado ou falho.

## Testes automatizados

A suíte usa JUnit. Onde o comportamento depende de fluxo ou de colaboração, os testes usam Turbine e MockK. O Room é exercitado com Robolectric e banco em memória.

Os testes cobrem:

- invariantes de dinheiro, carrinho e pagamento;
- as 36 combinações de status da máquina de estados;
- criação, processamento, retry, consulta e comprovante;
- idempotência, timeout e resposta duplicada da API fictícia;
- persistência e atualização de status no Room;
- concorrência de processamento, valor remoto divergente e preservação do pagamento quando a API não tem a cobrança;
- estados de carregamento, sucesso, vazio e erro dos ViewModels.

Execute, na raiz do projeto Gradle:

```bash
./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest assembleDebug
```

No Windows:

```bat
gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest assembleDebug
```

## Como executar

Requisitos: Android Studio com compileSdk 37, JDK 11 e um dispositivo ou emulador com API 24 ou superior.

1. Abra a pasta que contém `settings.gradle.kts`.
2. Sincronize o Gradle.
3. Execute a configuração `app`.
4. Para gerar o APK de debug: `./gradlew assembleDebug` ou `gradlew.bat assembleDebug`.

O fluxo na aplicação: inclua um produto, abra o carrinho, confirme o checkout, escolha PIX ou cartão fictício e acompanhe o resultado. No cartão, o cenário escolhido define a resposta da simulação.

## Capturas de tela

O repositório ainda não contém arquivos de imagem. As capturas abaixo devem ser feitas no emulador ou no dispositivo, a partir do aplicativo em execução. Sugestão de nomes, na ordem do fluxo:

| Arquivo | Tela |
|---|---|
| `docs/screenshots/01-home.png` | Catálogo, preço e quantidade no carrinho |
| `docs/screenshots/02-cart.png` | Itens, quantidades, subtotal e total |
| `docs/screenshots/03-checkout.png` | Resumo e confirmação do pedido |
| `docs/screenshots/04-payment-method.png` | PIX ou cartão fictício e cenário |
| `docs/screenshots/05-processing.png` | Indicador de processamento |
| `docs/screenshots/06-result.png` | Resultado, inclusive timeout sem aparecer como recusa |
| `docs/screenshots/07-receipt.png` | Comprovante |
| `docs/screenshots/08-transactions.png` | Histórico, filtro e detalhes |

No Android Studio: rode o `app`, abra o Logcat não é necessário, use o ícone de câmera do emulador ou **View > Tool Windows > Running Devices** e salve cada tela. Tema claro e tema escuro podem virar pares, por exemplo `01-home-dark.png`.

## Evolução possível

Estes itens não estão implementados:

- cliente HTTP no lugar da `FakePaymentApi`, usando o contrato que os casos de uso já consomem;
- persistência do carrinho e das cobranças fictícias entre encerramentos do processo;
- testes de interface no dispositivo. O teste instrumentado de exemplo do módulo `app` não cobre o fluxo de pagamento.
