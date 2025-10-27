# 📊 Diagramas de Fluxo - Arquitetura Hexagonal

## 1. Visão Geral da Arquitetura

```mermaid
graph TB
    subgraph "MUNDO EXTERNO"
        HTTP[HTTP Client]
        GRPC[gRPC Client]
        MSG[Message Queue]
        DB[(Database)]
        API[External APIs]
    end
    
    subgraph "ADAPTERS - INPUT (Entrada)"
        REST[OrderController<br/>REST API]
        GRPC_ADAPTER[OrderGrpcAdapter<br/>gRPC]
        EVENT[OrderEventListener<br/>Events]
    end
    
    subgraph "DOMAIN - CORE (Núcleo)"
        subgraph "Ports Input"
            CREATE_PORT[CreateOrderPort]
            CONFIRM_PORT[ConfirmOrderPort]
        end
        
        SERVICE[OrderService<br/>Casos de Uso]
        
        subgraph "Model"
            ORDER[Order<br/>Entidade]
            STATUS[OrderStatus<br/>Enum]
        end
        
        subgraph "Ports Output"
            REPO_PORT[OrderRepositoryPort]
            PAY_PORT[PaymentPort]
        end
    end
    
    subgraph "ADAPTERS - OUTPUT (Saída)"
        JPA[OrderJpaAdapter<br/>Persistence]
        MONGO[OrderMongoAdapter<br/>NoSQL]
        PAY_CLIENT[PaymentClientAdapter<br/>HTTP Client]
        PAY_MOCK[PaymentMockAdapter<br/>Mock]
    end
    
    HTTP --> REST
    GRPC --> GRPC_ADAPTER
    MSG --> EVENT
    
    REST --> CREATE_PORT
    REST --> CONFIRM_PORT
    GRPC_ADAPTER --> CREATE_PORT
    EVENT --> CREATE_PORT
    
    CREATE_PORT --> SERVICE
    CONFIRM_PORT --> SERVICE
    
    SERVICE --> ORDER
    ORDER --> STATUS
    
    SERVICE --> REPO_PORT
    SERVICE --> PAY_PORT
    
    REPO_PORT --> JPA
    REPO_PORT -.-> MONGO
    
    PAY_PORT --> PAY_CLIENT
    PAY_PORT -.-> PAY_MOCK
    
    JPA --> DB
    MONGO -.-> DB
    PAY_CLIENT --> API
    
    style ORDER fill:#e1f5ff
    style SERVICE fill:#fff3cd
    style REST fill:#d4edda
    style JPA fill:#d4edda
    style PAY_CLIENT fill:#d4edda
```

---

## 2. Fluxo Completo: Criar Pedido (POST)

```mermaid
sequenceDiagram
    participant Client as Cliente HTTP
    participant Controller as OrderController
    participant Port as CreateOrderPort
    participant Service as OrderService
    participant Domain as Order (Entity)
    participant RepoPort as OrderRepositoryPort
    participant Adapter as OrderJpaAdapter
    participant JPA as Spring JPA
    participant DB as Database
    
    Client->>Controller: POST /api/v1/orders<br/>{clientId, total}
    activate Controller
    
    Note over Controller: @RequestBody deserializa JSON
    Controller->>Controller: CreateOrderRequest
    
    Controller->>Port: create(clientId, total)
    activate Port
    
    Port->>Service: create(clientId, total)
    activate Service
    
    Service->>Domain: Order.create(clientId, total)
    activate Domain
    
    Note over Domain: UUID.randomUUID()<br/>status = PENDING<br/>createdAt = now()
    Domain-->>Service: Order (novo)
    deactivate Domain
    
    Service->>RepoPort: save(order)
    activate RepoPort
    
    RepoPort->>Adapter: save(order)
    activate Adapter
    
    Note over Adapter: OrderEntity.from(order)
    Adapter->>Adapter: Converte Domain → Entity
    
    Adapter->>JPA: save(entity)
    activate JPA
    
    JPA->>DB: INSERT INTO orders...
    activate DB
    DB-->>JPA: Row inserted
    deactivate DB
    
    JPA-->>Adapter: OrderEntity (salvo)
    deactivate JPA
    
    Note over Adapter: entity.toDomain()
    Adapter->>Adapter: Converte Entity → Domain
    
    Adapter-->>RepoPort: Order
    deactivate Adapter
    
    RepoPort-->>Service: Order
    deactivate RepoPort
    
    Service-->>Port: Order
    deactivate Service
    
    Port-->>Controller: Order
    deactivate Port
    
    Note over Controller: OrderResponse.from(order)
    Controller->>Controller: Converte para DTO
    
    Controller-->>Client: 201 Created<br/>OrderResponse JSON
    deactivate Controller
```

