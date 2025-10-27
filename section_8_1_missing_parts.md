# Seção 8.1 - Partes Faltantes (OrderEntity)

## LocalDateTime - Comparação Completa com Outros Tipos de Data

### Contexto no OrderEntity

```java
@Column(name = "created_at", nullable = false)
private LocalDateTime createdAt;
```

Vamos entender profundamente esta escolha e suas alternativas.

---

### 1. LocalDateTime (usado no projeto)

**O que é:**
- Representa data e hora **sem timezone**
- Formato: `2024-01-15T10:30:00`
- Parte do pacote `java.time` (Java 8+)

**Estrutura interna:**
```java
LocalDateTime now = LocalDateTime.now();
// Armazena:
// - year: 2024
// - month: 1 (JANUARY)
// - dayOfMonth: 15
// - hour: 10
// - minute: 30
// - second: 0
// - nanoOfSecond: 0
```

**Mapeamento SQL:**
```sql
-- PostgreSQL
created_at TIMESTAMP WITHOUT TIME ZONE

-- MySQL
created_at DATETIME

-- H2 (usado no projeto)
created_at TIMESTAMP

-- Valor armazenado
2024-01-15 10:30:00.000000
```

**Quando usar:**
- ✅ Aplicação e banco no mesmo timezone
- ✅ Data/hora sem importância de fuso horário
- ✅ Timestamps internos (criação, atualização de registros)
- ✅ Exemplos: horário de abertura de loja, data de nascimento com hora

**⚠️ Problema crítico do LocalDateTime:**

```java
// Cenário: Sistema distribuído

// Servidor no Brasil (UTC-3) cria pedido às 10h local
LocalDateTime createdAt = LocalDateTime.now(); 
// Valor: 2024-01-15T10:00:00

// Banco salva: 2024-01-15 10:00:00
// ❌ SEM informação de timezone!

// Cliente no Japão visualiza
// Vê: 2024-01-15 10:00:00
// Interpreta como: 10h da manhã no Japão
// ❌ MAS era 10h da manhã no Brasil!

// Real no Japão seria: 22h da noite (dia anterior)
// Diferença de 12 horas de confusão!
```

**Operações com LocalDateTime:**
```java
// Criar
LocalDateTime now = LocalDateTime.now();
LocalDateTime specific = LocalDateTime.of(2024, 1, 15, 10, 30);
LocalDateTime parsed = LocalDateTime.parse("2024-01-15T10:30:00");

// Manipular (retorna novo objeto - imutável)
LocalDateTime tomorrow = now.plusDays(1);
LocalDateTime nextWeek = now.plusWeeks(1);
LocalDateTime nextMonth = now.plusMonths(1);
LocalDateTime nextYear = now.plusYears(1);
LocalDateTime nextHour = now.plusHours(1);
LocalDateTime next30Min = now.plusMinutes(30);

// Subtrair
LocalDateTime yesterday = now.minusDays(1);
LocalDateTime lastMonth = now.minusMonths(1);

// Comparar
boolean isBefore = now.isBefore(tomorrow);  // true
boolean isAfter = now.isAfter(yesterday);   // true
boolean isEqual = now.isEqual(now);         // true

// ❌ NÃO use equals() para comparar valores
now.equals(tomorrow);  // Compara também nanos!
// ✅ Use isEqual()
now.isEqual(tomorrow); // Compara apenas o momento

// Extrair componentes
int year = now.getYear();           // 2024
int month = now.getMonthValue();    // 1
int day = now.getDayOfMonth();      // 15
int hour = now.getHour();           // 10
int minute = now.getMinute();       // 30
DayOfWeek dayOfWeek = now.getDayOfWeek(); // MONDAY
```

---

### 2. Instant (recomendado para sistemas distribuídos)

**O que é:**
- Representa um ponto no tempo **em UTC**
- Timestamp universal, sem ambiguidade
- Formato: `2024-01-15T13:00:00Z` (Z = Zulu = UTC)

**Por que usar:**
- ✅ **Sempre UTC** - sem ambiguidade de timezone
- ✅ Perfeito para sistemas distribuídos
- ✅ Logs de aplicação
- ✅ Auditoria
- ✅ Eventos entre microserviços
- ✅ Ordenação temporal global

**Mapeamento SQL:**
```sql
-- PostgreSQL
created_at TIMESTAMP WITH TIME ZONE

-- MySQL (armazena como UTC internamente)
created_at TIMESTAMP

-- Valor sempre em UTC
2024-01-15 13:00:00+00
```

**Exemplo prático - Problema resolvido:**

```java
// Servidor no Brasil (UTC-3) às 10h local
Instant createdAt = Instant.now();
// Valor salvo: 2024-01-15T13:00:00Z (UTC)
// ✅ Sempre UTC no banco!

// Cliente no Brasil visualiza
ZonedDateTime brazilTime = createdAt.atZone(ZoneId.of("America/Sao_Paulo"));
// Resultado: 2024-01-15T10:00:00-03:00
// ✅ Mostra 10h da manhã (correto!)

// Cliente no Japão visualiza
ZonedDateTime japanTime = createdAt.atZone(ZoneId.of("Asia/Tokyo"));
// Resultado: 2024-01-15T22:00:00+09:00
// ✅ Mostra 22h da noite (correto!)

// Cliente nos EUA visualiza
ZonedDateTime usTime = createdAt.atZone(ZoneId.of("America/New_York"));
// Resultado: 2024-01-15T08:00:00-05:00
// ✅ Mostra 8h da manhã (correto!)

// Todos veem o MESMO momento, em seu timezone local!
```

