**✅ Seção 8.1 (OrderEntity) - COMPLETA**

Esta seção pode ser copiada diretamente para o README.md principal, substituindo a seção 8.1 existente.

---

## 📝 O que foi coberto:

### ✅ Imports explicados
- Domain imports (Order, OrderStatus)
- JPA imports (jakarta.persistence.*)
- Lombok imports (@Data, @NoArgsConstructor, @AllArgsConstructor)
- Java base imports (BigDecimal, LocalDateTime, UUID)

### ✅ Anotações JPA detalhadas
- **@Entity** - O que Hibernate faz, SQL gerado
- **@Table** - Por que "orders" e não "order", opções avançadas
- **@Data** - Todo código gerado pelo Lombok
- **@NoArgsConstructor** - Por que JPA exige, como Hibernate usa
- **@AllArgsConstructor** - Uso prático

### ✅ Cada campo explicado
- **@Id + UUID** - Estratégias de geração (IDENTITY, SEQUENCE, UUID), comparação Long vs UUID
- **@Column** - Todas opções (name, nullable, unique, length, insertable, updatable), exemplos práticos
- **BigDecimal** - Por que usar, problema com double/float, precision e scale, operações
- **@Enumerated** - STRING vs ORDINAL com perigos detalhados, cenários de falha, migração
- **LocalDateTime** - Comparação completa com Instant, ZonedDateTime, LocalDate, LocalTime, quando usar cada

### ✅ Métodos de conversão
- **from()** - Factory method static, Domain → Entity, por que usar
- **toDomain()** - Método de instância, Entity → Domain, diferença de from()
- **Fluxo bidirecional completo** - Diagrama visual passo a passo

### ✅ Conceitos fundamentais
- **Separação Domain vs Entity** - Por que duas classes, tabela comparativa
- **Independência do domínio** - Direção correta da dependência
- **Imutabilidade** - Domain imutável, Entity mutável

### ✅ Exemplos práticos
- SQL gerado por Hibernate
- Código gerado por Lombok
- Conversões entre tipos de data
- Queries SQL com Spring Data
- Tratamento de erros
- Casos de uso reais

---

## 🎯 Próximos passos

Se você quiser, posso agora criar as seções:

**8.2 - OrderSpringJpaRepository** (Interface Spring Data JPA)
- Como Spring Data funciona
- Métodos herdados automaticamente
- Queries por convenção de nome
- @Query customizada
- SQL gerado

**8.3 - OrderJpaAdapter** (Implementação do Port)
- @Component vs @Repository
- Implementação de OrderRepositoryPort
- Método save() detalhado
- Método findById() detalhado
- Método delete() detalhado
- Fluxo completo com conversões

Quer que eu continue com a 8.2 e 8.3?### 8.1 OrderEntity (Entidade JPA)

**Arquivo:** `src/main/java/com/example/adapter/output/persistence/OrderEntity.java`

```java
package com.example.adapter.output.persistence;
```

**Explicação:**
- `adapter/output/persistence` - Adaptador de saída para persistência
- Contém classes relacionadas ao banco de dados

---

```java
import com.example.domain.model.Order;
import com.example.domain.model.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
```

**Explicação detalhada dos imports:**

**Imports do Domínio:**
- `com.example.domain.model.Order` - Entidade de domínio (para conversão)
- `com.example.domain.model.OrderStatus` - Enum compartilhado

**Imports JPA (jakarta.persistence):**
- `@Entity` - Marca classe como entidade JPA (mapeada para tabela)
- `@Table` - Configura nome e propriedades da tabela
- `@Id` - Define chave primária
- `@Column` - Configura coluna específica (nome, constraints)
- `@Enumerated` - Define como persistir enums (STRING ou ORDINAL)

**Por que jakarta.persistence e não javax.persistence?**
- **Jakarta EE 9+** renomeou de `javax.*` para `jakarta.*`
- Spring Boot 3+ usa Jakarta EE
- Versões antigas do Spring Boot (<3.0) usavam `javax.persistence`

**Imports Lombok:**
- `@Data` - Gera getters, setters, equals, hashCode, toString
- `@NoArgsConstructor` - Construtor vazio (JPA exige!)
- `@AllArgsConstructor` - Construtor com todos os campos

