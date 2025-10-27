# Seções 8.2 e 8.3 - Persistência Completa

## 8.2 OrderSpringJpaRepository (Interface Spring Data JPA)

**Arquivo:** `src/main/java/com/example/adapter/output/persistence/OrderSpringJpaRepository.java`

```java
package com.example.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface OrderSpringJpaRepository extends JpaRepository<OrderEntity, UUID> {
}
```

---

### Explicação Completa

#### Imports

```java
import org.springframework.data.jpa.repository.JpaRepository;
```
**O que é:** Interface base do Spring Data JPA que fornece métodos CRUD prontos.

```java
import org.springframework.stereotype.Repository;
```
**O que é:** Anotação que marca componente de persistência do Spring.

```java
import java.util.UUID;
```
**O que é:** Tipo do ID usado na entidade (chave primária).

---

#### @Repository

```java
@Repository
```

**Explicação:**
- Marca interface como componente de persistência do Spring
- Spring registra automaticamente como bean no ApplicationContext
- Traduz exceções de persistência para hierarquia do Spring

**O que faz internamente:**
```java
// Spring detecta @Repository
// Cria proxy da interface
// Registra como bean
// Aplica tradução de exceções
```

**Hierarquia de exceções:**
```
DataAccessException (Spring)
    ├── DataIntegrityViolationException
    ├── OptimisticLockingFailureException
    ├── PessimisticLockingFailureException
    └── ...

// Traduz de:
SQLException (JDBC)
PersistenceException (JPA)
// Para:
DataAccessException (Spring - mais genérica)
```

**Por que traduzir exceções?**
- ✅ Abstração: Código não depende de exceções específicas do banco
- ✅ Portabilidade: Trocar banco não quebra tratamento de erros
- ✅ Consistência: Todas exceções seguem mesmo padrão

**Tecnicamente opcional:**
- Spring Data detecta interfaces que estendem `JpaRepository`
- Mas `@Repository` deixa explícito e adiciona tradução de exceções

---

#### Interface e Herança

```java
public interface OrderSpringJpaRepository extends JpaRepository<OrderEntity, UUID> {
```

**Explicação:**

**`interface`** - Não é classe!
- Você **NÃO implementa** nada manualmente
- Spring Data JPA gera implementação automaticamente em runtime
- **Magia do Spring Data!** ✨

**`extends JpaRepository<OrderEntity, UUID>`**
- Herda todos os métodos CRUD prontos
- **Parâmetros genéricos:**
  - `OrderEntity` - Tipo da entidade (anotada com @Entity)
  - `UUID` - Tipo da chave primária (campo com @Id)

**Como Spring Data funciona:**

1. **Em tempo de inicialização:**
```java
// Spring escaneia pacotes
@EnableJpaRepositories("com.example.adapter.output.persistence")

// Encontra OrderSpringJpaRepository
// Detecta que estende JpaRepository
// Cria proxy dinâmico (JDK Dynamic Proxy ou CGLIB)
```

2. **Cria implementação:**
```java
// Spring gera automaticamente (simplificado):
public class OrderSpringJpaRepositoryImpl implements OrderSpringJpaRepository {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public OrderEntity save(OrderEntity entity) {
        if (entity.getId() == null || !entityManager.contains(entity)) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }
    
    @Override
    public Optional<OrderEntity> findById(UUID id) {
        OrderEntity entity = entityManager.find(OrderEntity.class, id);
        return Optional.ofNullable(entity);
    }
    
    @Override
    public List<OrderEntity> findAll() {
        return entityManager
            .createQuery("SELECT e FROM OrderEntity e", OrderEntity.class)
            .getResultList();
    }
    
    // ... outros métodos
}
```

3. **Registra como bean:**
```java
// ApplicationContext contém:
orderSpringJpaRepository -> OrderSpringJpaRepositoryImpl (proxy)
```

---

### Métodos Herdados Automaticamente

#### Categoria: Salvamento

```java
// Salva uma entidade (INSERT ou UPDATE)
OrderEntity save(OrderEntity entity);
```
**Comportamento:**
- Se `entity.getId()` é `null` → INSERT
- Se `entity.getId()` existe E está no contexto → UPDATE
- Se `entity.getId()` existe mas NÃO está no contexto → SELECT + UPDATE (merge)

**SQL gerado:**
```sql
-- INSERT (novo)
INSERT INTO orders (id, client_id, total, status, created_at) 
VALUES (?, ?, ?, ?, ?);

-- UPDATE (existente)
UPDATE orders 
SET client_id = ?, total = ?, status = ?, created_at = ? 
WHERE id = ?;
```

**Exemplo:**
```java
// Criar novo
OrderEntity newEntity = new OrderEntity();
newEntity.setId(UUID.randomUUID());
newEntity.setClientId("client-123");
newEntity.setTotal(BigDecimal.valueOf(100));
newEntity.setStatus(OrderStatus.PENDING);
newEntity.setCreatedAt(LocalDateTime.now());

OrderEntity saved = repository.save(newEntity); // INSERT

// Atualizar existente
saved.setStatus(OrderStatus.CONFIRMED);
repository.save(saved); // UPDATE
```

---

```java
// Salva múltiplas entidades
List<OrderEntity> saveAll(Iterable<OrderEntity> entities);
```
**Comportamento:**
- Chama `save()` para cada entidade
- Retorna lista com todas salvas