**Operações com Instant:**
```java
// Criar
Instant now = Instant.now();
Instant specific = Instant.ofEpochSecond(1705327200);
Instant parsed = Instant.parse("2024-01-15T13:00:00Z");

// Manipular (usando Duration)
Instant oneHourLater = now.plus(Duration.ofHours(1));
Instant oneDayLater = now.plus(Duration.ofDays(1));
Instant oneWeekLater = now.plus(Duration.ofDays(7));

// Comparar
boolean isBefore = now.isBefore(oneHourLater);  // true
long secondsBetween = Duration.between(now, oneHourLater).getSeconds(); // 3600

// Converter para epoch (milissegundos desde 1970-01-01T00:00:00Z)
long epochMilli = now.toEpochMilli();
long epochSecond = now.getEpochSecond();

// Criar de epoch
Instant fromEpoch = Instant.ofEpochMilli(1705327200000L);
```

**Como usar no JPA:**
```java
@Entity
public class OrderEntity {
    @Id
    private UUID id;
    
    // ✅ Use Instant para timestamps globais
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

---

### 3. ZonedDateTime (mais completo)

**O que é:**
- Data, hora **E timezone** juntos
- Mais completo, mas mais pesado
- Formato: `2024-01-15T14:00:00-03:00[America/Sao_Paulo]`

**Quando usar:**
- ✅ Agendamentos futuros (reuniões, eventos)
- ✅ Quando timezone faz parte do dado de negócio
- ✅ "Reunião às 14h horário de Brasília"
- ✅ Compromissos que devem manter timezone original

**Mapeamento SQL:**
```sql
-- PostgreSQL (suporta timezone completo)
scheduled_at TIMESTAMP WITH TIME ZONE

-- MySQL (converte para UTC, perde timezone original)
scheduled_at TIMESTAMP

-- Valor armazenado
2024-01-15 14:00:00-03:00
```

**Exemplo prático:**

```java
// Agendar reunião para 14h em São Paulo
ZonedDateTime meeting = ZonedDateTime.of(
    2024, 1, 15,  // Data
    14, 0, 0, 0,  // Hora: 14:00:00
    ZoneId.of("America/Sao_Paulo")  // Timezone
);
// Valor: 2024-01-15T14:00:00-03:00[America/Sao_Paulo]

// Participante em Nova York - qual horário para ele?
ZonedDateTime nyTime = meeting.withZoneSameInstant(ZoneId.of("America/New_York"));
// Resultado: 2024-01-15T12:00:00-05:00[America/New_York]
// ✅ 12h em NY = 14h em SP

// Participante em Tóquio
ZonedDateTime tokyoTime = meeting.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
// Resultado: 2024-01-16T02:00:00+09:00[Asia/Tokyo]
// ✅ 2h da manhã do DIA SEGUINTE em Tóquio!

// Horário de verão
// ZonedDateTime ajusta automaticamente para DST (Daylight Saving Time)
```

**Operações:**
```java
// Criar
ZonedDateTime now = ZonedDateTime.now();
ZonedDateTime inBrazil = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));

// Manipular
ZonedDateTime tomorrow = now.plusDays(1);

// Mudar timezone mantendo o MESMO INSTANTE (muda hora local)
ZonedDateTime sameInstantInNY = now.withZoneSameInstant(ZoneId.of("America/New_York"));

// Mudar timezone mantendo HORA LOCAL (muda o instante!)
ZonedDateTime sameLocalInNY = now.withZoneSameLocal(ZoneId.of("America/New_York"));

// Converter para Instant (UTC)
Instant instant = now.toInstant();
```

---

### 4. LocalDate (apenas data)

**O que é:**
- Apenas data (ano, mês, dia)
- Sem hora, sem timezone
- Formato: `2024-01-15`

**Quando usar:**
- ✅ Datas sem hora (aniversários, vencimentos)
- ✅ Data de nascimento
- ✅ Validade de documentos

**Mapeamento SQL:**
```sql
birth_date DATE
-- Valor: 2024-01-15
```

**Operações:**
```java
LocalDate today = LocalDate.now();
LocalDate birthDate = LocalDate.of(1990, 5, 15);

// Calcular idade
Period age = Period.between(birthDate, LocalDate.now());
int years = age.getYears();  // 33 (em 2024)

// Verificar dia da semana
DayOfWeek dayOfWeek = today.getDayOfWeek();  // MONDAY

// Verificar ano bissexto
boolean isLeapYear = today.isLeapYear();
```

---

### 5. LocalTime (apenas hora)

**O que é:**
- Apenas hora (hora, minuto, segundo)
- Sem data, sem timezone
- Formato: `09:00:00`

**Quando usar:**
- ✅ Horários de funcionamento
- ✅ Durações diárias

**Mapeamento SQL:**
```sql
opening_time TIME
-- Valor: 09:00:00
```

---

### Tabela Comparativa Completa

| Tipo | Componentes | Timezone | Tamanho DB | Quando usar | Mapeamento SQL |
|------|-------------|----------|------------|-------------|----------------|
| **LocalDateTime** | Data + Hora | ❌ Não | 8 bytes | App local, mesmo timezone | TIMESTAMP |
| **Instant** | Timestamp UTC | ✅ UTC | 12 bytes | Sistemas distribuídos, logs | TIMESTAMP |
| **ZonedDateTime** | Data + Hora + TZ | ✅ Sim | 16 bytes | Agendamentos, eventos futuros | TIMESTAMP WITH TZ |
| **LocalDate** | Data | ❌ Não | 4 bytes | Aniversários, vencimentos | DATE |
| **LocalTime** | Hora | ❌ Não | 4 bytes | Horário de abertura | TIME |

---

### Conversões Entre Tipos

```java
// ========== LocalDateTime ↔ Instant ==========