**Imports Java Base:**
- `BigDecimal` - Valores monetários com precisão decimal
- `LocalDateTime` - Data e hora (sem timezone)
- `UUID` - Identificador único universal

---

```java
@Entity
```

**Explicação completa:**
- Marca classe como **entidade JPA**
- **O que isso significa na prática:**
  - Hibernate criará uma tabela no banco de dados
  - Cada instância desta classe = uma linha na tabela
  - Cada campo = uma coluna na tabela
  - Hibernate gerencia automaticamente INSERT/UPDATE/DELETE

**O que Hibernate faz automaticamente:**
1. Analisa a classe na inicialização
2. Cria schema no banco (se `ddl-auto=create` ou `update`)
3. Gera SQL automaticamente para CRUD
4. Mapeia resultados de queries para objetos Java

**SQL gerado automaticamente (se ddl-auto=create):**
```sql
CREATE TABLE order_entity (
    id UUID NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    total DECIMAL(19,2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);
```

---

```java
@Table(name = "orders")
```

**Explicação:**
- Sobrescreve o nome padrão da tabela
- **Sem essa anotação:** Hibernate usaria `order_entity` (nome da classe em snake_case)
- **Com ela:** Tabela se chama `orders`

**Por que "orders" e não "order"?**
- `order` é palavra reservada em SQL (`ORDER BY`)
- Usar palavra reservada causa erro de sintaxe SQL
- **Boa prática:** Usar plurais para nomes de tabelas (`orders`, `products`, `users`)

**Outras opções de @Table:**
```java
@Table(
    name = "orders",                    // Nome da tabela
    schema = "sales",                   // Schema do banco
    catalog = "mydb",                   // Catálogo (para alguns bancos)
    uniqueConstraints = {               // Constraints de unicidade
        @UniqueConstraint(columnNames = {"client_id", "created_at"})
    },
    indexes = {                         // Índices
        @Index(name = "idx_status", columnList = "status")
    }
)
```

**Exemplo com schema:**
```java
@Table(name = "orders", schema = "ecommerce")
// SQL: SELECT * FROM ecommerce.orders
```

---

```java
@Data
```

**Explicação detalhada:**
- Anotação do Lombok que gera métodos automaticamente
- **Código gerado:**

```java
// Getters
public UUID getId() { return id; }
public String getClientId() { return clientId; }
public BigDecimal getTotal() { return total; }
public OrderStatus getStatus() { return status; }
public LocalDateTime getCreatedAt() { return createdAt; }

// Setters
public void setId(UUID id) { this.id = id; }
public void setClientId(String clientId) { this.clientId = clientId; }
public void setTotal(BigDecimal total) { this.total = total; }
public void setStatus(OrderStatus status) { this.status = status; }
public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

// equals() - compara todos os campos
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OrderEntity that = (OrderEntity) o;
    return Objects.equals(id, that.id) &&
           Objects.equals(clientId, that.clientId) &&
           Objects.equals(total, that.total) &&
           Objects.equals(status, that.status) &&
           Objects.equals(createdAt, that.createdAt);
}

// hashCode() - baseado em todos os campos
@Override
public int hashCode() {
    return Objects.hash(id, clientId, total, status, createdAt);
}

// toString() - representação em String
@Override
public String toString() {
    return "OrderEntity{" +
           "id=" + id +
           ", clientId='" + clientId + '\'' +
           ", total=" + total +
           ", status=" + status +
           ", createdAt=" + createdAt +
           '}';
}
```

**Por que JPA precisa de getters/setters?**
- Hibernate usa **reflexão** para acessar campos
- Acessa via getters/setters, não diretamente nos campos
- **Lazy loading** e **proxies** dependem disso

**Exemplo de uso do Hibernate internamente:**
```java
// Hibernate popula objeto após query
OrderEntity entity = new OrderEntity();
entity.setId(resultSet.getObject("id", UUID.class));
entity.setClientId(resultSet.getString("client_id"));
entity.setTotal(resultSet.getBigDecimal("total"));
// ...
```

---