---

## 3. Fluxo Completo: Confirmar Pedido (PUT)

```mermaid
sequenceDiagram
    participant Client as Cliente HTTP
    participant Controller as OrderController
    participant Port as ConfirmOrderPort
    participant Service as OrderService
    participant RepoPort as OrderRepositoryPort
    participant Adapter as OrderJpaAdapter
    participant JPA as Spring JPA
    participant DB as Database
    participant PayPort as PaymentPort
    participant PayAdapter as PaymentClientAdapter
    participant PayAPI as Payment Service
    participant Domain as Order (Entity)
    
    Client->>Controller: PUT /orders/{id}/confirm
    activate Controller
    
    Controller->>Port: confirm(orderId)
    activate Port
    
    Port->>Service: confirm(orderId)
    activate Service
    
    Note over Service: 1. Buscar pedido
    Service->>RepoPort: findById(orderId)
    activate RepoPort
    
    RepoPort->>Adapter: findById(orderId)
    activate Adapter
    
    Adapter->>JPA: findById(UUID)
    activate JPA
    
    JPA->>DB: SELECT * FROM orders<br/>WHERE id = ?
    activate DB
    DB-->>JPA: OrderEntity
    deactivate DB
    
    JPA-->>Adapter: Optional<OrderEntity>
    deactivate JPA
    
    Note over Adapter: entity.toDomain()
    Adapter-->>RepoPort: Optional<Order>
    deactivate Adapter
    
    RepoPort-->>Service: Optional<Order>
    deactivate RepoPort
    
    Note over Service: .orElseThrow()
    Service->>Service: Order encontrado
    
    Note over Service: 2. Processar pagamento
    Service->>PayPort: process(orderId, total)
    activate PayPort
    
    PayPort->>PayAdapter: process(orderId, total)
    activate PayAdapter
    
    PayAdapter->>PayAPI: POST /payments<br/>{orderId, amount}
    activate PayAPI
    
    Note over PayAPI: Valida cartão<br/>Cobra valor<br/>Retorna aprovação
    
    PayAPI-->>PayAdapter: {approved: true}
    deactivate PayAPI
    
    PayAdapter-->>PayPort: true
    deactivate PayAdapter
    
    PayPort-->>Service: true (aprovado)
    deactivate PayPort
    
    Note over Service: 3. Confirmar pedido
    Service->>Domain: order.confirm()
    activate Domain
    
    Note over Domain: Valida status = PENDING<br/>Retorna novo Order<br/>com status = CONFIRMED
    
    Domain-->>Service: Order (confirmado)
    deactivate Domain
    
    Note over Service: 4. Salvar pedido confirmado
    Service->>RepoPort: save(confirmedOrder)
    activate RepoPort
    
    RepoPort->>Adapter: save(order)
    activate Adapter
    
    Note over Adapter: OrderEntity.from(order)
    Adapter->>JPA: save(entity)
    activate JPA
    
    JPA->>DB: UPDATE orders<br/>SET status = 'CONFIRMED'<br/>WHERE id = ?
    activate DB
    DB-->>JPA: Updated
    deactivate DB
    
    JPA-->>Adapter: OrderEntity
    deactivate JPA
    
    Note over Adapter: entity.toDomain()
    Adapter-->>RepoPort: Order
    deactivate Adapter
    
    RepoPort-->>Service: Order
    deactivate RepoPort
    
    Service-->>Port: Order (confirmado)
    deactivate Service
    
    Port-->>Controller: Order
    deactivate Port
    
    Note over Controller: OrderResponse.from(order)
    Controller-->>Client: 200 OK<br/>OrderResponse JSON
    deactivate Controller
```