// LocalDateTime → Instant (precisa de timezone)
LocalDateTime local = LocalDateTime.now();
Instant instant = local.atZone(ZoneId.systemDefault()).toInstant();

// Instant → LocalDateTime (converte para timezone local)
Instant instant = Instant.now();
LocalDateTime local = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

// ========== Instant ↔ ZonedDateTime ==========

// Instant → ZonedDateTime
Instant instant = Instant.now();
ZonedDateTime zoned = instant.atZone(ZoneId.of("America/Sao_Paulo"));

// ZonedDateTime → Instant
ZonedDateTime zoned = ZonedDateTime.now();
Instant instant = zoned.toInstant();

// ========== LocalDateTime ↔ ZonedDateTime ==========

// LocalDateTime → ZonedDateTime (assume timezone)
LocalDateTime local = LocalDateTime.now();
ZonedDateTime zoned = local.atZone(ZoneId.systemDefault());

// ZonedDateTime → LocalDateTime (perde timezone!)
ZonedDateTime zoned = ZonedDateTime.now();
LocalDateTime local = zoned.toLocalDateTime();

// ========== LocalDate + LocalTime ↔ LocalDateTime ==========

// Combinar
LocalDate date = LocalDate.of(2024, 1, 15);
LocalTime time = LocalTime.of(10, 30);
LocalDateTime dateTime = LocalDateTime.of(date, time);

// Separar
LocalDateTime dateTime = LocalDateTime.now();
LocalDate date = dateTime.toLocalDate();
LocalTime time = dateTime.toLocalTime();

// ========== String ↔ Tipos ==========

// Parse
LocalDateTime parsed = LocalDateTime.parse("2024-01-15T10:30:00");
Instant instantParsed = Instant.parse("2024-01-15T13:00:00Z");
ZonedDateTime zonedParsed = ZonedDateTime.parse("2024-01-15T10:00:00-03:00[America/Sao_Paulo]");

// Format
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
String formatted = LocalDateTime.now().format(formatter);
// Resultado: "15/01/2024 10:30:00"

// ========== Epoch (milissegundos) ↔ Instant ==========

// Epoch → Instant
long epochMilli = System.currentTimeMillis();
Instant instant = Instant.ofEpochMilli(epochMilli);

// Instant → Epoch
Instant instant = Instant.now();
long epochMilli = instant.toEpochMilli();
```

---

### Recomendação para o Projeto

**Nosso caso (OrderEntity):**
```java
@Column(name = "created_at", nullable = false)
private LocalDateTime createdAt;
```

**Por que LocalDateTime foi escolhido:**
- ✅ Aplicação simples (não é sistema global ainda)
- ✅ Banco e aplicação no mesmo servidor/timezone
- ✅ Timestamp interno (não exposto diretamente ao usuário)
- ✅ Mais simples que Instant/ZonedDateTime

**Quando migrar para Instant:**
```java
// Se aplicação crescer para múltiplos países
@Column(name = "created_at", nullable = false)
private Instant createdAt;

// Na apresentação, converte para timezone do usuário
ZonedDateTime userTime = createdAt.atZone(userZoneId);
```

---

## Métodos de Conversão (from() e toDomain())

### Contexto

OrderEntity precisa converter entre:
- **Order** (domínio - lógica de negócio)
- **OrderEntity** (JPA - persistência)

```
Order (domain) ←→ OrderEntity (JPA) ←→ Database
```

---

### Método from() - Domain → Entity

```java
public static OrderEntity from(Order order) {
    return new OrderEntity(
        order.getId(),
        order.getClientId(),
        order.getTotal(),
        order.getStatus(),
        order.getCreatedAt()
    );
}
```

**Explicação completa:**

**1. Por que `static`?**
- Método de classe (não precisa de instância)
- Chamado como: `OrderEntity.from(order)`
- **Factory Method pattern**

**2. Por que Factory Method e não construtor direto?**

❌ **Sem factory method (problemático):**
```java
// Em múltiplos lugares do código
OrderEntity entity1 = new OrderEntity(
    order.getId(),
    order.getClientId(),
    order.getTotal(),
    order.getStatus(),
    order.getCreatedAt()
);

// Se Order adicionar campo, precisa mudar TODOS os lugares!
// Se Order mudar estrutura, quebra em vários pontos!
```

✅ **Com factory method (correto):**
```java
// Em qualquer lugar
OrderEntity entity = OrderEntity.from(order);

// Mudança centralizada! Altera apenas o método from()
```

**3. O que acontece internamente:**

```java
// Passo 1: Chama getters do domain
UUID id = order.getId();               // getter
String clientId = order.getClientId(); // getter
BigDecimal total = order.getTotal();   // getter
OrderStatus status = order.getStatus(); // getter
LocalDateTime createdAt = order.getCreatedAt(); // getter

// Passo 2: Usa construtor gerado por @AllArgsConstructor
OrderEntity entity = new OrderEntity(id, clientId, total, status, createdAt);

// Passo 3: Retorna instância pronta
return entity;
```

**4. Vantagens:**
- ✅ **Centralização:** Lógica de conversão em um único lugar
- ✅ **Manutenibilidade:** Mudança em um ponto afeta todos
- ✅ **Testabilidade:** Pode testar conversão isoladamente
- ✅ **Clareza:** Nome descritivo (from indica origem)
- ✅ **Flexibilidade:** Pode adicionar lógica sem quebrar código

**5. Exemplo com lógica adicional:**

```java
public static OrderEntity from(Order order) {
    // Validação
    if (order == null) {
        throw new IllegalArgumentException("Order cannot be null");
    }
    
    // Log (se necessário)
    log.debug("Converting Order {} to Entity", order.getId());
    
    // Transformação (se necessário)
    BigDecimal normalizedTotal = order.getTotal()
        .setScale(2, RoundingMode.HALF_UP); // Garante 2 casas decimais
    
    return new OrderEntity(
        order.getId(),
        order.getClientId(),
        normalizedTotal,  // Valor normalizado
        order.getStatus(),
        order.getCreatedAt()
    );
}
```

**6. Alternativas de nomes (todas válidas):**
```java
// Usado no projeto
public static OrderEntity from(Order order)