```java
@NoArgsConstructor
```

**Explicação:**
- Gera construtor vazio: `public OrderEntity() {}`
- **Por que JPA/Hibernate EXIGE isso?**

**Como Hibernate cria objetos:**
1. Executa query SQL: `SELECT * FROM orders WHERE id = ?`
2. Recebe resultado (linha do banco)
3. Cria instância vazia: `OrderEntity entity = new OrderEntity()`
4. Popula campos via setters:
   ```java
   entity.setId(resultSet.getObject("id", UUID.class));
   entity.setClientId(resultSet.getString("client_id"));
   entity.setTotal(resultSet.getBigDecimal("total"));
   entity.setStatus(OrderStatus.valueOf(resultSet.getString("status")));
   entity.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
   ```

**Se não tiver construtor vazio, você verá este erro:**
```
org.hibernate.InstantiationException: No default constructor for entity: 
com.example.adapter.output.persistence.OrderEntity
```

**⚠️ Importante:** 
- Construtor vazio pode ser `protected` ou `private` (Hibernate consegue acessar via reflexão)
- Mas `public` é mais comum e evita problemas

**Alternativas:**
```java
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// Gera: protected OrderEntity() {}
// Útil para prevenir instanciação fora do package
```

---

```java
@AllArgsConstructor
```

**Explicação:**
- Gera construtor com **todos os campos** como parâmetros
- **Código gerado:**
```java
public OrderEntity(UUID id, String clientId, BigDecimal total, 
                   OrderStatus status, LocalDateTime createdAt) {
    this.id = id;
    this.clientId = clientId;
    this.total = total;
    this.status = status;
    this.createdAt = createdAt;
}
```

**Por que precisamos deste construtor?**
- Usado no método `from()` para converter Domain → Entity
- Permite criar instância completa de uma vez
- Mais seguro que criar vazio e popular via setters

**Uso prático:**
```java
// Com @AllArgsConstructor (conciso)
OrderEntity entity = new OrderEntity(
    UUID.randomUUID(),
    "client-123",
    BigDecimal.valueOf(150.50),
    OrderStatus.PENDING,
    LocalDateTime.now()
);

// Sem (verboso)
OrderEntity entity = new OrderEntity();
entity.setId(UUID.randomUUID());
entity.setClientId("client-123");
entity.setTotal(BigDecimal.valueOf(150.50));
entity.setStatus(OrderStatus.PENDING);
entity.setCreatedAt(LocalDateTime.now());
```

---

```java
public class OrderEntity {
```

**Explicação:**
- Classe que representa a **tabela** no banco de dados
- **NÃO é a entidade de domínio!**

**Diferenças cruciais:**

| Aspecto | Order (Domain) | OrderEntity (JPA) |
|---------|---------------|-------------------|
| **Propósito** | Lógica de negócio | Persistência |
| **Mutabilidade** | Imutável (`@Value`) | Mutável (`@Data`) |
| **Dependências** | Zero (puro Java) | JPA, Hibernate |
| **Anotações** | Lombok apenas | JPA + Lombok |
| **Responsabilidade** | Regras de negócio | Mapeamento banco |
| **Métodos** | Business methods (`confirm()`, `create()`) | Conversores (`from()`, `toDomain()`) |
| **Testes** | Sem dependências | Precisa de banco ou mocks |

**Por que duas classes separadas?**
- ✅ **Domínio limpo** - Sem poluição de anotações JPA
- ✅ **Flexibilidade** - Domínio pode mudar sem afetar banco
- ✅ **Testabilidade** - Testar domínio sem banco de dados
- ✅ **Independência** - Trocar JPA por MongoDB sem alterar domínio
- ✅ **Single Responsibility** - Cada classe tem uma responsabilidade

**Exemplo de mudança no domínio SEM afetar banco:**
```java
// Domínio adiciona novo campo calculado
@Value
public class Order {
    UUID id;
    String clientId;
    BigDecimal total;
    OrderStatus status;
    LocalDateTime createdAt;
    
    // NOVO: Campo calculado (não persiste)
    public BigDecimal totalWithTax() {
        return total.multiply(BigDecimal.valueOf(1.1));
    }
}

// OrderEntity permanece inalterado!
// Banco não muda
```