**Uso:**
```java
List<OrderEntity> entities = Arrays.asList(entity1, entity2, entity3);
List<OrderEntity> saved = repository.saveAll(entities);
```

---

```java
// Salva e força flush (escreve imediatamente no banco)
OrderEntity saveAndFlush(OrderEntity entity);
```
**Comportamento:**
- Salva entidade
- Chama `entityManager.flush()` - força execução do SQL
- Útil quando precisa do ID gerado pelo banco imediatamente

**Diferença de save():**
```java
// save() - SQL pode ser adiado até fim da transação
OrderEntity saved = repository.save(entity);
// SQL pode não ter executado ainda!

// saveAndFlush() - SQL executa AGORA
OrderEntity saved = repository.saveAndFlush(entity);
// SQL JÁ executou!
```

---

#### Categoria: Leitura

```java
// Busca por ID
Optional<OrderEntity> findById(UUID id);
```
**Comportamento:**
- Retorna `Optional.of(entity)` se encontrado
- Retorna `Optional.empty()` se não encontrado
- **Nunca retorna null!**

**SQL gerado:**
```sql
SELECT * FROM orders WHERE id = ?;
```

**Uso correto:**
```java
Optional<OrderEntity> result = repository.findById(UUID.randomUUID());

// ✅ CORRETO - trata ambos os casos
OrderEntity entity = result.orElseThrow(() -> 
    new EntityNotFoundException("Order not found")
);

// ✅ CORRETO - ifPresent
result.ifPresent(entity -> {
    System.out.println("Found: " + entity);
});

// ❌ ERRADO - não trata ausência
OrderEntity entity = result.get(); // NoSuchElementException se vazio!
```

---

```java
// Verifica se existe por ID
boolean existsById(UUID id);
```
**Comportamento:**
- Retorna `true` se existir
- Retorna `false` se não existir
- **Mais eficiente que findById()** quando só precisa verificar existência

**SQL gerado:**
```sql
SELECT COUNT(*) FROM orders WHERE id = ?;
-- Ou (otimizado):
SELECT 1 FROM orders WHERE id = ? LIMIT 1;
```

**Uso:**
```java
if (repository.existsById(orderId)) {
    // Existe
} else {
    // Não existe
}
```

---

```java
// Busca todas as entidades
List<OrderEntity> findAll();
```
**Comportamento:**
- Retorna lista com TODAS as entidades da tabela
- ⚠️ **Cuidado:** Pode retornar milhões de registros!
- **Em produção:** Use paginação!

**SQL gerado:**
```sql
SELECT * FROM orders;
```

**Uso:**
```java
List<OrderEntity> allOrders = repository.findAll();
// ⚠️ Pode ser MUITO grande!
```

---

```java
// Busca todas com paginação
Page<OrderEntity> findAll(Pageable pageable);
```
**Comportamento:**
- Retorna página com subset dos dados
- Inclui informações de paginação (total, páginas, etc.)

**SQL gerado:**
```sql
-- Contar total
SELECT COUNT(*) FROM orders;

-- Buscar página
SELECT * FROM orders LIMIT 10 OFFSET 20;
```

**Uso:**
```java
// Página 2, 10 itens por página, ordenado por data
Pageable pageable = PageRequest.of(1, 10, Sort.by("createdAt").descending());
Page<OrderEntity> page = repository.findAll(pageable);

System.out.println("Total elementos: " + page.getTotalElements());
System.out.println("Total páginas: " + page.getTotalPages());
System.out.println("Página atual: " + page.getNumber());
System.out.println("Itens nesta página: " + page.getNumberOfElements());

List<OrderEntity> orders = page.getContent();
```

---

```java
// Busca múltiplos por IDs
List<OrderEntity> findAllById(Iterable<UUID> ids);
```
**Comportamento:**
- Busca todas entidades com IDs fornecidos
- Retorna apenas as encontradas (não lança exceção se algum ID não existir)

**SQL gerado:**
```sql
SELECT * FROM orders WHERE id IN (?, ?, ?);
```

**Uso:**
```java
List<UUID> ids = Arrays.asList(id1, id2, id3);
List<OrderEntity> orders = repository.findAllById(ids);
// Pode retornar 0, 1, 2 ou 3 elementos
```

---

```java
// Conta total de registros
long count();
```
**Comportamento:**
- Retorna quantidade total de registros na tabela

**SQL gerado:**
```sql
SELECT COUNT(*) FROM orders;
```

---

#### Categoria: Deleção

```java
// Deleta por ID
void deleteById(UUID id);
```
**Comportamento:**
- Deleta entidade com ID fornecido
- ⚠️ Lança `EmptyResultDataAccessException` se ID não existir

**SQL gerado:**
```sql
-- Busca primeiro (para validar)
SELECT * FROM orders WHERE id = ?;

-- Depois deleta
DELETE FROM orders WHERE id = ?;
```

**Uso:**
```java
try {
    repository.deleteById(orderId);
} catch (EmptyResultDataAccessException e) {
    // ID não existe
}
```

**Alternativa segura:**
```java
// Não lança exceção se não existir
repository.findById(orderId).ifPresent(repository::delete);
```

---

```java
// Deleta entidade
void delete(OrderEntity entity);
```
**Comportamento:**
- Deleta entidade fornecida
- Usa o ID da entidade

**SQL gerado:**
```sql
DELETE FROM orders WHERE id = ?;
```

---