// Outras convenções comuns
public static OrderEntity of(Order order)
public static OrderEntity fromDomain(Order order)
public static OrderEntity toEntity(Order order)
public static OrderEntity fromOrder(Order order)
```

---

### Método toDomain() - Entity → Domain

```java
public Order toDomain() {
    return new Order(
        this.id,
        this.clientId,
        this.total,
        this.status,
        this.createdAt
    );
}
```

**Explicação completa:**

**1. Por que método de instância (não static)?**
```java
// Você já tem uma entity carregada do banco
OrderEntity entity = jpaRepository.findById(id).get();

// Quer converter para domínio
Order order = entity.toDomain();  // Método de instância
```

**2. Diferença entre from() e toDomain():**

| Aspecto | from() | toDomain() |
|---------|--------|------------|
| **Tipo** | static | instância |
| **Direção** | Domain → Entity | Entity → Domain |
| **Chamada** | `OrderEntity.from(order)` | `entity.toDomain()` |
| **Quando** | Antes de salvar | Depois de carregar |
| **Parâmetro** | Recebe Order | Usa `this` |
| **Retorno** | OrderEntity | Order |

**3. Fluxo bidirecional:**

```
       from()                    toDomain()
Order --------> OrderEntity ----------------> Order
(domain)        (JPA)                         (domain)
   ↓                ↓                            ↑
[Service]      [Database]                   [Service]
```

**4. O que acontece internamente:**

```java
// Passo 1: Acessa campos da entity atual (this)
UUID id = this.id;
String clientId = this.clientId;
BigDecimal total = this.total;
OrderStatus status = this.status;
LocalDateTime createdAt = this.createdAt;

// Passo 2: Chama construtor de Order
Order order = new Order(id, clientId, total, status, createdAt);

// Passo 3: Retorna Order (domínio)
return order;
```

**5. Por que `this` é opcional mas recomendado:**

```java
// Com this (mais explícito)
return new Order(
    this.id,
    this.clientId,
    this.total,
    this.status,
    this.createdAt
);

// Sem this (mais conciso)
return new Order(
    id,
    clientId,
    total,
    status,
    createdAt
);

// Ambos funcionam, mas `this` deixa claro que são campos da instância
```

**6. Ciclo completo de uso:**

```java
// 1. Service cria Order (domain)
Order newOrder = Order.create("client-123", BigDecimal.valueOf(100));

// 2. Adapter converte Domain → Entity
OrderEntity entity = OrderEntity.from(newOrder);  // from()

// 3. JPA salva no banco
OrderEntity saved = jpaRepository.save(entity);

// 4. Adapter converte Entity → Domain
Order savedOrder = saved.toDomain();  // toDomain()

// 5. Service recebe Order (domain)
return savedOrder;
```

**7. Exemplo com Optional (buscar por ID):**

```java
@Override
public Optional<Order> findById(String id) {
    return jpaRepository.findById(UUID.fromString(id))
        .map(OrderEntity::toDomain);  // Method reference!
}

// Equivalente a:
// .map(entity -> entity.toDomain())
```

---

### Mapeamento Visual dos Campos

```
Order (Domain)              OrderEntity (JPA)
┌────────────────────┐     ┌────────────────────┐
│ id: UUID           │────→│ id: UUID           │
│ clientId: String   │────→│ clientId: String   │
│ total: BigDecimal  │────→│ total: BigDecimal  │
│ status: OrderStatus│────→│ status: OrderStatus│
│ createdAt: LDT     │────→│ createdAt: LDT     │
└────────────────────┘     └────────────────────┘
        ↑                           │
        │        toDomain()         │
        └───────────────────────────┘

LDT = LocalDateTime
```

**Mapeamento 1:1:**
- Todos os campos têm **mesmos nomes e tipos**
- Nenhuma transformação necessária
- **Shallow copy** suficiente (objetos são imutáveis)

---

### Quando o Mapeamento NÃO é 1:1

**Exemplo 1: Campo calculado**
```java
// Order tem método calculado
public BigDecimal getTotalWithTax() {
    return total.multiply(BigDecimal.valueOf(1.1));
}

// Entity persiste valor calculado
public static OrderEntity from(Order order) {
    return new OrderEntity(
        order.getId(),
        order.getClientId(),
        order.getTotalWithTax(),  // ← Calcula antes de salvar
        order.getStatus(),
        order.getCreatedAt()
    );
}
```

**Exemplo 2: Relacionamento (ID vs Objeto)**
```java
// Order tem objeto completo
public class Order {
    UUID id;
    Customer customer;  // Objeto completo
}

// Entity tem apenas ID
public class OrderEntity {
    UUID id;
    UUID customerId;  // Apenas ID (FK)
}