---

```java
    @Id
    private UUID id;
```

**Explicação linha por linha:**

**`@Id`**
- Marca o campo como **chave primária** da tabela
- Hibernate usa este campo no `WHERE` de queries
- **Obrigatório!** Toda entidade JPA precisa de `@Id`

**`UUID id`**
- Tipo: Identificador único universal (128 bits)
- Formato: `550e8400-e29b-41d4-a716-446655440000`

**Por que UUID ao invés de Long?**

| Aspecto | UUID | Long |
|---------|------|------|
| **Tamanho** | 16 bytes (128 bits) | 8 bytes (64 bits) |
| **Geração** | Aplicação (UUID.randomUUID()) | Banco (auto-increment) |
| **Único** | Globalmente | Apenas na tabela |
| **Segurança** | Não expõe quantidade | Expõe: 1, 2, 3... |
| **Distribuição** | Funciona em múltiplos bancos | Conflitos em sharding |
| **Performance** | Índice menos eficiente | Índice sequencial eficiente |

**Vantagens do UUID:**
- ✅ Globalmente único (não precisa do banco para gerar)
- ✅ Seguro (atacantes não sabem quantos registros existem)
- ✅ Distribuído (funciona em múltiplos bancos/microserviços)
- ✅ Conhecemos o ID antes de salvar (útil em eventos)

**Desvantagens do UUID:**
- ❌ Mais espaço (16 bytes vs 8 bytes)
- ❌ Índice menos eficiente (não sequencial)
- ❌ Mais difícil de debugar

**Estratégias de geração de ID:**

```java
// 1. UUID gerado pela aplicação (nosso caso)
@Id
private UUID id;
// Aplicação gera: UUID.randomUUID()
// JPA apenas persiste o valor

// 2. Auto-incremento do banco (MySQL, PostgreSQL)
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
// Banco gera: 1, 2, 3, 4...
// SQL: INSERT INTO orders (...) VALUES (...) - banco retorna ID

// 3. Sequence do banco (PostgreSQL, Oracle)
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_seq")
@SequenceGenerator(name = "order_seq", sequenceName = "order_sequence", allocationSize = 1)
private Long id;
// Banco usa sequence: NEXTVAL('order_sequence')

// 4. UUID gerado pelo Hibernate
@Id
@GeneratedValue(generator = "UUID")
@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
private UUID id;
// Hibernate gera UUID (não a aplicação)

// 5. Tabela de IDs (compatível com todos os bancos)
@Id
@GeneratedValue(strategy = GenerationType.TABLE)
private Long id;
// Hibernate mantém tabela separada para IDs
```

**Nosso caso (UUID gerado na aplicação):**
```java
// Domain gera ID
Order order = Order.create(clientId, total);
// Internamente: UUID.randomUUID()

// JPA apenas persiste
OrderEntity entity = OrderEntity.from(order);
jpaRepository.save(entity);
// SQL: INSERT INTO orders (id, ...) VALUES ('550e8400...', ...)
```

**Vantagem:** Conhecemos o ID antes de salvar!
```java
Order order = orderService.create("client-123", BigDecimal.valueOf(100));
String orderId = order.getId().toString(); // Já temos o ID!
// Útil para: logs, eventos, retornos imediatos
```

---

```java
    @Column(name = "client_id", nullable = false)
    private String clientId;
```

**Explicação detalhada:**

**`@Column`**
- Configura a coluna no banco de dados
- **Opcional** - JPA usa padrões se omitido
- Permite customizar nome, constraints, tipo

**`name = "client_id"`**
- Nome da coluna no banco
- **Convenção:** snake_case para SQL (palavras separadas por underscore)
- **Campo Java:** `clientId` (camelCase)
- **Coluna SQL:** `client_id` (snake_case)

**Sem especificar name:**
```java
@Column
private String clientId;
// Hibernate decide baseado no dialeto:
// - PostgreSQL: client_id
// - MySQL: clientId ou client_id (depende da configuração)
// - Oracle: CLIENT_ID
```