```java
// Deleta múltiplas entidades
void deleteAll(Iterable<OrderEntity> entities);
```
**Comportamento:**
- Chama `delete()` para cada entidade
- ⚠️ N queries (ineficiente para muitos registros)

---

```java
// Deleta todas as entidades
void deleteAll();
```
**Comportamento:**
- Deleta TODOS os registros da tabela
- ⚠️ **Perigoso!** Use com cuidado!

**SQL gerado:**
```sql
DELETE FROM orders;
```

---

```java
// Deleta todas em batch (mais eficiente)
void deleteAllInBatch();
```
**Comportamento:**
- Executa um único DELETE
- Mais eficiente que `deleteAll()`
- ⚠️ Não verifica cascatas/relacionamentos

---

### Queries Customizadas (Opcionais)

Você pode adicionar métodos próprios seguindo convenções de nomenclatura:

#### Por Convenção de Nome

```java
public interface OrderSpringJpaRepository extends JpaRepository<OrderEntity, UUID> {
    
    // Spring gera SQL automaticamente baseado no nome!
    
    // Buscar por clientId
    List<OrderEntity> findByClientId(String clientId);
    // SQL: SELECT * FROM orders WHERE client_id = ?
    
    // Buscar por status
    List<OrderEntity> findByStatus(OrderStatus status);
    // SQL: SELECT * FROM orders WHERE status = ?
    
    // Buscar por clientId E status
    List<OrderEntity> findByClientIdAndStatus(String clientId, OrderStatus status);
    // SQL: SELECT * FROM orders WHERE client_id = ? AND status = ?
    
    // Buscar por clientId OU status
    List<OrderEntity> findByClientIdOrStatus(String clientId, OrderStatus status);
    // SQL: SELECT * FROM orders WHERE client_id = ? OR status = ?
    
    // Buscar total maior que valor
    List<OrderEntity> findByTotalGreaterThan(BigDecimal amount);
    // SQL: SELECT * FROM orders WHERE total > ?
    
    // Buscar total menor que valor
    List<OrderEntity> findByTotalLessThan(BigDecimal amount);
    // SQL: SELECT * FROM orders WHERE total < ?
    
    // Buscar entre datas
    List<OrderEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    // SQL: SELECT * FROM orders WHERE created_at BETWEEN ? AND ?
    
    // Buscar após data
    List<OrderEntity> findByCreatedAtAfter(LocalDateTime date);
    // SQL: SELECT * FROM orders WHERE created_at > ?
    
    // Buscar com ordenação
    List<OrderEntity> findByClientIdOrderByCreatedAtDesc(String clientId);
    // SQL: SELECT * FROM orders WHERE client_id = ? ORDER BY created_at DESC
    
    // Contar por status
    long countByStatus(OrderStatus status);
    // SQL: SELECT COUNT(*) FROM orders WHERE status = ?
    
    // Verificar existência
    boolean existsByClientId(String clientId);
    // SQL: SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END 
    //      FROM orders WHERE client_id = ?
    
    // Deletar por status
    void deleteByStatus(OrderStatus status);
    // SQL: DELETE FROM orders WHERE status = ?
    
    // Buscar com LIKE
    List<OrderEntity> findByClientIdContaining(String clientIdPart);
    // SQL: SELECT * FROM orders WHERE client_id LIKE %?%
    
    // Buscar com LIKE no início
    List<OrderEntity> findByClientIdStartingWith(String prefix);
    // SQL: SELECT * FROM orders WHERE client_id LIKE ?%
}
```

#### Palavras-chave Suportadas

| Palavra-chave | SQL | Exemplo |
|---------------|-----|---------|
| `And` | AND | `findByNameAndAge` |
| `Or` | OR | `findByNameOrAge` |
| `Between` | BETWEEN | `findByAgeBetween` |
| `LessThan` | < | `findByAgeLessThan` |
| `LessThanEqual` | <= | `findByAgeLessThanEqual` |
| `GreaterThan` | > | `findByAgeGreaterThan` |
| `GreaterThanEqual` | >= | `findByAgeGreaterThanEqual` |
| `After` | > | `findByDateAfter` |
| `Before` | < | `findByDateBefore` |
| `IsNull` | IS NULL | `findByAgeIsNull` |
| `IsNotNull` | IS NOT NULL | `findByAgeIsNotNull` |
| `Like` | LIKE | `findByNameLike` |
| `NotLike` | NOT LIKE | `findByNameNotLike` |
| `StartingWith` | LIKE ?% | `findByNameStartingWith` |
| `EndingWith` | LIKE %? | `findByNameEndingWith` |
| `Containing` | LIKE %?% | `findByNameContaining` |
| `OrderBy` | ORDER BY | `findByAgeOrderByNameDesc` |
| `Not` | != | `findByAgeNot` |
| `In` | IN | `findByAgeIn(Collection<Age>)` |
| `NotIn` | NOT IN | `findByAgeNotIn(Collection<Age>)` |
| `True` | = true | `findByActiveTrue` |
| `False` | = false | `findByActiveFalse` |

---

#### Com @Query (JPQL)