// Conversão extrai ID
public static OrderEntity from(Order order) {
    return new OrderEntity(
        order.getId(),
        order.getCustomer().getId(),  // ← Navega para pegar ID
        // ...
    );
}
```

---

## Diagramas Visuais do Fluxo de Dados

### Fluxo Completo: Criar Pedido (POST)

```
┌──────────────────────────────────────────────────────────┐
│ 10. OrderService (retorna Order)                         │
│     Retorna Order para o controller                      │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 11. OrderController (conversão para DTO)                 │
│     OrderResponse response = OrderResponse.from(order);  │
│                                                          │
│     Conversão Domain → DTO:                              │
│     order.getId().toString() → response.id              │
│     order.getClientId() → response.clientId             │
│     order.getTotal() → response.total                   │
│     order.getStatus().name() → response.status          │
│                                                          │
│     return ResponseEntity.status(201).body(response);    │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 12. HTTP Response                                        │
│     HTTP/1.1 201 Created                                 │
│     Content-Type: application/json                       │
│                                                          │
│     {                                                    │
│       "id": "550e8400-e29b-41d4-a716-446655440000",    │
│       "clientId": "client-123",                         │
│       "total": 150.50,                                  │
│       "status": "PENDING"                               │
│     }                                                    │
└──────────────────────────────────────────────────────────┘
```

---

### Fluxo de Conversão: Domain ↔ Entity ↔ Database

```
┌─────────────────────────────────────────────────────────────────┐
│                   DOMAIN LAYER (Lógica de Negócio)              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Order (Imutável - @Value)                                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ - id: UUID                                                │  │
│  │ - clientId: String                                        │  │
│  │ - total: BigDecimal                                       │  │
│  │ - status: OrderStatus                                     │  │
│  │ - createdAt: LocalDateTime                                │  │
│  │                                                            │  │
│  │ + create(clientId, total): Order                          │  │
│  │ + confirm(): Order                                        │  │
│  └──────────────────────────────────────────────────────────┘  │
│                           ↓ from()                              │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│              ADAPTER LAYER (Conversão Domain ↔ Entity)          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  OrderEntity (Mutável - @Data)                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ @Entity                                                   │  │
│  │ @Table(name = "orders")                                   │  │
│  │                                                            │  │
│  │ @Id                                                       │  │
│  │ private UUID id;                                          │  │
│  │                                                            │  │
│  │ @Column(name = "client_id", nullable = false)            │  │
│  │ private String clientId;                                  │  │
│  │                                                            │  │
│  │ @Column(name = "total", nullable = false)                │  │
│  │ private BigDecimal total;                                 │  │
│  │                                                            │  │
│  │ @Enumerated(EnumType.STRING)                              │  │
│  │ @Column(name = "status", nullable = false)               │  │
│  │ private OrderStatus status;                               │  │
│  │                                                            │  │
│  │ @Column(name = "created_at", nullable = false)           │  │
│  │ private LocalDateTime createdAt;                          │  │
│  │                                                            │  │
│  │ + from(Order): OrderEntity                                │  │
│  │ + toDomain(): Order                                       │  │
│  └──────────────────────────────────────────────────────────┘  │
│                           ↓ JPA                                 │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE (H2)                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Table: orders                                                   │
│  ┌────────────┬──────────┬────────┬─────────┬──────────────┐   │
│  │ id         │client_id │ total  │ status  │ created_at   │   │
│  │ (UUID)     │(VARCHAR) │(DECIMAL│(VARCHAR)│(TIMESTAMP)   │   │
│  │            │          │ 19,2)  │         │              │   │
│  ├────────────┼──────────┼────────┼─────────┼──────────────┤   │
│  │550e8400... │client-123│ 150.50 │ PENDING │2024-01-15... │   │
│  │446655...   │          │        │         │10:30:00      │   │
│  └────────────┴──────────┴────────┴─────────┴──────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                            ↑
                       ↑ toDomain()
```

---

### Fluxo de Leitura: Database → Domain

```
┌──────────────────────────────────────────────────────────┐
│ 1. Service chama                                         │
│    repository.findById("550e8400...")                    │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 2. OrderJpaAdapter                                       │
│    jpaRepository.findById(UUID.fromString(id))          │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 3. Spring Data JPA gera SQL                              │
│    SELECT * FROM orders WHERE id = ?                     │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 4. Database executa query                                │
│    Retorna ResultSet com dados da linha                  │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 5. Hibernate mapeia ResultSet → OrderEntity              │
│    OrderEntity entity = new OrderEntity();              │
│    entity.setId(resultSet.getObject("id"));             │
│    entity.setClientId(resultSet.getString("client_id"));│
│    entity.setTotal(resultSet.getBigDecimal("total"));   │
│    entity.setStatus(OrderStatus.valueOf(                │
│        resultSet.getString("status")));                  │
│    entity.setCreatedAt(resultSet.getTimestamp(          │
│        "created_at").toLocalDateTime());                │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 6. OrderJpaAdapter converte Entity → Domain              │
│    .map(OrderEntity::toDomain)                          │
│                                                          │
│    Order order = new Order(                              │
│        entity.id,                                       │
│        entity.clientId,                                 │
│        entity.total,                                    │
│        entity.status,                                   │
│        entity.createdAt                                 │
│    );                                                    │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 7. Retorna Optional<Order> para Service                 │
│    Optional.of(order)                                    │
└──────────────────────────────────────────────────────────┘
```

---

## Independência do Domínio Explicada

### Princípio Fundamental

**O domínio NÃO deve depender de nada externo!**

```
┌─────────────────────────────────────────────────────────┐
│                    DOMAIN (Núcleo)                      │
│  ┌────────────────────────────────────────────────┐    │
│  │  Order.java                                     │    │
│  │  OrderStatus.java                               │    │
│  │                                                 │    │
│  │  ❌ SEM imports de:                            │    │
│  │     - jakarta.persistence.*                     │    │
│  │     - org.springframework.*                     │    │
│  │     - adapter.*                                 │    │
│  │                                                 │    │
│  │  ✅ APENAS imports de:                         │    │
│  │     - java.time.*                               │    │
│  │     - java.math.*                               │    │
│  │     - java.util.*                               │    │
│  │     - lombok.* (opcional)                       │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
                       ↑
                       │ depende
                       │