**Por que especificar o nome explicitamente?**
- ✅ **Consistência:** Sempre em snake_case
- ✅ **Previsibilidade:** Não depende do dialeto SQL
- ✅ **Refatoração:** Renomear campo Java não muda coluna do banco
- ✅ **Documentação:** Fica claro qual é o nome no banco

**`nullable = false`**
- Equivalente a `NOT NULL` no SQL
- **Validação em dois níveis:**
  1. **JPA (antes do SQL):** Lança `PropertyValueException` se null
  2. **Banco (constraint):** Rejeita INSERT/UPDATE com null

**Resultado SQL (ddl-auto=create):**
```sql
CREATE TABLE orders (
    client_id VARCHAR(255) NOT NULL,
    ...
);
```

**Teste prático:**
```java
// Tentando salvar com clientId null
OrderEntity entity = new OrderEntity();
entity.setId(UUID.randomUUID());
entity.setClientId(null);  // NULL!
entity.setTotal(BigDecimal.valueOf(100));
entity.setStatus(OrderStatus.PENDING);
entity.setCreatedAt(LocalDateTime.now());

jpaRepository.save(entity);
// Lança: PropertyValueException: not-null property references a null or transient value
```

**Outras opções de @Column:**
```java
@Column(
    name = "client_id",               // Nome da coluna
    nullable = false,                 // NOT NULL constraint
    unique = false,                   // UNIQUE constraint
    length = 100,                     // VARCHAR(100) - padrão é 255
    insertable = true,                // Inclui em INSERT
    updatable = true,                 // Inclui em UPDATE
    columnDefinition = "VARCHAR(100) DEFAULT 'unknown'"  // SQL customizado
)
private String clientId;
```

**Exemplos práticos:**

```java
// Campo que não pode ser atualizado após criação
@Column(name = "created_by", updatable = false)
private String createdBy;
// SQL: INSERT usa o valor, UPDATE ignora

// Campo que não pode ser inserido (gerado pelo banco)
@Column(name = "row_version", insertable = false, updatable = false)
private Long version;
// SQL: INSERT e UPDATE ignoram, SELECT lê

// Email único
@Column(name = "email", unique = true, length = 150)
private String email;
// SQL: CREATE UNIQUE INDEX idx_email ON orders(email)

// Texto longo
@Column(name = "description", length = 5000)
private String description;
// SQL: VARCHAR(5000) ou TEXT (depende do tamanho)
```

---

```java
    @Column(name = "total", nullable = false)
    private BigDecimal total;
```

**Explicação:**

**`BigDecimal`**
- Tipo Java para valores decimais com **precisão exata**
- Perfeito para valores monetários
- Mapeia para `DECIMAL` ou `NUMERIC` no SQL

**Por que BigDecimal e não double/float?**

❌ **Problema com double/float:**
```java
double price1 = 0.1;
double price2 = 0.2;
double total = price1 + price2;
System.out.println(total);  // 0.30000000000000004 ❌

// Problema real: dinheiro
double saldo = 1000.00;
double debito = 999.90;
double resto = saldo - debito;
System.out.println(resto);  // 0.09999999999990905 ❌
// Cliente reclamando: "Cadê meus 10 centavos?!"
```

✅ **Solução com BigDecimal:**
```java
BigDecimal price1 = new BigDecimal("0.1");
BigDecimal price2 = new BigDecimal("0.2");
BigDecimal total = price1.add(price2);
System.out.println(total);  // 0.3 ✅

// Sempre use String no construtor!
BigDecimal saldo = new BigDecimal("1000.00");
BigDecimal debito = new BigDecimal("999.90");
BigDecimal resto = saldo.subtract(debito);
System.out.println(resto);  // 0.10 ✅
```

**⚠️ Armadilha comum:**
```java
// ❌ ERRADO - ainda tem imprecisão!
BigDecimal wrong = new BigDecimal(0.1);
System.out.println(wrong);  // 0.1000000000000000055511151231257827021181583404541015625

// ✅ CORRETO
BigDecimal right = new BigDecimal("0.1");
System.out.println(right);  // 0.1
```