---

## 4. Camadas e Dependências

```mermaid
graph TD
    subgraph "LAYER 1 - Infrastructure (Frameworks)"
        A1[Spring Framework]
        A2[Hibernate/JPA]
        A3[Jackson JSON]
        A4[Tomcat]
    end
    
    subgraph "LAYER 2 - Adapters (Implementações)"
        B1[OrderController<br/>REST]
        B2[OrderJpaAdapter<br/>Persistence]
        B3[PaymentClientAdapter<br/>HTTP]
        B4[OrderEntity<br/>JPA Entity]
    end
    
    subgraph "LAYER 3 - Ports (Interfaces)"
        C1[CreateOrderPort<br/>Input]
        C2[ConfirmOrderPort<br/>Input]
        C3[OrderRepositoryPort<br/>Output]
        C4[PaymentPort<br/>Output]
    end
    
    subgraph "LAYER 4 - Domain (Core Business)"
        D1[OrderService<br/>Use Cases]
        D2[Order<br/>Entity]
        D3[OrderStatus<br/>Enum]
    end
    
    A1 --> B1
    A2 --> B2
    A2 --> B4
    A3 --> B1
    
    B1 --> C1
    B1 --> C2
    B2 --> C3
    B3 --> C4
    
    C1 --> D1
    C2 --> D1
    D1 --> C3
    D1 --> C4
    
    D1 --> D2
    D2 --> D3
    
    style D1 fill:#e1f5ff
    style D2 fill:#e1f5ff
    style D3 fill:#e1f5ff
    style C1 fill:#fff3cd
    style C2 fill:#fff3cd
    style C3 fill:#fff3cd
    style C4 fill:#fff3cd
    style B1 fill:#d4edda
    style B2 fill:#d4edda
    style B3 fill:#d4edda
```

**Legenda:**
- 🔵 **Domain (Azul)** - Lógica pura de negócio, sem dependências externas
- 🟡 **Ports (Amarelo)** - Interfaces que definem contratos
- 🟢 **Adapters (Verde)** - Implementações concretas que usam frameworks
- ⚫ **Infrastructure (Cinza)** - Frameworks e bibliotecas externas

---

## 5. Conversão de Dados (DTOs)

```mermaid
graph LR
    subgraph "HTTP Layer"
        A[CreateOrderRequest<br/>JSON]
        B[OrderResponse<br/>JSON]
    end
    
    subgraph "Domain Layer"
        C[Order<br/>Domain Entity]
    end
    
    subgraph "Persistence Layer"
        D[OrderEntity<br/>JPA Entity]
    end
    
    subgraph "Database"
        E[(Table: orders)]
    end
    
    A -->|Controller| C
    C -->|OrderResponse.from| B
    C -->|OrderEntity.from| D
    D -->|entity.toDomain| C
    D -->|JPA save| E
    E -->|JPA query| D
    
    style C fill:#e1f5ff
    style A fill:#ffeaa7
    style B fill:#ffeaa7
    style D fill:#dfe6e9
```

**Exemplo de dados em cada camada:**

### CreateOrderRequest (JSON)
```json
{
  "clientId": "client-123",
  "total": 150.50
}
```

### Order (Domain)
```java
Order {
  id: UUID("550e8400-e29b-41d4-a716-446655440000"),
  clientId: "client-123",
  total: BigDecimal(150.50),
  status: OrderStatus.PENDING,
  createdAt: LocalDateTime("2024-01-15T10:30:00")
}
```