```java
public interface OrderSpringJpaRepository extends JpaRepository<OrderEntity, UUID> {
    
    // JPQL (Java Persistence Query Language)
    @Query("SELECT o FROM OrderEntity o WHERE o.total > :amount")
    List<OrderEntity> findExpensiveOrders(@Param("amount") BigDecimal amount);
    
    // JPQL com múltiplos parâmetros
    @Query("SELECT o FROM OrderEntity o WHERE o.clientId = :clientId AND o.status = :status")
    List<OrderEntity> findByClientAndStatus(
        @Param("clientId") String clientId,
        @Param("status") OrderStatus status
    );
    
    // JPQL com Join (se houver relacionamento)
    @Query("SELECT o FROM OrderEntity o JOIN o.items i WHERE i.quantity > :qty")
    List<OrderEntity> findOrdersWithManyItems(@Param("qty") int quantity);
    
    // SQL nativo
    @Query(value = "SELECT * FROM orders WHERE total > ?1", nativeQuery = true)
    List<OrderEntity> findExpensiveOrdersNative(BigDecimal amount);
    
    // Update customizado
    @Modifying
    @Transactional
    @Query("UPDATE OrderEntity o SET o.status = :status WHERE o.id = :id")
    int updateStatus(@Param("id") UUID id, @Param("status") OrderStatus status);
    
    // Delete customizado
    @Modifying
    @Transactional
    @Query("DELETE FROM OrderEntity o WHERE o.status = :status")
    int deleteByStatus(@Param("status") OrderStatus status);
}
```

---

### Por Que Spring Data é Poderoso?

#### Sem Spring Data (JDBC puro)

```java
public class OrderRepositoryImpl {
    
    private DataSource dataSource;
    
    public OrderEntity findById(UUID id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setObject(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    OrderEntity entity = new OrderEntity();
                    entity.setId((UUID) rs.getObject("id"));
                    entity.setClientId(rs.getString("client_id"));
                    entity.setTotal(rs.getBigDecimal("total"));
                    entity.setStatus(OrderStatus.valueOf(rs.getString("status")));
                    entity.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return entity;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
    
    // 50+ linhas de código boilerplate para CADA método!
}
```

#### Com Spring Data

```java
public interface OrderSpringJpaRepository extends JpaRepository<OrderEntity, UUID> {
    // Pronto! findById() já existe
    // Zero código boilerplate
}
```

**Economia:** ~90% menos código!

---

## 8.3 OrderJpaAdapter (Implementação do Port)

**Arquivo:** `src/main/java/com/example/adapter/output/persistence/OrderJpaAdapter.java`

```java
package com.example.adapter.output.persistence;

import com.example.domain.model.Order;
import com.example.domain.ports.output.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderJpaAdapter implements OrderRepositoryPort {
    private final OrderSpringJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderEntity.from(order);
        OrderEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Order> findById(String id) {
        return jpaRepository.findById(UUID.fromString(id))
            .map(OrderEntity::toDomain);
    }

    @Override
    public void delete(String id) {
        jpaRepository.deleteById(UUID.fromString(id));
    }
}
```

---

### Explicação Completa

#### Imports

```java
import com.example.domain.model.Order;
```
**O que é:** Entidade de domínio (tipo que o adapter retorna)

```java
import com.example.domain.ports.output.OrderRepositoryPort;
```
**O que é:** Porta (interface) que será implementada

```java
import lombok.RequiredArgsConstructor;
```
**O que é:** Lombok gera construtor com campos final

```java
import org.springframework.stereotype.Component;
```
**O que é:** Marca classe como bean Spring

```java
import java.util.Optional;
import java.util.UUID;
```
**O que é:** Tipos Java padrão

---

#### Anotações da Classe

```java
@Component
```

**Explicação:**
- Registra classe como bean gerenciado pelo Spring
- Spring cria instância única (singleton por padrão)
- Permite injeção automática em outras classes

**Por que @Component e não @Repository?**
- Ambos funcionam (são equivalentes tecnicamente)
- `@Repository` é mais semântico (indica camada de dados + tradução de exceções)
- `@Component` é mais genérico (bom para adapters)
- **Nossa escolha:** `@Component` pois é adapter, não repositório puro

**Alternativas:**
```java
@Repository  // Também funcionaria
@Service     // ❌ Não recomendado (Service é para lógica de negócio)
```

---

```java
@RequiredArgsConstructor
```

**Explicação:**
- Lombok gera construtor com campos `final`

**Código gerado:**
```java
public OrderJpaAdapter(OrderSpringJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
}
```

**Por que construtor ao invés de @Autowired no campo?**

❌ **Field Injection (não recomendado):**
```java
@Autowired
private OrderSpringJpaRepository jpaRepository;

// Problemas:
// - Dificulta testes (precisa reflection)
// - Não pode ser final (mutável)
// - Dependências ocultas
```

✅ **Constructor Injection (recomendado):**
```java
@RequiredArgsConstructor
public class OrderJpaAdapter {
    private final OrderSpringJpaRepository jpaRepository;
    
    // Vantagens:
    // - Testável (passa mock no construtor)
    // - Imutável (final)
    // - Dependências explícitas
    // - Impossível criar sem dependências
}
```

---

#### Classe e Implementação

```java
public class OrderJpaAdapter implements OrderRepositoryPort {
```

**Explicação:**

**`implements OrderRepositoryPort`**
- Implementa porta definida no domínio
- **Contrato:** Deve implementar todos os métodos da interface
- Spring injetará essa implementação quando alguém pedir `OrderRepositoryPort`

**Inversão de Dependência (SOLID - DIP):**
```
Domain (OrderRepositoryPort interface)
   ↑ depende
Adapter (OrderJpaAdapter implements)

Domínio define O QUE precisa
Adapter implementa COMO faz
```

