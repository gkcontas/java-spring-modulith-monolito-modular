# Monólito Modular com Spring Modulith

Aplicação de pedidos dividida em cinco módulos que não se chamam: publicam e escutam eventos. Um deploy, um banco, e fronteiras entre os módulos verificadas por teste.

É o meio-termo entre o monólito sem divisões internas e os microsserviços — sem rede, sem serialização entre serviços e sem orquestração de infraestrutura, mas com o acoplamento controlado que torna possível extrair um módulo no dia em que isso fizer sentido.

## Os módulos

```
POST /orders
   │
   ▼
order        cria o pedido (PENDING) e publica OrderPlaced
   │
   ▼
inventory    reserva estoque  → StockReserved  |  sem saldo → StockRejected
   │
   ▼
payment      cobra            → PaymentSettled |  recusado  → PaymentFailed
   │
   ▼
fulfillment  traduz o desfecho em CONFIRMED ou REJECTED
notification registra cada passo
```

| Módulo | Responsabilidade | Depende de |
|---|---|---|
| `order` | Dono do pedido; publica `OrderPlaced` | — |
| `inventory` | Reserva de estoque | `order` |
| `payment` | Cobrança | `inventory` |
| `fulfillment` | Process manager: traduz os desfechos em mudança de status | `order`, `inventory`, `payment` |
| `notification` | Histórico do que aconteceu com cada pedido | `order`, `inventory`, `payment` |

Cada módulo é um pacote: o que está na raiz dele é a API pública, o que está em `internal` é privado. Entidades, repositórios e controllers ficam em `internal`; interfaces de serviço e eventos ficam na raiz.

O módulo `fulfillment` existe por um motivo estrutural. Se `order` escutasse os eventos de `inventory`, os dois dependeriam um do outro e a verificação acusaria ciclo. Concentrar a coordenação em um módulo à parte mantém o grafo acíclico, e é onde uma saga moraria num sistema maior.

## Tecnologias e bibliotecas

| | |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 |
| Modularização | Spring Modulith 1.4 (core, events-jpa, actuator, docs, test) |
| Persistência | Spring Data JPA, PostgreSQL 16 |
| Migrations | Flyway |
| Validação | Bean Validation |
| Build | Gradle Kotlin DSL (wrapper `gradlew`) |
| Testes | JUnit 5, AssertJ, Awaitility, Testcontainers |
| Apoio | Lombok nas entidades |

## Pré-requisitos

- JDK 21 ou superior
- Docker

## Como rodar

```bash
docker compose up -d
```

```bash
./gradlew bootRun
```