### OrderEntity (JPA)
```java
OrderEntity {
  id: UUID("550e8400-e29b-41d4-a716-446655440000"),
  clientId: "client-123",
  total: BigDecimal(150.50),
  status: OrderStatus.PENDING,
  createdAt: LocalDateTime("2024-01-15T10:30:00")
}
```

### Database (SQL)
```sql
| id                                   | client_id   | total  | status  | created_at          |
|--------------------------------------|-------------|--------|---------|---------------------|
| 550e8400-e29b-41d4-a716-446655440000 | client-123  | 150.50 | PENDING | 2024-01-15 10:30:00 |
```

### OrderResponse (JSON)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "client-123",
  "total": 150.50,
  "status": "PENDING"
}
```

---

## 6. Ciclo de Vida de um Bean Spring

```mermaid
stateDiagram-v2
    [*] --> ClasspathScan: @SpringBootApplication inicia
    
    ClasspathScan --> BeanDefinition: Encontra @Component, @Service, etc.
    BeanDefinition --> BeanInstantiation: Spring cria instância
    
    BeanInstantiation --> DependencyInjection: Injeta dependências (@Autowired)
    DependencyInjection --> BeanPostProcessorBefore: Processa @PostConstruct
    
    BeanPostProcessorBefore --> InitMethod: Chama métodos de inicialização
    InitMethod --> BeanPostProcessorAfter: Bean pronto
    
    BeanPostProcessorAfter --> Ready: Bean disponível no ApplicationContext
    
    Ready --> InUse: Aplicação usa o bean
    InUse --> Ready: Bean reutilizado (Singleton)
    
    Ready --> PreDestroy: Shutdown da aplicação
    PreDestroy --> [*]: @PreDestroy executado
```

**Exemplo prático:**

```java
@Component
public class OrderService {
    
    private final OrderRepositoryPort repository;
    
    // 1. Construtor chamado (injeção de dependência)
    public OrderService(OrderRepositoryPort repository) {
        System.out.println("1. Constructor called");
        this.repository = repository;
    }
    
    // 2. Após injeção de dependências
    @PostConstruct
    public void init() {
        System.out.println("2. @PostConstruct - Bean initialized");
    }
    
    // 3. Bean pronto para uso
    public Order create(String clientId, BigDecimal total) {
        System.out.println("3. Bean in use");
        return Order.create(clientId, total);
    }
    
    // 4. Antes de destruir o bean
    @PreDestroy
    public void cleanup() {
        System.out.println("4. @PreDestroy - Bean destroyed");
    }
}
```

---

## 7. Tratamento de Erros (Exception Flow)

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant ExceptionHandler
    
    Client->>Controller: PUT /orders/invalid-id/confirm
    activate Controller
    
    Controller->>Service: confirm("invalid-id")
    activate Service
    
    Service->>Repository: findById("invalid-id")
    activate Repository
    
    Repository-->>Service: Optional.empty()
    deactivate Repository
    
    Note over Service: .orElseThrow()
    Service--xController: IllegalArgumentException<br/>"Order not found"
    deactivate Service
    
    Controller--xExceptionHandler: Exception propagada
    deactivate Controller
    
    activate ExceptionHandler
    Note over ExceptionHandler: @ControllerAdvice<br/>captura exceção
    
    ExceptionHandler->>ExceptionHandler: Cria ErrorResponse
    
    ExceptionHandler-->>Client: 404 Not Found<br/>{message: "Order not found"}
    deactivate ExceptionHandler
```