**Como Spring resolve injeção:**
```java
// Em OrderService
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepositoryPort repository;  // Interface!
}

// Spring procura: Quem implementa OrderRepositoryPort?
// Encontra: OrderJpaAdapter
// Injeta: new OrderJpaAdapter(jpaRepository)
```

---

```java
    private final OrderSpringJpaRepository jpaRepository;
```

**Explicação:**
- Dependência do repositório Spring Data
- `final` - Imutável, injetado via construtor
- **Tipo:** Interface do Spring Data JPA (não implementação!)

**Fluxo de dependências:**
```
OrderService (domain)
    ↓ usa
OrderRepositoryPort (interface - domain)
    ↓ implementada por
OrderJpaAdapter (adapter)
    ↓ usa
OrderSpringJpaRepository (interface - Spring Data)
    ↓ implementada por
SimpleJpaRepository (proxy gerado pelo Spring)
    ↓ usa
EntityManager (JPA)
    ↓ usa
JDBC Driver
    ↓ usa
Database
```

---

### Método save()

```java
    @Override
    public Order save(Order order) {
```

**Explicação:**
- `@Override` - Implementa método da porta
- **Contrato:**
  - Recebe: `Order` (domínio)
  - Retorna: `Order` (domínio)
- **Responsabilidade:** Persistir e retornar Order atualizado

**Fluxo do método:**
1. Converte Domain → Entity
2. Salva Entity via Spring Data
3. Converte Entity → Domain
4. Retorna Domain

---

```java
        OrderEntity entity = OrderEntity.from(order);
```

**Explicação:**
- **Passo 1:** Converte `Order` (domínio) para `OrderEntity` (JPA)
- Usa factory method estático `from()`

**Por que converter?**
- JPA trabalha com entities anotadas (@Entity)
- Domain não tem/não pode ter anotações JPA
- Separação de responsabilidades

**O que acontece:**
```java
// OrderEntity.from() faz:
return new OrderEntity(
    order.getId(),           // UUID
    order.getClientId(),     // String
    order.getTotal(),        // BigDecimal
    order.getStatus(),       // OrderStatus
    order.getCreatedAt()     // LocalDateTime
);
```

---

```java
        OrderEntity saved = jpaRepository.save(entity);
```

**Explicação:**
- **Passo 2:** Salva no banco via Spring Data JPA
- `save()` é idempotente (INSERT ou UPDATE)

**Como Spring Data decide INSERT vs UPDATE:**
```java
if (entity.getId() == null) {
    // INSERT
    entityManager.persist(entity);
} else if (entityManager.contains(entity)) {
    // UPDATE (entity já está no contexto)
    // Hibernate detecta mudanças automaticamente
} else {
    // MERGE (entity existe mas não está no contexto)
    entity = entityManager.merge(entity);
}
```

**SQL gerado:**

**INSERT (novo pedido):**
```sql
INSERT INTO orders (id, client_id, total, status, created_at) 
VALUES ('550e8400-e29b-41d4-a716-446655440000', 
        'client-123', 
        150.50, 
        'PENDING', 
        '2024-01-15 10:30:00');
```

**UPDATE (pedido existente):**
```sql
UPDATE orders 
SET client_id = 'client-123',
    total = 150.50,
    status = 'CONFIRMED',
    created_at = '2024-01-15 10:30:00'
WHERE id = '550e8400-e29b-41d4-a716-446655440000';
```

**Por que retorna saved?**
- Banco pode modificar campos (timestamps, versões, etc.)
- saved contém estado real do banco

---

```java
        return saved.toDomain();
```

**Explicação:**
- **Passo 3:** Converte `OrderEntity` (JPA) → `Order` (domínio)
- Usa método de instância `toDomain()`
- **Importante:** Sempre retorna tipo do domínio!

**O que acontece:**
```java
// saved.toDomain() faz:
return new Order(
    this.id,           // UUID da entity
    this.clientId,     // String da entity
    this.total,        // BigDecimal da entity
    this.status,       // OrderStatus da entity
    this.createdAt     // LocalDateTime da entity
);
```

**Fluxo visual completo do save():**
```
Order (domain)
    ↓ OrderEntity.from(order)
OrderEntity (JPA) - novo objeto
    ↓ jpaRepository.save(entity)
SQL INSERT/UPDATE executa
    ↓
OrderEntity (JPA) - retornado do banco
    ↓ saved.toDomain()
Order (domain) - retorna para service
```

---

```java
    }
```

---

### Método findById()

```java
    @Override
    public Optional<Order> findById(String id) {
```

**Explicação:**
- Busca pedido por ID
- **Parâmetro:** `String id` (flexível - pode ser UUID.toString())
- **Retorno:** `Optional<Order>` (pode não existir)

**Por que Optional?**
- ✅ Explicita que pode não existir
- ✅ Força tratamento de ausência
- ✅ Evita NullPointerException
- ✅ Pattern funcional

---

```java
        return jpaRepository.findById(UUID.fromString(id))
```

**Explicação:**
- **Passo 1:** Converte String → UUID
  - `UUID.fromString("550e8400-e29b-41d4-a716-446655440000")` → UUID
  - **Lança:** `IllegalArgumentException` se formato inválido
  
- **Passo 2:** Chama repositório Spring Data
  - `findById(UUID)` retorna `Optional<OrderEntity>`

**SQL gerado:**
```sql
SELECT * FROM orders WHERE id = '550e8400-e29b-41d4-a716-446655440000';
```