A API fica em `http://localhost:8080`. O Flyway cria o schema e cadastra cinco SKUs de exemplo.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/orders` | Cria um pedido e dispara o fluxo (responde 202) |
| `GET` | `/orders/{id}` | Situação atual de um pedido |
| `GET` | `/orders` | Todos os pedidos |
| `GET` | `/stock` | Saldo e reservas por SKU |
| `GET` | `/payments` | Histórico de cobranças |
| `GET` | `/notifications/{orderId}` | O que aconteceu com um pedido, passo a passo |
| `GET` | `/event-registry` | Todas as entregas de evento registradas |
| `GET` | `/event-registry/incomplete` | Entregas pendentes |
| `POST` | `/event-registry/resubmit` | Reenvia as entregas pendentes |
| `GET` | `/actuator/modulith` | Estrutura dos módulos em runtime |

A criação responde **202**, não 201: o pedido existe, e se ele será confirmado depende de listeners que ainda não rodaram.

## Exemplos de uso

Fluxo completo:

```bash
curl -s -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"sku":"KEYBOARD-01","quantity":2,"amount":450.00,"customerEmail":"ana@example.com"}'
```

```bash
curl -s localhost:8080/orders/<id>
```

```bash
curl -s localhost:8080/notifications/<id>
```

```
OrderPlaced      Order received for 2 x KEYBOARD-01
StockReserved    Stock reserved for 2 x KEYBOARD-01
PaymentSettled   Charged 450.00
```

Sem estoque — `MONITOR-03` tem 3 unidades:

```bash
curl -s -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"sku":"MONITOR-03","quantity":10,"amount":2500.00,"customerEmail":"bruno@example.com"}'
```

```
REJECTED | out of stock: only 3 available
```

Acima do limite de pagamento:

```bash
curl -s -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"sku":"LAPTOP-04","quantity":1,"amount":15000.00,"customerEmail":"carla@example.com"}'
```

```
REJECTED | payment failed: amount above the 10000.00 limit
```

## Entrega confiável entre módulos

Os listeners são assíncronos: rodam depois do commit de quem publicou, em transação própria. Isso evita que o estoque seja reservado para um pedido que deu rollback, e evita que um listener lento vire um endpoint lento.

O preço de entregar assim é que uma falha no listener perderia o evento — do mesmo jeito que um consumidor de fila sem DLQ. O **registro de publicação** do Spring Modulith resolve isso: cada entrega é gravada na tabela `event_publication` antes do listener rodar e marcada como concluída quando ele retorna. Uma entrega por listener, não por evento.

O SKU `BOOM-99` simula o gateway de pagamento fora do ar na primeira tentativa:

```bash
curl -s -X POST localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"sku":"BOOM-99","quantity":1,"amount":99.00,"customerEmail":"diego@example.com"}'
```

```bash
curl -s localhost:8080/event-registry
```

```
PENDENTE  StockReservedListener.on    StockReserved
ok        NotificationListener.on     StockReserved
ok        NotificationListener.on     OrderPlaced
ok        OrderPlacedListener.on      OrderPlaced
```

O pedido fica em `PENDING`: nada o recusou, o passo apenas não aconteceu. E só a entrega do `payment` está pendente — o `notification` processou o mesmo `StockReserved` normalmente.

```bash
curl -s -X POST localhost:8080/event-registry/resubmit
```

```json
{"resubmitted": 1, "listeners": ["...payment.internal.StockReservedListener.on(...StockReserved)"]}
```

O reenvio retoma o fluxo exatamente onde parou e o pedido chega a `CONFIRMED`. Como a entrega é repetida com o mesmo evento, o listener precisa ser idempotente — a mesma exigência de um consumidor de mensagens.

A distinção entre os dois tipos de falha é deliberada: cobrança acima do limite é um **desfecho de negócio**, vira evento e o fluxo conclui; gateway inalcançável é uma **falha técnica**, o listener lança e a entrega fica pendente para reenvio.

## Estrutura verificada

```bash
curl -s localhost:8080/actuator/modulith
```

```
fulfillment   -> inventory (EVENT_LISTENER), order (USES_COMPONENT), payment (EVENT_LISTENER)
inventory     -> order (EVENT_LISTENER)
notification  -> inventory, order, payment (EVENT_LISTENER)
order         -> (nenhum)
payment       -> inventory (EVENT_LISTENER)
```

Só uma dependência é chamada direta de componente — `fulfillment` usando a API de `order` para mudar o status. Todo o resto é evento.

A mesma informação é verificada no build por `ApplicationModules.verify()`, que falha quando um módulo referencia o pacote `internal` de outro ou quando aparece um ciclo.

## Documentação gerada do código

O build escreve diagramas e fichas de módulo em `build/spring-modulith-docs`:

```
components.puml            diagrama C4 dos cinco módulos e suas relações
module-<nome>.puml         diagrama de cada módulo
module-<nome>.adoc         ficha: pacote base, beans expostos, eventos publicados e escutados
all-docs.adoc              documento agregador
```

Trecho de `module-inventory.adoc`:

```
Base package      | com.gkcontas.modulith.inventory
Spring components | InventoryManagement (via internal.DefaultInventoryManagement)
Events listened to| OrderPlaced (async)
```

Documentação derivada do mesmo modelo que a verificação usa não tem como ficar desatualizada.

## Testes

```bash
./gradlew test
```

14 testes:

| Classe | Testes | O que cobre |
|---|---|---|
| `ModuleStructureTest` | 2 | `verify()` das fronteiras e geração da documentação |
| `ModuleVerificationBitesTest` | 1 | A mesma verificação apontada para código que a viola, exigindo falha |
| `InventoryModuleTest` | 3 | Um módulo sozinho, com `@ApplicationModuleTest` e `Scenario` |
| `OrderFlowIntegrationTest` | 5 | Fluxo completo: confirmação, recusas e efeito em cada módulo |
| `EventPublicationRegistryTest` | 3 | Entrega pendente, reenvio e registro das entregas bem-sucedidas |

O teste de módulo isolado sobe um contexto com apenas os beans de `inventory` — `order`, `payment`, `fulfillment` e `notification` não existem ali. Se o módulo tivesse adquirido uma dependência de algum deles, o contexto não subiria. É a contraparte em runtime da verificação estática: uma prova que os imports estão limpos, a outra que a fiação está.

Os testes de integração usam Testcontainers e precisam de Docker; os de estrutura, não.

## Decisões de projeto

- **Nenhuma chave estrangeira cruza módulo.** `payments.order_id` é um valor, não uma FK para `orders`. Uma referência entre as tabelas de dois módulos é o que torna impossível separá-los depois.
- **Eventos carregam tudo que o consumidor precisa.** `StockReserved` leva o valor da cobrança mesmo sem o estoque se importar com dinheiro: a alternativa é `payment` perguntar para `order`, o que acrescenta uma dependência no sentido contrário.
- **Entidades não viram evento.** Os eventos são records de valores simples — publicar a entidade JPA levaria associações preguiçosas e identidade para dentro da transação de cada listener, além de não serializar no registro de publicação.
- **Pool de tarefas dimensionado.** Os listeners são assíncronos, então `spring.task.execution.pool` é a largura de toda a parte orientada a eventos; no padrão de uma thread, cada fluxo viraria fila.