**Mapeamento no banco:**
- **Sem configuração:** `DECIMAL(19,2)` (19 dígitos total, 2 decimais)
- **Com configuração:**
```java
@Column(name = "total", nullable = false, precision = 10, scale = 2)
private BigDecimal total;
// Banco: DECIMAL(10,2)
```

**Entendendo precision e scale:**
```
DECIMAL(precision, scale)
        ↑         ↑
   total dígitos  decimais

DECIMAL(10, 2) significa:
- 10 dígitos no total
- 2 dígitos após a vírgula
- Máximo: 99999999.99 (8 antes, 2 depois)

Exemplos válidos para DECIMAL(10,2):
✅ 12345678.90   (10 dígitos, 2 decimais)
✅ 99999999.99   (10 dígitos, 2 decimais)
✅ 0.01          (3 dígitos, 2 decimais)
❌ 123456789.00  (11 dígitos total) - ERRO!
❌ 1234567.123   (10 dígitos, 3 decimais) - ERRO!
```

**Configurações recomendadas por caso:**

```java
// Para Real Brasileiro (BRL)
@Column(precision = 15, scale = 2)
private BigDecimal preco;
// Até: 9.999.999.999.999,99 (quase 10 trilhões)

// Para Dólar Americano (USD)
@Column(precision = 12, scale = 2)
private BigDecimal price;
// Até: 999.999.999,99 (quase 1 bilhão)

// Para criptomoedas (Bitcoin)
@Column(precision = 20, scale = 8)
private BigDecimal btcAmount;
// Até: 999999999999.99999999 BTC
// Bitcoin tem 8 casas decimais

// Para taxas/percentuais
@Column(precision = 5, scale = 4)
private BigDecimal taxRate;
// Até: 9.9999 (999.99%)
// Precisão de 0.01%

// Para pesos/medidas precisas
@Column(precision = 10, scale = 3)
private BigDecimal weight;
// Até: 9999999.999 kg
// Precisão de 1 grama
```

**Operações com BigDecimal:**
```java
// ❌ ERRADO - operadores não funcionam
BigDecimal a = new BigDecimal("10.50");
BigDecimal b = new BigDecimal("5.25");
// a + b  // ERRO de compilação!
// a - b  // ERRO de compilação!
// a * b  // ERRO de compilação!
// a / b  // ERRO de compilação!

// ✅ CORRETO - usar métodos
BigDecimal soma = a.add(b);               // 15.75
BigDecimal subtracao = a.subtract(b);     // 5.25
BigDecimal multiplicacao = a.multiply(b); // 55.125
BigDecimal divisao = a.divide(b, 2, RoundingMode.HALF_UP); // 2.00

// Comparações
a.compareTo(b) > 0   // a > b
a.compareTo(b) == 0  // a == b
a.compareTo(b) < 0   // a < b

// ❌ NUNCA use equals para comparar valores!
a.equals(b)  // Compara escala também! "2.0" != "2.00"

// ✅ Use compareTo
a.compareTo(b) == 0  // Compara apenas valor
```

---

```java
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;
```

**Explicação completa:**

**`@Enumerated(EnumType.STRING)`**
- Define como o enum será persistido no banco
- **EnumType.STRING** - Salva o nome do enum como String

**Definição do enum:**
```java
public enum OrderStatus {
    PENDING,    // Ordinal = 0
    CONFIRMED,  // Ordinal = 1
    SHIPPED,    // Ordinal = 2
    DELIVERED,  // Ordinal = 3
    CANCELLED   // Ordinal = 4
}
```

**Comparação: STRING vs ORDINAL**

### Com EnumType.STRING (✅ RECOMENDADO)

**Código:**
```java
@Enumerated(EnumType.STRING)
private OrderStatus status;
```

**Banco de dados:**
```sql
CREATE TABLE orders (
    status VARCHAR(255) NOT NULL
);

-- Dados
| id | status     |
|----|-----------|
| 1  | PENDING   |
| 2  | CONFIRMED |
| 3  | SHIPPED   |
```

**Vantagens:**
- ✅ Legível em queries SQL diretas
- ✅ Refatoração segura (adicionar enum no meio não quebra)
- ✅ Debugging fácil (vê o nome, não número)
- ✅ Self-documenting (banco mostra o significado)