**O que Hibernate faz:**
1. Gera SQL SELECT
2. Executa query no banco
3. Mapeia ResultSet → OrderEntity
4. Retorna Optional<OrderEntity>

---

```java
            .map(OrderEntity::toDomain);
```

**Explicação:**
- **Passo 3:** Converte Optional<Entity> → Optional<Domain>
- `.map()` - Operação funcional do Optional

**Como funciona:**
```java
// Se Optional contém valor:
Optional<OrderEntity> entity = Optional.of(entityFromDatabase);
Optional<Order> order = entity.map(e -> e.toDomain());
// Resultado: Optional.of(order)

// Se Optional está vazio:
Optional<OrderEntity> entity = Optional.empty();
Optional<Order> order = entity.map(e -> e.toDomain());
// Resultado: Optional.empty()
// map() NÃO é chamado!
```

**Method reference:**
```java
.map(OrderEntity::toDomain)

// É equivalente a:
.map(entity -> entity.toDomain())

// Que faz:
.map(entity -> new Order(
    entity.id,
    entity.clientId,
    entity.total,
    entity.status,
    entity.createdAt
))
```

**Por que usar map() ao invés de if?**

❌ **Imperativo (mais verboso):**
```java
Optional<OrderEntity> entityOpt = jpaRepository.findById(UUID.fromString(id));
if (entityOpt.isPresent()) {
    OrderEntity entity = entityOpt.get();
    Order order = entity.toDomain();
    return Optional.of(order);
} else {
    return Optional.empty();
}
```

✅ **Funcional (conciso e seguro):**
```java
return jpaRepository.findById(UUID.fromString(id))
    .map(OrderEntity::toDomain);
```

**Vantagens:**
- ✅ Uma linha vs 7 linhas
- ✅ Impossível esquecer de tratar Optional vazio
- ✅ Composição funcional
- ✅ Imutável e thread-safe

---

```java
    }
```

**Fluxo visual completo do findById():**
```
String id
    ↓ UUID.fromString(id)
UUID
    ↓ jpaRepository.findById(uuid)
SQL: SELECT * FROM orders WHERE id = ?
    ↓
Optional<OrderEntity>
    ↓ .map(OrderEntity::toDomain)
Optional<Order>
    ↓
Retorna para Service
```

---

### Método delete()

```java
    @Override
    public void delete(String id) {
        jpaRepository.deleteById(UUID.fromString(id));
    }
```

**Explicação:**
- Deleta pedido por ID
- **Parâmetro:** `String id`
- **Retorno:** `void` (não retorna nada)

**Passos:**
1. Converte String → UUID
2. Chama Spring Data deleteById()

**SQL gerado:**
```sql
-- Spring Data primeiro busca (para validar)
SELECT * FROM orders WHERE id = ?;

-- Se existir, deleta
DELETE FROM orders WHERE id = ?;
```

**⚠️ Comportamento importante:**
```java
jpaRepository.deleteById(nonExistentId);
// Lança: EmptyResultDataAccessException
```

**Tratamento de erro:**

❌ **Sem tratamento:**
```java
@Override
public void delete(String id) {
    jpaRepository.deleteById(UUID.fromString(id));
    // ❌ Lança exceção se não existir
}
```

✅ **Com tratamento (opção 1 - silencioso):**
```java
@Override
public void delete(String id) {
    jpaRepository.findById(UUID.fromString(id))
        .ifPresent(jpaRepository::delete);
    // ✅ Não lança exceção, silenciosamente ignora se não existir
}
```

✅ **Com tratamento (opção 2 - explícito):**
```java
@Override
public void delete(String id) {
    try {
        jpaRepository.deleteById(UUID.fromString(id));
    } catch (EmptyResultDataAccessException e) {
        // Log ou ignora
        log.warn("Attempted to delete non-existent order: {}", id);
    }
}
```

**Nossa implementação:**
- Delegação simples
- Se ID não existir, exceção propaga para camada superior
- Service layer decide como tratar

---

### Resumo do Adapter