┌─────────────────────────────────────────────────────────┐
│                 ADAPTER (Infraestrutura)                │
│  ┌────────────────────────────────────────────────┐    │
│  │  OrderEntity.java                               │    │
│  │  OrderJpaAdapter.java                           │    │
│  │                                                 │    │
│  │  ✅ Pode importar:                             │    │
│  │     - com.example.domain.model.Order           │    │
│  │     - jakarta.persistence.*                     │    │
│  │     - org.springframework.*                     │    │
│  └────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

---

### Comparação: Order vs OrderEntity

#### Order.java (Domain - Puro)

```java
package com.example.domain.model;

import lombok.AllArgsConstructor;
import lombok.Value;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ✅ SEM anotações de framework!
// ✅ SEM dependências externas!

@Value  // Apenas Lombok (opcional)
@AllArgsConstructor
public class Order {
    UUID id;
    String clientId;
    BigDecimal total;
    OrderStatus status;
    LocalDateTime createdAt;
    
    // ✅ Lógica de negócio pura
    public static Order create(String clientId, BigDecimal total) {
        return new Order(
            UUID.randomUUID(),
            clientId,
            total,
            OrderStatus.PENDING,
            LocalDateTime.now()
        );
    }
    
    // ✅ Regra de negócio encapsulada
    public Order confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm");
        }
        return new Order(id, clientId, total, OrderStatus.CONFIRMED, createdAt);
    }
}
```

#### OrderEntity.java (Adapter - Poluído com framework)

```java
package com.example.adapter.output.persistence;

import com.example.domain.model.Order;  // ✅ Importa domínio
import com.example.domain.model.OrderStatus;
import jakarta.persistence.*;  // ❌ JPA (framework)
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// ❌ Poluído com anotações JPA
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {
    @Id
    private UUID id;
    
    @Column(name = "client_id", nullable = false)
    private String clientId;
    
    @Column(name = "total", nullable = false)
    private BigDecimal total;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // ✅ Conhece Order (domínio)
    // ✅ Responsável pela conversão
    public static OrderEntity from(Order order) {
        return new OrderEntity(
            order.getId(),
            order.getClientId(),
            order.getTotal(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
    
    public Order toDomain() {
        return new Order(id, clientId, total, status, createdAt);
    }
}
```

---

### Por Que Essa Separação é Fundamental?

#### 1. Testabilidade

**Domain (fácil de testar):**
```java
@Test
void shouldCreateOrderWithPendingStatus() {
    // ✅ Sem dependências! Teste puro
    Order order = Order.create("client-1", BigDecimal.valueOf(100));
    
    assertEquals(OrderStatus.PENDING, order.getStatus());
    assertNotNull(order.getId());
    // Rápido, sem banco, sem Spring
}

@Test
void shouldConfirmOrder() {
    // ✅ Sem mocks! Teste direto
    Order order = Order.create("client-1", BigDecimal.valueOf(100));
    Order confirmed = order.confirm();
    
    assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
    assertEquals(OrderStatus.PENDING, order.getStatus()); // Original não muda
}
```

**Entity (difícil de testar):**
```java
@Test
void shouldSaveOrderEntity() {
    // ❌ Precisa de:
    // - Spring Context
    // - Banco de dados (H2, testcontainers)
    // - EntityManager
    // - Transações
    
    OrderEntity entity = new OrderEntity();
    entity.setId(UUID.randomUUID());
    // ... configurar todos os campos
    
    entityManager.persist(entity);
    entityManager.flush();
    
    // Teste lento, complexo, frágil
}
```

---

#### 2. Flexibilidade de Persistência

**Trocar JPA por MongoDB:**

```java
// ❌ Se Order tivesse anotações JPA:
// Precisaria mudar Order.java (domínio!)
// Quebraria testes do domínio
// Lógica de negócio afetada

// ✅ Com separação:
// Order.java → Inalterado! ✅
// Cria novo: OrderMongoDocument.java
```

**Múltiplos adapters simultâneos:**

```java
// OrderEntity (JPA) - banco principal
@Entity
public class OrderEntity { }

// OrderMongoDocument (MongoDB) - cache
@Document
public class OrderMongoDocument { }

// OrderRedisHash (Redis) - sessões
@RedisHash
public class OrderRedisHash { }

// Todos convertem de/para Order (domínio)!
// Order permanece puro!
```

---

#### 3. Mudanças no Domínio Sem Afetar Banco

**Adicionar campo calculado:**

```java
// Domain muda
@Value
public class Order {
    UUID id;
    String clientId;
    BigDecimal total;
    OrderStatus status;
    LocalDateTime createdAt;
    
    // NOVO: Campo calculado (não persiste)
    public BigDecimal getTotalWithTax() {
        return total.multiply(BigDecimal.valueOf(1.1));
    }
    
    // NOVO: Método de negócio
    public boolean isExpensive() {
        return total.compareTo(BigDecimal.valueOf(1000)) > 0;
    }
}

// OrderEntity → Inalterado!
// Banco → Inalterado!
// ✅ Zero impacto na persistência
```

---

#### 4. Portabilidade

**Domain pode ser usado em:**
- Microserviços diferentes
- Bibliotecas compartilhadas
- Aplicações desktop
- Aplicações mobile (Android/iOS)
- Testes isolados
- Ferramentas CLI

```java
// Domain puro pode ser publicado como biblioteca
<dependency>
    <groupId>com.example</groupId>
    <artifactId>orders-domain</artifactId>
    <version>1.0.0</version>
</dependency>

// Sem trazer Spring, JPA, ou qualquer dependência pesada!
```