**Implementação de @ControllerAdvice:**

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## 8. Transações (@Transactional)

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant TxManager as Transaction Manager
    participant Repo1 as OrderRepository
    participant Repo2 as PaymentRepository
    participant DB
    
    Client->>Controller: POST /orders
    Controller->>Service: create(...)
    
    Note over Service: @Transactional detectado
    Service->>TxManager: Begin Transaction
    activate TxManager
    
    TxManager->>DB: BEGIN
    
    Service->>Repo1: save(order)
    Repo1->>DB: INSERT INTO orders
    
    Service->>Repo2: save(payment)
    Repo2->>DB: INSERT INTO payments
    
    alt Sucesso
        Service-->>TxManager: Commit
        TxManager->>DB: COMMIT
        TxManager-->>Service: Transaction committed
        deactivate TxManager
        Service-->>Controller: Order created
        Controller-->>Client: 201 Created
    else Exceção
        Service--xTxManager: Exception
        TxManager->>DB: ROLLBACK
        Note over DB: Todas alterações desfeitas
        TxManager--xService: Transaction rolled back
        deactivate TxManager
        Service--xController: Exception
        Controller-->>Client: 500 Error
    end
```

---

## 9. Profiles Spring (Dev vs Prod)

```mermaid
graph TB
    subgraph "application.properties"
        BASE[spring.profiles.active=dev]
    end
    
    subgraph "application-dev.properties"
        DEV1[spring.h2.console.enabled=true]
        DEV2[logging.level.root=DEBUG]
        DEV3[payment.mock.enabled=true]
    end
    
    subgraph "application-prod.properties"
        PROD1[spring.datasource.url=jdbc:postgresql...]
        PROD2[logging.level.root=WARN]
        PROD3[payment.mock.enabled=false]
    end
    
    BASE -->|active=dev| DEV1
    BASE -->|active=dev| DEV2
    BASE -->|active=dev| DEV3
    
    BASE -.->|active=prod| PROD1
    BASE -.->|active=prod| PROD2
    BASE -.->|active=prod| PROD3
    
    DEV3 --> MOCK[PaymentMockAdapter]
    PROD3 --> REAL[PaymentClientAdapter]
    
    style MOCK fill:#d4edda
    style REAL fill:#f8d7da
```

**Como ativar profiles:**

```bash
# Desenvolvimento
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Produção
java -jar app.jar --spring.profiles.active=prod
```

---

## 10. Container Spring (ApplicationContext)

```mermaid
graph TD
    subgraph "ApplicationContext (Spring Container)"
        A[OrderController]
        B[OrderService]
        C[OrderJpaAdapter]
        D[PaymentClientAdapter]
        E[RestTemplate]
        F[OrderSpringJpaRepository]
    end
    
    A -->|Injeta| B
    B -->|Injeta| C
    B -->|Injeta| D
    C -->|Injeta| F
    D -->|Injeta| E
    
    Note1[Singleton Scope<br/>Uma instância compartilhada]
    Note2[Spring gerencia<br/>ciclo de vida]
    
    style A fill:#d4edda
    style B fill:#fff3cd
    style C fill:#d4edda
    style D fill:#d4edda
```

**Como Spring resolve dependências:**

```java
// Spring faz automaticamente:
RestTemplate restTemplate = new RestTemplate();
OrderSpringJpaRepository jpaRepo = new OrderSpringJpaRepositoryImpl();
OrderJpaAdapter jpaAdapter = new OrderJpaAdapter(jpaRepo);
PaymentClientAdapter paymentAdapter = new PaymentClientAdapter(restTemplate);
OrderService service = new OrderService(jpaAdapter, paymentAdapter);
OrderController controller = new OrderController(service, service);

// Armazena tudo no ApplicationContext
```

---

## Como Visualizar Estes Diagramas

Estes diagramas usam **Mermaid**, suportado nativamente por:

### ✅ GitHub/GitLab
- Visualização automática em arquivos `.md`

### ✅ VS Code
- Extensão: "Markdown Preview Mermaid Support"

### ✅ IntelliJ IDEA
- Plugin: "Mermaid"

### ✅ Online
- https://mermaid.live/ - Editor online
- https://mermaid-js.github.io/ - Documentação oficial

### ✅ Exportar como imagem
```bash
# Instalar Mermaid CLI
npm install -g @mermaid-js/mermaid-cli

# Converter para PNG
mmdc -i diagrams.md -o diagram.png
```

---

**💡 Dica:** Salve este arquivo como `DIAGRAMS.md` no projeto!