```
┌─────────────────────────────────────────────────────────┐
│              OrderJpaAdapter                            │
│  (Implementa OrderRepositoryPort)                       │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  save(Order order):                                     │
│    1. Order → OrderEntity.from(order)                  │
│    2. jpaRepository.save(entity) → SQL INSERT/UPDATE   │
│    3. saved.toDomain() → Order                         │
│    4. return Order                                      │
│                                                         │
│  findById(String id):                                   │
│    1. String → UUID.fromString(id)                     │
│    2. jpaRepository.findById(uuid) → SQL SELECT        │
│    3. .map(toDomain) → Optional<Order>                 │
│    4. return Optional<Order>                           │
│                                                         │
│  delete(String id):                                     │
│    1. String → UUID.fromString(id)                     │
│    2. jpaRepository.deleteById(uuid) → SQL DELETE      │
│    3. void (sem retorno)                               │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Fluxo Completo: Criar e Salvar Pedido

### Passo a Passo

```
┌─────────────────────────────────────────────────────────┐
│ 1. Controller recebe requisição HTTP                    │
│    POST /api/v1/orders                                  │
│    Body: {"clientId": "client-123", "total": 150.50}   │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 2. OrderController.create()                             │
│    Order order = createOrderPort.create(                │
│        request.clientId(),                              │
│        request.total()                                  │
│    );                                                    │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 3. OrderService.create() [Domain Layer]                 │
│    Order newOrder = Order.create(clientId, total);     │
│                                                         │
│    Cria Order:                                          │
│    - id: UUID.randomUUID()                             │
│    - clientId: "client-123"                            │
│    - total: BigDecimal(150.50)                         │
│    - status: PENDING                                    │
│    - createdAt: LocalDateTime.now()                    │
│                                                         │
│    return repository.save(newOrder);                    │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 4. OrderJpaAdapter.save() [Adapter Layer]               │
│    OrderEntity entity = OrderEntity.from(order);       │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 5. OrderEntity.from() [Conversão Domain → Entity]       │
│    new OrderEntity(                                     │
│        order.getId(),        // UUID                    │
│        order.getClientId(),  // "client-123"           │
│        order.getTotal(),     // BigDecimal(150.50)     │
│        order.getStatus(),    // PENDING                 │
│        order.getCreatedAt()  // LocalDateTime.now()    │
│    )                                                    │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 6. OrderJpaAdapter.save() [continuação]                 │
│    OrderEntity saved = jpaRepository.save(entity);     │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 7. OrderSpringJpaRepository.save() [Spring Data]        │
│    - Detecta que entity.id não é null                  │
│    - Verifica se está no contexto de persistência      │
│    - Como é novo, executa INSERT                        │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 8. Hibernate gera SQL                                   │
│    INSERT INTO orders (                                 │
│        id, client_id, total, status, created_at        │
│    ) VALUES (                                           │
│        '550e8400-e29b-41d4-a716-446655440000',        │
│        'client-123',                                    │
│        150.50,                                          │
│        'PENDING',                                       │
│        '2024-01-15 10:30:00'                           │
│    );                                                   │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 9. Database (H2) executa INSERT                         │
│    Retorna: 1 row affected                             │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 10. OrderSpringJpaRepository retorna                    │
│     OrderEntity saved (com dados do banco)              │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 11. OrderJpaAdapter.save() [continuação]                │
│     return saved.toDomain();                            │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 12. OrderEntity.toDomain() [Conversão Entity → Domain]  │
│     new Order(                                          │
│         this.id,         // UUID                        │
│         this.clientId,   // "client-123"               │
│         this.total,      // BigDecimal(150.50)         │
│         this.status,     // PENDING                     │
│         this.createdAt   // LocalDateTime              │
│     )                                                   │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 13. OrderService retorna Order para Controller          │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 14. OrderController converte para DTO                   │
│     OrderResponse response = OrderResponse.from(order); │
└─────────────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────────────┐
│ 15. HTTP Response                                       │
│     HTTP/1.1 201 Created                                │
│     Content-Type: application/json                      │
│                                                         │
│     {                                                   │
│       "id": "550e8400-e29b-41d4-a716-446655440000",   │
│       "clientId": "client-123",                        │
│       "total": 150.50,                                 │
│       "status": "PENDING"                              │
│     }                                                   │
└─────────────────────────────────────────────────────────┘
```

---

## Testes do Adapter

### Teste Unitário (com Mocks)

```java
@ExtendWith(MockitoExtension.class)
class OrderJpaAdapterTest {
    
    @Mock
    private OrderSpringJpaRepository jpaRepository;
    
    @InjectMocks
    private OrderJpaAdapter adapter;
    
    @Test
    @DisplayName("Deve salvar order e retornar domain")
    void shouldSaveOrderAndReturnDomain() {
        // Arrange
        Order order = Order.create("client-123", BigDecimal.valueOf(100));
        OrderEntity entity = OrderEntity.from(order);
        
        when(jpaRepository.save(any(OrderEntity.class)))
            .thenReturn(entity);
        
        // Act
        Order result = adapter.save(order);
        
        // Assert
        assertNotNull(result);
        assertEquals(order.getId(), result.getId());
        assertEquals(order.getClientId(), result.getClientId());
        assertEquals(order.getTotal(), result.getTotal());
        
        verify(jpaRepository).save(any(OrderEntity.class));
    }
    
    @Test
    @DisplayName("Deve buscar order por ID")
    void shouldFindOrderById() {
        // Arrange
        UUID id = UUID.randomUUID();
        Order order = Order.create("client-123", BigDecimal.valueOf(100));
        OrderEntity entity = OrderEntity.from(order);
        
        when(jpaRepository.findById(id))
            .thenReturn(Optional.of(entity));
        
        // Act
        Optional<Order> result = adapter.findById(id.toString());
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(order.getClientId(), result.get().getClientId());
        
        verify(jpaRepository).findById(id);
    }
    
    @Test
    @DisplayName("Deve retornar empty quando order não existe")
    void shouldReturnEmptyWhenOrderNotFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        
        when(jpaRepository.findById(id))
            .thenReturn(Optional.empty());
        
        // Act
        Optional<Order> result = adapter.findById(id.toString());
        
        // Assert
        assertFalse(result.isPresent());
        
        verify(jpaRepository).findById(id);
    }
    
    @Test
    @DisplayName("Deve deletar order por ID")
    void shouldDeleteOrderById() {
        // Arrange
        UUID id = UUID.randomUUID();
        
        // Act
        adapter.delete(id.toString());
        
        // Assert
        verify(jpaRepository).deleteById(id);
    }
}
```

---

### Teste de Integração (com banco real)

```java
@SpringBootTest
@Transactional
class OrderJpaAdapterIntegrationTest {
    
    @Autowired
    private OrderJpaAdapter adapter;
    
    @Autowired
    private OrderSpringJpaRepository jpaRepository;
    