---

## Exemplos Práticos de Uso

### Exemplo 1: Criar e Salvar Pedido

```java
// Service Layer
@RequiredArgsConstructor
public class OrderService implements CreateOrderPort {
    private final OrderRepositoryPort repository;
    
    @Override
    public Order create(String clientId, BigDecimal total) {
        // 1. Cria Order (domain)
        Order newOrder = Order.create(clientId, total);
        
        // 2. Salva via porta (abstração)
        return repository.save(newOrder);
    }
}

// Adapter Layer
@Component
@RequiredArgsConstructor
public class OrderJpaAdapter implements OrderRepositoryPort {
    private final OrderSpringJpaRepository jpaRepository;
    
    @Override
    public Order save(Order order) {
        // 1. Domain → Entity
        OrderEntity entity = OrderEntity.from(order);
        
        // 2. Salva no banco
        OrderEntity saved = jpaRepository.save(entity);
        
        // 3. Entity → Domain
        return saved.toDomain();
    }
}

// Uso completo
Order order = orderService.create("client-123", BigDecimal.valueOf(150.50));
System.out.println(order.getId()); // UUID gerado
System.out.println(order.getStatus()); // PENDING
```

---

### Exemplo 2: Buscar e Confirmar Pedido

```java
// Service Layer
@Override
public Order confirm(String orderId) {
    // 1. Busca pedido
    Order order = repository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    
    // 2. Processa pagamento (via porta)
    boolean approved = paymentPort.process(orderId, order.getTotal());
    if (!approved) {
        throw new PaymentDeclinedException();
    }
    
    // 3. Confirma (regra de domínio)
    Order confirmed = order.confirm();
    
    // 4. Salva estado novo
    return repository.save(confirmed);
}

// Adapter Layer
@Override
public Optional<Order> findById(String id) {
    return jpaRepository.findById(UUID.fromString(id))
        .map(OrderEntity::toDomain);  // Entity → Domain
}

// SQL gerado automaticamente:
// SELECT * FROM orders WHERE id = ?
```

---

### Exemplo 3: Teste Unitário do Domínio

```java
class OrderTest {
    
    @Test
    @DisplayName("Deve criar pedido com status PENDING")
    void shouldCreateOrderWithPendingStatus() {
        // ✅ Sem dependências! Teste puro!
        Order order = Order.create("client-123", BigDecimal.valueOf(100));
        
        // Verificações
        assertNotNull(order.getId());
        assertEquals("client-123", order.getClientId());
        assertEquals(BigDecimal.valueOf(100), order.getTotal());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertNotNull(order.getCreatedAt());
        
        // ✅ Teste rápido: ~0ms
        // ✅ Sem banco de dados
        // ✅ Sem Spring context
        // ✅ Sem mocks
    }
    
    @Test
    @DisplayName("Deve confirmar pedido PENDING")
    void shouldConfirmPendingOrder() {
        Order pending = Order.create("client-123", BigDecimal.valueOf(100));
        
        // Confirma
        Order confirmed = pending.confirm();
        
        // Verificações
        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(pending.getId(), confirmed.getId());
        
        // ✅ Original não mudou (imutabilidade)
        assertEquals(OrderStatus.PENDING, pending.getStatus());
    }
    
    @Test
    @DisplayName("Deve lançar exceção ao confirmar pedido já confirmado")
    void shouldThrowWhenConfirmingAlreadyConfirmed() {
        Order order = Order.create("client-123", BigDecimal.valueOf(100))
            .confirm(); // Já confirmado
        
        // Tenta confirmar novamente
        assertThrows(IllegalStateException.class, () -> {
            order.confirm();
        });
    }
}
```

---

### Exemplo 4: Teste com Mocks (Service)

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    
    @Mock
    private OrderRepositoryPort repository;
    
    @Mock
    private PaymentPort paymentPort;
    
    private OrderService service;
    
    @BeforeEach
    void setUp() {
        service = new OrderService(repository, paymentPort);
    }
    
    @Test
    @DisplayName("Deve confirmar pedido quando pagamento aprovado")
    void shouldConfirmOrderWhenPaymentApproved() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        Order existingOrder = Order.create("client-123", BigDecimal.valueOf(100));
        Order confirmedOrder = existingOrder.confirm();
        
        when(repository.findById(orderId))
            .thenReturn(Optional.of(existingOrder));
        when(paymentPort.process(orderId, BigDecimal.valueOf(100)))
            .thenReturn(true);  // Pagamento aprovado
        when(repository.save(any(Order.class)))
            .thenReturn(confirmedOrder);
        
        // Act
        Order result = service.confirm(orderId);
        
        // Assert
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(repository).findById(orderId);
        verify(paymentPort).process(orderId, BigDecimal.valueOf(100));
        verify(repository).save(any(Order.class));
    }
    
    @Test
    @DisplayName("Deve lançar exceção quando pagamento recusado")
    void shouldThrowWhenPaymentDeclined() {
        // Arrange
        String orderId = UUID.randomUUID().toString();
        Order existingOrder = Order.create("client-123", BigDecimal.valueOf(100));
        
        when(repository.findById(orderId))
            .thenReturn(Optional.of(existingOrder));
        when(paymentPort.process(orderId, BigDecimal.valueOf(100)))
            .thenReturn(false);  // Pagamento recusado
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            service.confirm(orderId);
        });
        
        verify(repository, never()).save(any(Order.class)); // Não deve salvar
    }
}
```

---

### Exemplo 5: Conversão com Transformação

```java
// Cenário: Order tem campo adicional que não persiste
@Value
public class Order {
    UUID id;
    String clientId;
    BigDecimal total;
    OrderStatus status;
    LocalDateTime createdAt;
    List<OrderItem> items;  // NOVO: Itens do pedido
    