**Queries SQL legíveis:**
```sql
SELECT * FROM orders WHERE status = 'PENDING';
UPDATE orders SET status = 'CONFIRMED' WHERE id = 1;
```

### Com EnumType.ORDINAL (❌ PERIGOSO)

**Código:**
```java
@Enumerated(EnumType.ORDINAL)
private OrderStatus status;
```

**Banco de dados:**
```sql
CREATE TABLE orders (
    status INTEGER NOT NULL
);

-- Dados
| id | status |
|----|--------|
| 1  | 0      |  -- PENDING
| 2  | 1      |  -- CONFIRMED
| 3  | 2      |  -- SHIPPED
```

**Problemas críticos:**

**Cenário 1: Adicionar enum no meio**
```java
// ANTES (funcionando em produção)
enum OrderStatus {
    PENDING,    // 0
    CONFIRMED,  // 1
    SHIPPED,    // 2
    DELIVERED   // 3
}

// Pedido antigo no banco: status = 1 (CONFIRMED)
```

```java
// DEPOIS - Adicionou PROCESSING no meio!
enum OrderStatus {
    PENDING,     // 0 (inalterado)
    PROCESSING,  // 1 (NOVO!)
    CONFIRMED,   // 2 (era 1, agora é 2!)
    SHIPPED,     // 3 (era 2, agora é 3!)
    DELIVERED    // 4 (era 3, agora é 4!)
}

// 💥 DESASTRE: 
// Pedido antigo tem status = 1 no banco
// Antes significava CONFIRMED
// Agora significa PROCESSING!
// Dados corrompidos!
```

**Cenário 2: Reordenar enums**
```java
// Desenvolvedor reordena (comum em refatoração)
enum OrderStatus {
    CONFIRMED,   // 0 (era 1)
    PENDING,     // 1 (era 0)
    SHIPPED,     // 2 (inalterado)
    DELIVERED    // 3 (inalterado)
}

// 💥 Todos os dados ficam errados!
```

**Quando usar ORDINAL (raríssimo):**
- ❌ Nunca em produção com dados importantes
- ⚠️ Apenas se:
  - Ordem do enum NUNCA mudará
  - Você tem controle total do banco
  - Performance é crítica (economiza alguns bytes)
  - É enum interno sem significado de negócio

**Migração de ORDINAL para STRING:**
```sql
-- 1. Adicionar coluna temporária
ALTER TABLE orders ADD COLUMN status_temp VARCHAR(255);

-- 2. Converter dados
UPDATE orders 
SET status_temp = CASE status
    WHEN 0 THEN 'PENDING'
    WHEN 1 THEN 'CONFIRMED'
    WHEN 2 THEN 'SHIPPED'
    WHEN 3 THEN 'DELIVERED'
    WHEN 4 THEN 'CANCELLED'
END;

-- 3. Remover coluna antiga
ALTER TABLE orders DROP COLUMN status;

-- 4. Renomear coluna temporária
ALTER TABLE orders RENAME COLUMN status_temp TO status;
```

**Configuração adicional (opcional):**
```java
// Se quiser coluna menor (economizar espaço)
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false, length = 20)
private OrderStatus status;
// VARCHAR(20) ao invés de VARCHAR(255)
```

**Conversão customizada (avançado):**
```java
// Se precisar salvar código diferente no banco
@Convert(converter = OrderStatusConverter.class)
@Column(name = "status", length = 3)
private OrderStatus status;

// Converter
@Converter
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {
    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        return switch (status) {
            case PENDING -> "PEN";
            case CONFIRMED -> "CNF";
            case SHIPPED -> "SHP";
            case DELIVERED -> "DEL";
            case CANCELLED -> "CAN";
        };
    }
    
    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        return switch (dbData) {
            case "PEN" -> OrderStatus.PENDING;
            case "CNF" -> OrderStatus.CONFIRMED;
            case "SHP" -> OrderStatus.SHIPPED;
            case "DEL" -> OrderStatus.DELIVERED;
            case "CAN" -> OrderStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown: " + dbData);
        };
    }
}
//
```