    @Test
    @DisplayName("Deve salvar e recuperar order do banco")
    void shouldSaveAndRetrieveOrder() {
        // Arrange
        Order order = Order.create("client-123", BigDecimal.valueOf(100));
        
        // Act - Save
        Order saved = adapter.save(order);
        
        // Assert - Save
        assertNotNull(saved.getId());
        
        // Act - Find
        Optional<Order> found = adapter.findById(saved.getId().toString());
        
        // Assert - Find
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(saved.getClientId(), found.get().getClientId());
        assertEquals(saved.getTotal(), found.get().getTotal());
        assertEquals(saved.getStatus(), found.get().getStatus());
    }
    
    @Test
    @DisplayName("Deve atualizar order existente")
    void shouldUpdateExistingOrder() {
        // Arrange - Criar e salvar
        Order order = Order.create("client-123", BigDecimal.valueOf(100));
        Order saved = adapter.save(order);
        
        // Act - Confirmar e salvar novamente
        Order confirmed = saved.confirm();
        Order updated = adapter.save(confirmed);
        
        // Assert
        assertEquals(saved.getId(), updated.getId());
        assertEquals(OrderStatus.CONFIRMED, updated.getStatus());
        
        // Verificar no banco
        Optional<Order> fromDb = adapter.findById(saved.getId().toString());
        assertTrue(fromDb.isPresent());
        assertEquals(OrderStatus.CONFIRMED, fromDb.get().getStatus());
    }
    
    @Test
    @DisplayName("Deve deletar order")
    void shouldDeleteOrder() {
        // Arrange
        Order order = Order.create("client-123", BigDecimal.valueOf(100));
        Order saved = adapter.save(order);
        
        // Act
        adapter.delete(saved.getId().toString());
        
        // Assert
        Optional<Order> found = adapter.findById(saved.getId().toString());
        assertFalse(found.isPresent());
    }
}
```

---

## Boas Práticas

### ✅ DO (Faça)

1. **Sempre retorne tipos do domínio**
```java
// ✅ Correto
@Override
public Order save(Order order) {
    OrderEntity entity = OrderEntity.from(order);
    OrderEntity saved = jpaRepository.save(entity);
    return saved.toDomain(); // Retorna Order (domínio)
}
```

2. **Use Optional para indicar ausência**
```java
// ✅ Correto
@Override
public Optional<Order> findById(String id) {
    return jpaRepository.findById(UUID.fromString(id))
        .map(OrderEntity::toDomain);
}
```

3. **Isole conversões**
```java
// ✅ Correto - conversões em métodos dedicados
OrderEntity entity = OrderEntity.from(order);
Order domain = entity.toDomain();
```

4. **Trate exceções apropriadamente**
```java
// ✅ Correto
@Override
public Optional<Order> findById(String id) {
    try {
        return jpaRepository.findById(UUID.fromString(id))
            .map(OrderEntity::toDomain);
    } catch (IllegalArgumentException e) {
        log.warn("Invalid UUID format: {}", id);
        return Optional.empty();
    }
}
```

---

### ❌ DON'T (Não Faça)

1. **Nunca retorne entidades JPA**
```java
// ❌ Errado
@Override
public OrderEntity save(Order order) {
    return jpaRepository.save(OrderEntity.from(order));
    // ❌ Expõe OrderEntity para o domínio!
}
```

2. **Nunca retorne null**
```java
// ❌ Errado
@Override
public Order findById(String id) {
    OrderEntity entity = jpaRepository.findById(UUID.fromString(id)).orElse(null);
    return entity != null ? entity.toDomain() : null;
    // ❌ Usa null ao invés de Optional!
}
```

3. **Nunca misture lógica de negócio no adapter**
```java
// ❌ Errado
@Override
public Order save(Order order) {
    if (order.getTotal().compareTo(BigDecimal.valueOf(1000)) > 0) {
        // ❌ Regra de negócio no adapter!
        order = order.applyDiscount();
    }
    OrderEntity entity = OrderEntity.from(order);
    return jpaRepository.save(entity).toDomain();
}
```

4. **Nunca faça conversão inline sem método**
```java
// ❌ Errado
@Override
public Order save(Order order) {
    OrderEntity entity = new OrderEntity(
        order.getId(),
        order.getClientId(),
        // ... 20 linhas de mapeamento manual
    );
    // ❌ Conversão duplicada, difícil de manter
}
```

---

## Resumo das Seções 8.2 e 8.3

### OrderSpringJpaRepository (8.2)

| Aspecto | Detalhe |
|---------|---------|
| **Tipo** | Interface (não implementa nada) |
| **Extends** | JpaRepository<OrderEntity, UUID> |
| **Métodos** | ~20 métodos CRUD herdados automaticamente |
| **SQL** | Gerado automaticamente pelo Spring Data |
| **Customização** | Queries por nome ou @Query |
| **Implementação** | Gerada dinamicamente pelo Spring |

### OrderJpaAdapter (8.3)

| Aspecto | Detalhe |
|---------|---------|
| **Tipo** | Classe concreta |
| **Implements** | OrderRepositoryPort (porta do domínio) |
| **Responsabilidade** | Converter Domain ↔ Entity e chamar JPA |
| **Dependência** | OrderSpringJpaRepository |
| **Métodos** | save(), findById(), delete() |
| **Conversões** | OrderEntity.from() e toDomain() |

---

**✅ Seções 8.2 e 8.3 COMPLETAS!**