    // Campo calculado (não persiste)
    public BigDecimal getCalculatedTotal() {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// Conversão Domain → Entity (ignora items)
public static OrderEntity from(Order order) {
    return new OrderEntity(
        order.getId(),
        order.getClientId(),
        order.getCalculatedTotal(),  // Usa total calculado
        order.getStatus(),
        order.getCreatedAt()
        // items não são persistidos aqui
    );
}

// Conversão Entity → Domain (reconstrói items de outra fonte)
public Order toDomain(List<OrderItemEntity> itemEntities) {
    List<OrderItem> items = itemEntities.stream()
        .map(OrderItemEntity::toDomain)
        .toList();
    
    return new Order(
        this.id,
        this.clientId,
        this.total,
        this.status,
        this.createdAt,
        items  // Reconstrói lista de items
    );
}
```

---

## Resumo Visual Final

```
┌────────────────────────────────────────────────────────────┐
│                  ARQUITETURA HEXAGONAL                      │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              DOMAIN (Núcleo Puro)                    │ │
│  │  ┌────────────────────────────────────────────────┐ │ │
│  │  │  Order.java (Entidade)                         │ │ │
│  │  │  - Imutável (@Value)                           │ │ │
│  │  │  - Sem dependências externas                   │ │ │
│  │  │  - Lógica de negócio pura                      │ │ │
│  │  │  - create(), confirm()                         │ │ │
│  │  └────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
│                           ↕                                │
│  ┌──────────────────────────────────────────────────────┐ │
│  │           ADAPTER (Infraestrutura)                   │ │
│  │  ┌────────────────────────────────────────────────┐ │ │
│  │  │  OrderEntity.java (Persistência)               │ │ │
│  │  │  - Mutável (@Data)                             │ │ │
│  │  │  - Anotações JPA (@Entity, @Table, @Column)   │ │ │
│  │  │  - Depende de Order (domínio)                  │ │ │
│  │  │  - from(Order), toDomain()                     │ │ │
│  │  └────────────────────────────────────────────────┘ │ │
│  └──────────────────────────────────────────────────────┘ │
│                           ↕                                │
│  ┌──────────────────────────────────────────────────────┐ │
│  │                  DATABASE                            │ │
│  │  Table: orders                                       │ │
│  │  - id (UUID)                                         │ │
│  │  - client_id (VARCHAR)                               │ │
│  │  - total (DECIMAL)                                   │ │
│  │  - status (VARCHAR)                                  │ │
│  │  - created_at (TIMESTAMP)                            │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                             │
│  Conversões:                                                │
│  Order → OrderEntity: from(order)                          │
│  OrderEntity → Order: toDomain()                           │
│                                                             │
│  Direção da dependência: Adapter → Domain                  │
│  Domain NUNCA depende de Adapter!                          │
└────────────────────────────────────────────────────────────┘
```

---

**✅ Seção 8.1 (OrderEntity) - COMPLETA COM TODAS AS PARTES FALTANTES!** 1. HTTP Request                                          │
│    POST /api/v1/orders                                   │
│    Body: {"clientId": "client-123", "total": 150.50}    │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 2. OrderController (REST Adapter)                        │
│    - @RequestBody desserializa JSON                      │
│    - CreateOrderRequest("client-123", 150.50)           │
│    - Chama createOrderPort.create(...)                  │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 3. CreateOrderPort (Interface)                           │
│    - Porta de entrada (abstração)                       │
│    - Implementada por OrderService                       │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 4. OrderService (Domain - Use Case)                      │
│    Order newOrder = Order.create("client-123", 150.50); │
│                                                          │
│    Cria Order:                                           │
│    - id: UUID.randomUUID()                              │
│    - clientId: "client-123"                             │
│    - total: BigDecimal(150.50)                          │
│    - status: PENDING                                     │
│    - createdAt: LocalDateTime.now()                     │
│                                                          │
│    return repository.save(newOrder);                     │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 5. OrderRepositoryPort (Interface)                       │
│    - Porta de saída (abstração)                         │
│    - Implementada por OrderJpaAdapter                    │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 6. OrderJpaAdapter (Persistence Adapter)                 │
│    OrderEntity entity = OrderEntity.from(order);        │
│                                                          │
│    Conversão Domain → Entity:                            │
│    order.getId() → entity.id                            │
│    order.getClientId() → entity.clientId                │
│    order.getTotal() → entity.total                      │
│    order.getStatus() → entity.status                    │
│    order.getCreatedAt() → entity.createdAt              │
│                                                          │
│    OrderEntity saved = jpaRepository.save(entity);      │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 7. Spring Data JPA                                       │
│    - Detecta que entity não existe no contexto          │
│    - Gera SQL INSERT                                     │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 8. Database (H2)                                         │
│    INSERT INTO orders (id, client_id, total,            │
│                       status, created_at)                │
│    VALUES ('550e8400-e29b-41d4-a716-446655440000',     │
│            'client-123', 150.50, 'PENDING',             │
│            '2024-01-15 10:30:00');                      │
│                                                          │
│    Retorna: Rows affected: 1                            │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│ 9. OrderJpaAdapter (continuação)                         │
│    Order order = saved.toDomain();                      │
│                                                          │
│    Conversão Entity → Domain:                            │
│    entity.id → order.id                                 │
│    entity.clientId → order.clientId                     │
│    entity.total → order.total                           │
│    entity.status → order.status                         │
│    entity.createdAt → order.createdAt                   │
│                                                          │
│    return order;                                         │
└──────────────────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────────────────┐
│