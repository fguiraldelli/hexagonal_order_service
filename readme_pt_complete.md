> Implementação completa de Arquitetura Hexagonal (Ports & Adapters) usando Java 21 e Spring Framework para microserviços.

## 📋 Índice

- [Visão Geral](#visão-geral)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Explicação Linha a Linha](#explicação-linha-a-linha)
  - [1. Configuração Maven (pom.xml)](#1-configuração-maven-pomxml)
  - [2. Propriedades da Aplicação](#2-propriedades-da-aplicação)
  - [3. Classe Principal](#3-classe-principal)
  - [4. Domain Layer - Modelos](#4-domain-layer---modelos)
  - [5. Domain Layer - Portas](#5-domain-layer---portas)
  - [6. Domain Layer - Serviços](#6-domain-layer---serviços)
  - [7. Adapter Input - REST](#7-adapter-input---rest)
  - [8. Adapter Output - Persistência](#8-adapter-output---persistência)
  - [9. Adapter Output - Cliente Externo](#9-adapter-output---cliente-externo)
  - [10. Configuração de Beans](#10-configuração-de-beans)
- [Todas as Anotações Explicadas](#todas-as-anotações-explicadas)
- [Fluxo Completo de Uma Requisição](#fluxo-completo-de-uma-requisição)
- [Como Executar](#como-executar)
- [Testando a API](#testando-a-api)

---

## 🎯 Visão Geral

A **Arquitetura Hexagonal** (também conhecida como **Ports & Adapters**) é um padrão que:

- ✅ **Isola a lógica de negócio** das dependências externas (banco de dados, APIs, frameworks)
- ✅ **Facilita testes** - você pode testar regras de negócio sem depender de infraestrutura
- ✅ **Permite trocar implementações** - mudar de PostgreSQL para MongoDB? Apenas troque o adapter
- ✅ **Ideal para microserviços** - cada serviço é independente e bem estruturado

### Conceito Visual

```
┌───────────────────────────────────────────────────────┐
│          APLICAÇÃO (Hexágono)                         │
│                                                       │
│  ┌─────────────────────────────────────────────────┐  │
│  │   DOMÍNIO (Lógica de Negócio Pura)              │  │
│  │                                                 │  │
│  │  Order.java        ← Entidades (modelos)        │  │
│  │  OrderService.java ← Casos de uso (regras)      │  │
│  │                                                 │  │
│  └─────────────────────────────────────────────────┘  │ 
│              ▲                    ▲                   │
│         Portas de Entrada    Portas de Saída          │
│         (como entrar)        (como sair)              │
│              │                    │                   │
└──────────────┼────────────────────┼───────────────────┘
               │                    │
        ┌──────┴────────┐    ┌─────┴──────────┐
        │  REST API     │    │  Banco Dados   │
        │  gRPC         │    │  APIs Externas │
        │  Mensageria   │    │  Cache         │
        └───────────────┘    └────────────────┘
```

---

## 📁 Estrutura do Projeto

```
orders-hexagonal/
├── pom.xml                                    # Configuração Maven
├── README.md                                  # Este arquivo
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── OrdersApplication.java         # Classe principal Spring Boot
│   │   │   │
│   │   │   ├── domain/                        # ⭐ DOMÍNIO (núcleo)
│   │   │   │   ├── model/                     # Entidades de negócio
│   │   │   │   │   ├── Order.java             # Entidade Order (Pedido)
│   │   │   │   │   └── OrderStatus.java       # Enum de status
│   │   │   │   │
│   │   │   │   ├── ports/                     # Portas (interfaces)
│   │   │   │   │   ├── input/                 # Como o mundo acessa
│   │   │   │   │   │   ├── CreateOrderPort.java
│   │   │   │   │   │   └── ConfirmOrderPort.java
│   │   │   │   │   │
│   │   │   │   │   └── output/                # Como o domínio acessa o mundo
│   │   │   │   │       ├── OrderRepositoryPort.java
│   │   │   │   │       └── PaymentPort.java
│   │   │   │   │
│   │   │   │   └── service/                   # Casos de uso
│   │   │   │       └── OrderService.java      # Implementa as regras
│   │   │   │
│   │   │   ├── adapter/                       # 🔌 ADAPTADORES
│   │   │   │   ├── input/                     # Adaptadores de entrada
│   │   │   │   │   └── rest/
│   │   │   │   │       └── OrderController.java  # REST API
│   │   │   │   │
│   │   │   │   └── output/                    # Adaptadores de saída
│   │   │   │       ├── persistence/           # Banco de dados
│   │   │   │       │   ├── OrderEntity.java   # Entidade JPA
│   │   │   │       │   ├── OrderSpringJpaRepository.java
│   │   │   │       │   └── OrderJpaAdapter.java
│   │   │   │       │
│   │   │   │       └── external/              # APIs externas
│   │   │   │           ├── PaymentClientAdapter.java
│   │   │   │           └── PaymentMockAdapter.java
│   │   │   │
│   │   │   └── config/                        # ⚙️ CONFIGURAÇÃO
│   │   │       └── BeanConfig.java            # Beans Spring
│   │   │
│   │   └── resources/
│   │       └── application.properties         # Configurações
│   │
│   └── test/                                  # 🧪 Testes
│       └── java/com/example/
│
└── target/                                    # Artefatos compilados
```

### Legenda das Camadas

| Camada | O que é | Pode depender de |
|--------|---------|------------------|
| **Domain** | Lógica pura de negócio | ❌ NADA (totalmente independente) |
| **Ports** | Contratos/Interfaces | ❌ NADA (apenas abstrações) |
| **Adapters** | Implementações concretas | ✅ Frameworks (Spring, JPA, etc.) |
| **Config** | Configuração do Spring | ✅ Spring Framework |

---

## 📖 Explicação Linha a Linha

### 1. Configuração Maven (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
```

**Explicação:**
- `<?xml version="1.0" encoding="UTF-8"?>` - Declara que é um arquivo XML com encoding UTF-8
- `<project>` - Tag raiz do Maven, define o projeto
- `xmlns` - Define o namespace XML padrão do Maven
- `modelVersion` - Versão do modelo de POM (sempre 4.0.0)

```xml
    <groupId>com.example</groupId>
    <artifactId>orders-hexagonal</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
```

**Explicação:**
- `groupId` - Identificador único da organização/empresa (convenção: domínio reverso)
- `artifactId` - Nome do projeto/artefato (será o nome do JAR gerado)
- `version` - Versão atual do projeto
- `packaging` - Tipo de empacotamento (jar = arquivo executável Java)

```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>
```

**Explicação:**
- `<parent>` - Herda configurações do Spring Boot Parent POM
- **Por que?** O parent POM traz:
  - Versões compatíveis de todas as dependências Spring
  - Configurações padrão de plugins Maven
  - Gerenciamento automático de versões (não precisa especificar versão em cada dependência)
- `relativePath/>` - Diz ao Maven para buscar o parent no repositório remoto, não localmente

```xml
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>
```

**Explicação:**
- `java.version` - Define Java 21 como versão base
- `project.build.sourceEncoding` - Define UTF-8 como encoding padrão dos arquivos
- `maven.compiler.source` - Código fonte compatível com Java 21
- `maven.compiler.target` - Bytecode compilado para Java 21

```xml
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
```

**Explicação:**
- `spring-boot-starter-web` - Starter que inclui:
  - **Spring MVC** - Para criar REST APIs
  - **Tomcat embutido** - Servidor web incluso
  - **Jackson** - Para serialização JSON
  - **Validation** - Para validação de dados
- **Por que?** Um único starter traz tudo necessário para criar APIs REST

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
```

**Explicação:**
- `spring-boot-starter-data-jpa` - Starter que inclui:
  - **Hibernate** - Implementação JPA (ORM)
  - **Spring Data JPA** - Repositórios automáticos
  - **Jakarta Persistence API** - Especificação JPA
- **Por que?** Facilita o acesso a banco de dados sem SQL manual

```xml
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>
```

**Explicação:**
- `h2` - Banco de dados em memória para desenvolvimento/testes
- `scope>runtime</scope>` - Necessário apenas em tempo de execução, não em compilação
- **Por que H2?** 
  - Rápido para desenvolvimento
  - Não precisa instalar nada
  - Dados são resetados ao reiniciar (ideal para testes)

```xml
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
```

**Explicação:**
- `lombok` - Biblioteca que gera código automaticamente via anotações
- `optional>true</optional>` - Não é transitivo (não aparece para quem usar seu JAR)
- **O que Lombok faz?**
  - `@Value` → Gera getters, equals, hashCode, toString
  - `@RequiredArgsConstructor` → Gera construtor com campos final
  - `@Data` → Gera getters, setters, equals, hashCode, toString
- **Por que?** Reduz código boilerplate (repetitivo)

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
```

**Explicação:**
- `spring-boot-starter-test` - Inclui:
  - **JUnit 5** - Framework de testes
  - **Mockito** - Para criar mocks
  - **AssertJ** - Asserções fluentes
  - **Spring Test** - Utilitários de teste Spring
- `scope>test</scope>` - Usado apenas em testes, não em produção

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Explicação:**
- `spring-boot-maven-plugin` - Plugin que:
  - Empacota a aplicação como JAR executável
  - Permite rodar com `mvn spring-boot:run`
  - Inclui todas as dependências no JAR final (fat JAR)

---

### 2. Propriedades da Aplicação

**Arquivo:** `src/main/resources/application.properties`

```properties
spring.application.name=orders-hexagonal
```

**Explicação:**
- Define o nome da aplicação
- **Por que?** Útil para:
  - Logs (identifica qual aplicação está logando)
  - Service Discovery (Eureka, Consul)
  - Monitoramento (rastreamento distribuído)

```properties
server.port=8080
```

**Explicação:**
- Define a porta onde o servidor Tomcat embutido escuta
- Padrão é 8080, mas pode ser qualquer porta livre
- **Exemplo:** `server.port=9000` mudaria para porta 9000

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

**Explicação linha por linha:**
- `spring.datasource.url=jdbc:h2:mem:testdb`
  - `jdbc:h2` - Protocolo JDBC para banco H2
  - `mem` - Banco em memória (dados perdidos ao reiniciar)
  - `testdb` - Nome do banco de dados
  
- `spring.datasource.driverClassName=org.h2.Driver`
  - Define o driver JDBC do H2
  
- `spring.datasource.username=sa`
  - Usuário padrão (sa = system administrator)
  
- `spring.datasource.password=`
  - Senha vazia (não recomendado em produção!)

```properties
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

**Explicação:**
- Define o dialeto SQL específico do H2
- **Por que?** Cada banco tem particularidades:
  - PostgreSQL: `PostgreSQLDialect`
  - MySQL: `MySQL8Dialect`
  - Oracle: `OracleDialect`

```properties
spring.jpa.hibernate.ddl-auto=create-drop
```

**Explicação:**
- `create-drop` - Cria schema ao iniciar, dropa ao finalizar
- **Outras opções:**
  - `none` - Não faz nada (produção)
  - `validate` - Valida se schema está correto
  - `update` - Atualiza schema (cuidado em produção!)
  - `create` - Cria schema ao iniciar
- **⚠️ PRODUÇÃO:** Use `none` ou `validate`

```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**Explicação:**
- `show-sql=true` - Mostra SQL executado no console
- `format_sql=true` - Formata SQL para melhor leitura
- **Por que?** Útil para debug e aprendizado
- **⚠️ PRODUÇÃO:** Desative (`false`) por performance

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**Explicação:**
- `enabled=true` - Ativa interface web do H2
- `path=/h2-console` - URL para acessar: http://localhost:8080/h2-console
- **Por que?** Ver dados do banco visualmente
- **⚠️ PRODUÇÃO:** Sempre `false` (risco de segurança)

---

### 3. Classe Principal

**Arquivo:** `src/main/java/com/example/OrdersApplication.java`

```java
package com.example;
```

**Explicação:**
- Define o pacote (namespace) da classe
- **Convenção Java:** `com.empresa.projeto`
- **Por que?** Evita conflitos de nomes entre projetos

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
```

**Explicação:**
- `import` - Importa classes de outros pacotes
- `SpringApplication` - Classe que inicializa o Spring Boot
- `SpringBootApplication` - Anotação principal do Spring Boot

```java
@SpringBootApplication
```

**Explicação:**
- **Meta-anotação** que combina 3 anotações:
  
  1. **`@Configuration`** - Marca como classe de configuração
     - Permite definir `@Bean` methods
  
  2. **`@EnableAutoConfiguration`** - Ativa configuração automática
     - Spring detecta dependências e configura automaticamente
     - Exemplo: Vê `spring-boot-starter-web` → configura Tomcat
  
  3. **`@ComponentScan`** - Escaneia componentes no pacote e subpacotes
     - Procura classes com `@Component`, `@Service`, `@Repository`, `@Controller`
     - **Base:** Pacote onde está a classe anotada (`com.example`)

**Por que usar?** Simplifica configuração - uma anotação faz tudo!

```java
public class OrdersApplication {
```

**Explicação:**
- Classe pública principal da aplicação
- **Convenção:** NomeProjetoApplication

```java
    public static void main(String[] args) {
```

**Explicação:**
- Método `main` - ponto de entrada da aplicação Java
- `public` - Acessível de qualquer lugar
- `static` - Pode ser chamado sem instanciar a classe
- `void` - Não retorna nada
- `String[] args` - Argumentos da linha de comando

```java
        SpringApplication.run(OrdersApplication.class, args);
```

**Explicação linha por linha:**
- `SpringApplication.run(...)` - Inicializa o Spring Boot
- `OrdersApplication.class` - Referência à classe principal
- `args` - Repassa argumentos do terminal

**O que esse método faz internamente?**
1. Cria ApplicationContext (container Spring)
2. Escaneia componentes (@Component, @Service, etc.)
3. Configura automaticamente dependências
4. Inicia servidor Tomcat embutido
5. Registra beans no container
6. Inicia a aplicação

```java
    }
}
```

---

### 4. Domain Layer - Modelos

#### 4.1 Entidade Order

**Arquivo:** `src/main/java/com/example/domain/model/Order.java`

```java
package com.example.domain.model;
```

**Explicação:**
- `domain` - Camada de domínio (lógica de negócio)
- `model` - Entidades/modelos de negócio

```java
import lombok.AllArgsConstructor;
import lombok.Value;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
```

**Explicação dos imports:**
- `lombok.Value` - Cria classe imutável
- `lombok.AllArgsConstructor` - Gera construtor com todos os campos
- `BigDecimal` - Tipo para valores monetários (mais preciso que `double`)
- `LocalDateTime` - Data e hora sem timezone
- `UUID` - Identificador único universal

```java
@Value
```

**Explicação detalhada:**
- Anotação do Lombok que torna a classe **imutável**
- **O que gera automaticamente:**
  - Todos os campos são `private final`
  - Getters para todos os campos (sem setters!)
  - `equals()` baseado em todos os campos
  - `hashCode()` baseado em todos os campos
  - `toString()` com todos os campos
  - Classe é `final` (não pode ser herdada)

**Por que imutável?**
- ✅ Thread-safe (seguro em concorrência)
- ✅ Previsível (não muda acidentalmente)
- ✅ Facilita debug (estado constante)
- ✅ Melhor para programação funcional

```java
@AllArgsConstructor
```

**Explicação:**
- Gera construtor que recebe TODOS os campos como parâmetros
- **Exemplo gerado:**
```java
public Order(UUID id, String clientId, BigDecimal total, 
             OrderStatus status, LocalDateTime createdAt) {
    this.id = id;
    this.clientId = clientId;
    // ...
}
```

**Por que?** Permite criar instância completa de uma vez

```java
public class Order {
```

**Explicação:**
- Classe que representa um pedido (Order)
- **É uma entidade de domínio**, não uma entidade JPA!
- Contém apenas lógica de negócio, sem dependências de frameworks

```java
    UUID id;
    String clientId;
    BigDecimal total;
    OrderStatus status;
    LocalDateTime createdAt;
```

**Explicação campo por campo:**

- `UUID id` - Identificador único do pedido
  - **Por que UUID?** Globalmente único, não precisa de banco para gerar
  
- `String clientId` - Identificador do cliente
  - **Por que String?** Flexível (pode ser UUID, CPF, email, etc.)
  
- `BigDecimal total` - Valor total do pedido
  - **Por que BigDecimal?** Precisão decimal exata (importante para dinheiro!)
  - ❌ **Nunca use** `double` ou `float` para dinheiro (erros de arredondamento)
  
- `OrderStatus status` - Status atual do pedido
  - **Enum** garante apenas valores válidos
  
- `LocalDateTime createdAt` - Data/hora de criação
  - **Por que LocalDateTime?** Sem timezone (útil se timezone for irrelevante)
  - **Alternativa:** `ZonedDateTime` ou `Instant`

**🔍 Observação:** Todos os campos são `final` implicitamente por causa do `@Value`

```java
    public static Order create(String clientId, BigDecimal total) {
```

**Explicação:**
- **Factory Method** (Padrão de Projeto)
- `static` - Método de classe (não precisa de instância)
- **Nome:** `create` deixa claro que é criação, não construtor simples

**Por que Factory Method?**
- ✅ Encapsula lógica de criação
- ✅ Garante que objeto é criado em estado válido
- ✅ Pode ter nomes descritivos (`createPending`, `createFromDto`, etc.)
- ✅ Pode aplicar regras de negócio na criação

```java
        return new Order(
            UUID.randomUUID(),
            clientId,
            total,
            OrderStatus.PENDING,
            LocalDateTime.now()
        );
```

**Explicação linha por linha:**

- `UUID.randomUUID()` - Gera ID único aleatório
  - **Por que aqui?** Domínio decide o ID, não o banco
  
- `clientId` - Recebido como parâmetro
  
- `total` - Recebido como parâmetro
  
- `OrderStatus.PENDING` - **REGRA DE NEGÓCIO:**
  - Todo pedido novo SEMPRE inicia como `PENDING`
  - Não pode criar pedido já confirmado
  
- `LocalDateTime.now()` - Data/hora atual da criação
  - **Por que aqui?** Domínio registra o momento da criação

**🎯 Importante:** Esse método **força** um estado inicial consistente

```java
    }
```

```java
    public Order confirm() {
```

**Explicação:**
- **Business Method** (Método de negócio)
- Não é setter! É uma **transição de estado**
- **Imutabilidade:** Retorna NOVA instância, não modifica a atual

```java
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order cannot be confirmed in status: " + status);
        }
```

**Explicação:**
- **REGRA DE NEGÓCIO:** Só pode confirmar pedido `PENDING`
- `IllegalStateException` - Exceção indicando estado inválido
- **Por que validar?** Domínio protege suas próprias regras

**Exemplo de uso:**
```java
Order pending = Order.create("client-1", BigDecimal.valueOf(100));
Order confirmed = pending.confirm(); // ✅ OK

Order alreadyConfirmed = confirmed;
alreadyConfirmed.confirm(); // ❌ Lança exceção!
```

```java
        return new Order(id, clientId, total, OrderStatus.CONFIRMED, createdAt);
```

**Explicação:**
- Cria NOVA instância com status `CONFIRMED`
- **Mantém:** `id`, `clientId`, `total`, `createdAt`
- **Muda:** apenas `status`
- **Imutabilidade:** Original permanece inalterado

```java
    }
}
```

---

#### 4.2 Enum OrderStatus

**Arquivo:** `src/main/java/com/example/domain/model/OrderStatus.java`

```java
package com.example.domain.model;

public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}
```

**Explicação completa:**

- `enum` - Tipo especial que define conjunto fixo de constantes
- **Por que enum?**
  - ✅ Type-safe (não aceita `status = "INVALID"`)
  - ✅ Autocomplete no IDE
  - ✅ Impossível ter valor inválido
  - ✅ Bom para `switch` statements

**Estados do pedido:**
- `PENDING` - Criado, aguardando confirmação
- `CONFIRMED` - Pagamento aprovado
- `SHIPPED` - Em trânsito
- `DELIVERED` - Entregue ao cliente
- `CANCELLED` - Cancelado

**Transições válidas:**
```
PENDING → CONFIRMED → SHIPPED → DELIVERED
   ↓
CANCELLED
```

---

### 5. Domain Layer - Portas

#### 5.1 Portas de Entrada (Input Ports)

**O que são?** Interfaces que definem **como o mundo externo acessa** a lógica de negócio.

##### CreateOrderPort

**Arquivo:** `src/main/java/com/example/domain/ports/input/CreateOrderPort.java`

```java
package com.example.domain.ports.input;
```

**Explicação:**
- `ports/input` - Portas de entrada (entrada de dados na aplicação)

```java
import com.example.domain.model.Order;
import java.math.BigDecimal;

public interface CreateOrderPort {
    Order create(String clientId, BigDecimal total);
}
```

**Explicação linha por linha:**

- `public interface` - Contrato público (não implementação)
- `CreateOrderPort` - Nome claro do que faz (porta para criar pedido)
- `Order create(...)` - Método que cria e retorna um pedido

**Por que interface?**
- ✅ Desacopla quem chama de quem implementa
- ✅ Facilita testes (mock da interface)
- ✅ Permite múltiplas implementações
- ✅ Quem usa não precisa saber "como" é feito

**Quem usa?**
- `OrderController` (REST)
- `OrderGrpcAdapter` (gRPC - se implementar)
- `OrderEventListener` (Messaging - se implementar)

**Quem implementa?**
- `OrderService` (caso de uso)

---

##### ConfirmOrderPort

**Arquivo:** `src/main/java/com/example/domain/ports/input/ConfirmOrderPort.java`

```java
package com.example.domain.ports.input;

import com.example.domain.model.Order;

public interface ConfirmOrderPort {
    Order confirm(String orderId);
}
```

**Explicação:**
- Similar ao `CreateOrderPort`
- Recebe ID do pedido existente
- Retorna pedido confirmado

**Por que separar em duas interfaces?**
- ✅ **Interface Segregation Principle** (SOLID)
- ✅ Cliente usa apenas o que precisa
- ✅ Mudanças isoladas

---

#### 5.2 Portas de Saída (Output Ports)

**O que são?** Interfaces que definem **como o domínio se comunica com o mundo externo**.

##### OrderRepositoryPort

**Arquivo:** `src/main/java/com/example/domain/ports/output/OrderRepositoryPort.java`

```java
package com.example.domain.ports.output;
```

**Explicação:**
- `ports/output` - Portas de saída (domínio acessa mundo externo)

```java
import com.example.domain.model.Order;
import java.util.Optional;

public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(String id);
    void delete(String id);
}
```

**Explicação método por método:**

```java
Order save(Order order);
```
- Salva pedido (criar novo ou atualizar existente)
- **Retorna:** Order salvo (pode ter campos atualizados pelo banco)
- **Por que retornar?** Banco pode gerar dados (timestamps, etc.)

```java
Optional<Order> findById(String id);
```
- Busca pedido por ID
- **`Optional<Order>`** - Pode existir ou não
- **Por que Optional?**
  - ✅ Força tratamento de ausência
  - ✅ Evita `NullPointerException`
  - ✅ Código mais seguro

**Uso correto:**
```java
Optional<Order> result = repository.findById("id-123");

// ✅ Correto - trata ambos os casos
result.ifPresent(order -> System.out.println(order));

// ✅ Correto - lança exceção se não existir
Order order = result.orElseThrow(() -> new RuntimeException("Not found"));

// ❌ Errado - derrota o propósito do Optional
Order order = result.get(); // Pode lançar NoSuchElementException
```

```java
void delete(String id);
```
- Deleta pedido por ID
- `void` - Não retorna nada
- **Por que String e não UUID?** Flexibilidade (pode ser UUID.toString())

**Por que essa interface?**
- ✅ Domínio não conhece JPA, MongoDB, etc.
- ✅ Trocar de banco = trocar implementação
- ✅ Testes usam implementação fake em memória

**Quem implementa?**
- `OrderJpaAdapter` (JPA/SQL)
- `OrderMongoAdapter` (MongoDB - se implementar)
- `OrderInMemoryAdapter` (Testes)

---

##### PaymentPort

**Arquivo:** `src/main/java/com/example/domain/ports/output/PaymentPort.java`

```java
package com.example.domain.ports.output;

import java.math.BigDecimal;

public interface PaymentPort {
    boolean process(String orderId, BigDecimal amount);
}
```

**Explicação:**

```java
boolean process(String orderId, BigDecimal amount);
```
- Processa pagamento em serviço externo
- **Parâmetros:**
  - `orderId` - ID do pedido (rastreamento)
  - `amount` - Valor a cobrar
- **Retorno:**
  - `true` - Pagamento aprovado
  - `false` - Pagamento recusado

**Por que boolean simples?**
- Domínio só precisa saber: aprovado ou não?
- Detalhes (motivo da recusa, etc.) são preocupação do adapter

**Quem implementa?**
- `PaymentClientAdapter` - Chama API real (Stripe, PagSeguro, etc.)
- `PaymentMockAdapter` - Mock para testes/desenvolvimento

---

### 6. Domain Layer - Serviços

**O que é?** Classe que implementa **casos de uso** (regras de negócio).

**Arquivo:** `src/main/java/com/example/domain/service/OrderService.java`

```java
package com.example.domain.service;
```

**Explicação:**
- `service` - Camada de serviços (orquestra lógica de negócio)

```java
import com.example.domain.model.Order;
import com.example.domain.ports.input.CreateOrderPort;
import com.example.domain.ports.input.ConfirmOrderPort;
import com.example.domain.ports.output.OrderRepositoryPort;
import com.example.domain.ports.output.PaymentPort;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
```

**Explicação dos imports:**
- `Order` - Entidade de domínio
- `CreateOrderPort`, `ConfirmOrderPort` - Portas que essa classe implementa
- `OrderRepositoryPort`, `PaymentPort` - Portas que essa classe USA
- `RequiredArgsConstructor` - Lombok gera construtor com campos final
- `BigDecimal` - Tipo para valores monetários

```java
@RequiredArgsConstructor
```

**Explicação detalhada:**
- Anotação do Lombok
- **Gera construtor** com todos os campos `final`
- **Código gerado automaticamente:**
```java
public OrderService(OrderRepositoryPort repository, PaymentPort paymentPort) {
    this.repository = repository;
    this.paymentPort = paymentPort;
}
```

**Por que?**
- ✅ Menos código boilerplate
- ✅ Injeção de dependências via construtor (recomendado pelo Spring)
- ✅ Campos final garantem imutabilidade das dependências

```java
public class OrderService implements CreateOrderPort, ConfirmOrderPort {
```

**Explicação:**
- `OrderService` - Implementa os casos de uso de Order
- `implements CreateOrderPort, ConfirmOrderPort` - **Implementa as portas de entrada**
- **Por que implementar as portas?**
  - Garante que o contrato está sendo cumprido
  - Permite injetar como interface (`CreateOrderPort port`)

```java
    private final OrderRepositoryPort repository;
    private final PaymentPort paymentPort;
```

**Explicação:**
- `final` - Dependências são imutáveis (definidas no construtor)
- `OrderRepositoryPort` - Porta de saída para persistência
- `PaymentPort` - Porta de saída para pagamento

**🎯 Inversão de Dependência:**
- Service depende de **abstrações** (interfaces), não implementações
- Quem injeta a implementação? Spring via `BeanConfig`

```java
    @Override
    public Order create(String clientId, BigDecimal total) {
```

**Explicação:**
- `@Override` - Indica que implementa método da interface
- **Por que?** Compila erro se assinatura não bater com interface

```java
        Order newOrder = Order.create(clientId, total);
```

**Explicação:**
- Chama factory method do domínio
- **Por que assim?** Lógica de criação está no domínio, não no service
- Order vem com:
  - ID gerado automaticamente
  - Status = PENDING
  - createdAt = agora

```java
        return repository.save(newOrder);
```

**Explicação:**
- Salva via porta de saída
- Service não sabe SE é JPA, MongoDB, etc.
- Retorna pedido salvo (pode ter campos atualizados)

```java
    }
```

---

```java
    @Override
    public Order confirm(String orderId) {
```

**Explicação:**
- Implementa porta `ConfirmOrderPort`
- **Caso de uso:** Confirmar um pedido existente

```java
        Order order = repository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
```

**Explicação linha por linha:**

- `repository.findById(orderId)` - Busca pedido, retorna `Optional<Order>`

- `.orElseThrow(...)` - Se `Optional` estiver vazio, lança exceção
  - **Alternativas:**
    - `.orElse(defaultValue)` - Retorna valor padrão
    - `.orElseGet(() -> createDefault())` - Chama função para criar padrão
  
- `() -> new IllegalArgumentException(...)` - Lambda que cria exceção
  - **Por que IllegalArgumentException?** ID inválido é problema do chamador

**🎯 Pattern Funcional:**
```java
// Ao invés de:
Optional<Order> opt = repository.findById(orderId);
if (!opt.isPresent()) {
    throw new IllegalArgumentException("Not found");
}
Order order = opt.get();

// Usamos (mais conciso):
Order order = repository.findById(orderId)
    .orElseThrow(() -> new IllegalArgumentException("Not found"));
```

```java
        boolean paymentApproved = paymentPort.process(orderId, order.getTotal());
```

**Explicação:**
- Chama porta de saída para processar pagamento
- **Parâmetros:**
  - `orderId` - Para rastreamento
  - `order.getTotal()` - Valor a cobrar
- **Retorno:** `true` se aprovado, `false` se recusado

**🎯 Importante:** Service não sabe se é:
- Stripe, PagSeguro, PayPal, etc.
- Chamada HTTP, gRPC, fila, etc.
- Apenas usa a abstração!

```java
        if (!paymentApproved) {
            throw new RuntimeException("Payment was declined");
        }
```

**Explicação:**
- **REGRA DE NEGÓCIO:** Não pode confirmar se pagamento falhou
- `RuntimeException` - Exceção não checada
- **Em produção:** Usar exceção customizada
```java
throw new PaymentDeclinedException("Payment declined for order: " + orderId);
```

```java
        Order confirmedOrder = order.confirm();
```

**Explicação:**
- Chama método de negócio da entidade
- **Imutabilidade:** `order` original permanece inalterado
- `confirmedOrder` é nova instância com status CONFIRMED
- **Por que assim?** Lógica de transição de estado está no domínio!

```java
        return repository.save(confirmedOrder);
```

**Explicação:**
- Persiste pedido confirmado
- **Por que save e não update?**
  - Save é idempotente (create ou update)
  - Repository decide se é INSERT ou UPDATE

```java
    }
}
```

---

### 7. Adapter Input - REST

**O que é?** Adaptador que expõe a lógica via **REST API HTTP**.

**Arquivo:** `src/main/java/com/example/adapter/input/rest/OrderController.java`

```java
package com.example.adapter.input.rest;
```

**Explicação:**
- `adapter/input` - Adaptadores de entrada
- `rest` - Tipo de adaptador (REST HTTP)

```java
import com.example.domain.ports.input.CreateOrderPort;
import com.example.domain.ports.input.ConfirmOrderPort;
import com.example.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
```

**Explicação dos imports Spring:**
- `HttpStatus` - Enum com códigos HTTP (200, 201, 404, etc.)
- `ResponseEntity` - Encapsula resposta HTTP (status + body + headers)
- `@RestController` - Marca classe como controller REST
- `@RequestMapping` - Define URL base
- `@PostMapping` - Mapeia POST
- `@PutMapping` - Mapeia PUT
- `@RequestBody` - Desserializa JSON do body
- `@PathVariable` - Extrai variável da URL

```java
@RestController
```

**Explicação:**
- Anotação do Spring MVC
- **Combina:**
  - `@Controller` - Marca como controller Spring
  - `@ResponseBody` - Retorno é serializado como JSON (não é view)

**O que faz?**
- Spring registra essa classe como bean
- Métodos retornam objetos que viram JSON automaticamente
- Exceções são convertidas em respostas HTTP de erro

**Alternativas:**
- `@Controller` - Para retornar views (Thymeleaf, JSP)
- `@RestController` - Para APIs REST (sempre retorna dados)

```java
@RequestMapping("/api/v1/orders")
```

**Explicação:**
- Define **URL base** para todos os endpoints da classe
- Todos os métodos herdam esse prefixo
- **Exemplo:**
  - `@PostMapping` → `POST /api/v1/orders`
  - `@GetMapping("/{id}")` → `GET /api/v1/orders/{id}`

**Por que `/api/v1/`?**
- `/api` - Prefixo comum para APIs
- `/v1` - Versionamento (v2, v3 futuramente)
- **Vantagem:** Pode ter v1 e v2 rodando simultaneamente

```java
@RequiredArgsConstructor
```

**Explicação:**
- Lombok gera construtor com campos final
- Spring injeta dependências automaticamente

```java
public class OrderController {
```

**Explicação:**
- Classe controller para endpoints de Order

```java
    private final CreateOrderPort createOrderPort;
    private final ConfirmOrderPort confirmOrderPort;
```

**Explicação:**
- Dependências são **PORTAS** (interfaces), não implementações
- **Inversão de Dependência:**
  - Controller não conhece `OrderService`
  - Usa apenas as abstrações
- **Quem injeta?** Spring, baseado no `BeanConfig`

```java
    @PostMapping
```

**Explicação:**
- Mapeia requisições `POST /api/v1/orders`
- **POST** = Criar novo recurso (convenção REST)

**Outras anotações similares:**
- `@GetMapping` - GET (buscar)
- `@PutMapping` - PUT (atualizar completo)
- `@PatchMapping` - PATCH (atualizar parcial)
- `@DeleteMapping` - DELETE (remover)

```java
    public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
```

**Explicação linha por linha:**

- `ResponseEntity<OrderResponse>` - Retorno encapsulado
  - Permite controlar status HTTP (200, 201, 400, etc.)
  - Body tipado (OrderResponse)
  - Headers customizados
  
- `@RequestBody` - Desserializa JSON do body HTTP para objeto Java
  - **Exemplo de requisição:**
  ```json
  {
    "clientId": "client-123",
    "total": 150.50
  }
  ```
  - Spring automaticamente converte para `CreateOrderRequest`
  
- `CreateOrderRequest request` - DTO (Data Transfer Object)
  - **Por que DTO?** Separar contrato HTTP de entidade de domínio

**O que Spring faz automaticamente:**
1. Lê JSON do body da requisição
2. Converte para `CreateOrderRequest` (Jackson)
3. Valida (se tiver anotações `@Valid`)
4. Chama o método passando o objeto

```java
        Order order = createOrderPort.create(request.clientId(), request.total());
```

**Explicação:**
- Chama porta de entrada com dados do DTO
- **Records (Java 14+):** `request.clientId()` em vez de `getClientId()`
- Retorna `Order` (entidade de domínio)

```java
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrderResponse.from(order));
```

**Explicação linha por linha:**

- `ResponseEntity.status(HttpStatus.CREATED)` - Define status 201 (Created)
  - **Por que 201?** Convenção REST para criação bem-sucedida
  - **Alternativas:**
    - `HttpStatus.OK` - 200 (sucesso genérico)
    - `HttpStatus.BAD_REQUEST` - 400 (erro do cliente)
    - `HttpStatus.NOT_FOUND` - 404 (não encontrado)

- `.body(OrderResponse.from(order))` - Define body da resposta
  - **Importante:** Não retorna `Order` diretamente!
  - Converte para `OrderResponse` (DTO de resposta)
  - **Por que?** Separar entidade interna de contrato HTTP

**Resposta HTTP final:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "clientId": "client-123",
  "total": 150.50,
  "status": "PENDING"
}
```

```java
    }
```

---

```java
    @PutMapping("/{id}/confirm")
```

**Explicação:**
- Mapeia requisições `PUT /api/v1/orders/{id}/confirm`
- `{id}` - Variável na URL (path variable)
- **Exemplo:** `PUT /api/v1/orders/123-456/confirm`

**Por que PUT?**
- PUT = Atualizar/modificar recurso (convenção REST)
- **Alternativa:** `POST /api/v1/orders/{id}/confirm` (também válido)

```java
    public ResponseEntity<OrderResponse> confirm(@PathVariable String id) {
```

**Explicação:**

- `@PathVariable String id` - Extrai `{id}` da URL
  - **Exemplo:** `PUT /orders/abc-123/confirm` → `id = "abc-123"`
  
- **Outras formas:**
```java
// Nome diferente
@PathVariable("id") String orderId

// Múltiplas variáveis
@GetMapping("/users/{userId}/orders/{orderId}")
public void get(@PathVariable String userId, @PathVariable String orderId)
```

```java
        Order order = confirmOrderPort.confirm(id);
        return ResponseEntity.ok(OrderResponse.from(order));
```

**Explicação:**
- `confirmOrderPort.confirm(id)` - Chama porta
- `ResponseEntity.ok(...)` - Atalho para status 200 (OK)
  - Equivalente a: `ResponseEntity.status(HttpStatus.OK).body(...)`

```java
    }
```

---

#### DTOs (Data Transfer Objects)

```java
    record CreateOrderRequest(String clientId, BigDecimal total) {}
```

**Explicação:**

- `record` - Classe de dados imutável (Java 14+)
- **Gera automaticamente:**
  - Construtor: `new CreateOrderRequest("client-1", BigDecimal.TEN)`
  - Getters: `request.clientId()`, `request.total()`
  - `equals()`, `hashCode()`, `toString()`
  
- **Por que record?**
  - ✅ Menos código que classe tradicional
  - ✅ Imutável por padrão
  - ✅ Sintaxe limpa

**Equivalente tradicional:**
```java
public class CreateOrderRequest {
    private final String clientId;
    private final BigDecimal total;
    
    public CreateOrderRequest(String clientId, BigDecimal total) {
        this.clientId = clientId;
        this.total = total;
    }
    
    public String getClientId() { return clientId; }
    public BigDecimal getTotal() { return total; }
    // + equals, hashCode, toString
}
```

**Por que DTO separado?**
- ✅ Contrato HTTP independente do domínio
- ✅ Domínio pode mudar sem quebrar API
- ✅ Validações específicas de HTTP (`@NotNull`, `@Min`, etc.)

---

```java
    record OrderResponse(String id, String clientId, BigDecimal total, String status) {
```

**Explicação:**
- DTO de resposta HTTP
- **Campos:**
  - `String id` - UUID convertido para String (mais fácil em JSON)
  - `String status` - Enum convertido para String

```java
        static OrderResponse from(Order order) {
```

**Explicação:**
- **Factory method estático** para conversão
- `static` - Pertence ao record, não à instância

**Por que factory method?**
- ✅ Centraliza conversão Domain → DTO
- ✅ Nome descritivo (`from`, `of`, `fromDomain`)
- ✅ Reutilizável

```java
            return new OrderResponse(
                order.getId().toString(),
                order.getClientId(),
                order.getTotal(),
                order.getStatus().name()
            );
```

**Explicação conversões:**
- `order.getId().toString()` - UUID → String
- `order.getStatus().name()` - Enum → String ("PENDING", "CONFIRMED", etc.)
- Demais campos copiados diretamente

```java
        }
    }
}
```

---

### 8. Adapter Output - Persistência

**O que é?** Adaptador que implementa persistência usando **JPA/Hibernate**.

#### 8.1 OrderEntity (Entidade JPA)

**Arquivo:** `src/main/java/com/example/adapter/output/persistence/OrderEntity.java`

```java
package com.example.adapter.output.persistence;
```

**Explicação:**
- `adapter/output/persistence` - Adaptador de saída para persistência

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

**Explicação dos imports JPA:

```java
@Repository
```

**Explicação:**

- Marca como componente de persistência Spring
- **O que faz?**
  - Spring registra como bean
  - Traduz exceções de persistência para Spring DataAccessException
  - Permite injeção em outras classes
  
**Tecnicamente opcional aqui:** `JpaRepository` já é suficiente, mas `@Repository` deixa explícito

```java
public interface OrderSpringJpaRepository extends JpaRepository<OrderEntity, UUID> {
```

**Explicação linha por linha:**

- `interface` - Não precisa implementar! Spring Data gera automaticamente
- `extends JpaRepository<OrderEntity, UUID>` - Herda métodos CRUD
  - `OrderEntity` - Tipo da entidade
  - `UUID` - Tipo do ID

**Métodos herdados automaticamente:**

```java
// Criação/Atualização
save(OrderEntity entity)
saveAll(Iterable<OrderEntity> entities)

// Leitura
findById(UUID id) → Optional<OrderEntity>
findAll() → List<OrderEntity>
existsById(UUID id) → boolean
count() → long

// Deleção
deleteById(UUID id)
delete(OrderEntity entity)
deleteAll()
```

**Magia do Spring Data:**

```java
// Você escreve APENAS isto:
public interface OrderSpringJpaRepository extends JpaRepository<OrderEntity, UUID> {
}

// Spring GERA automaticamente:
public class OrderSpringJpaRepositoryImpl implements OrderSpringJpaRepository {
    @Override
    public OrderEntity save(OrderEntity entity) {
        // Código de persistência JPA gerado automaticamente
    }
    // ... todos os outros métodos
}
```

**Queries personalizadas (opcional):**

```java
public interface OrderSpringJpaRepository extends JpaRepository<OrderEntity, UUID> {
    // Spring gera SQL automaticamente baseado no nome do método!
    List<OrderEntity> findByClientId(String clientId);
    List<OrderEntity> findByStatus(OrderStatus status);
    List<OrderEntity> findByClientIdAndStatus(String clientId, OrderStatus status);
    
    // Ou com @Query manual:
    @Query("SELECT o FROM OrderEntity o WHERE o.total > :amount")
    List<OrderEntity> findExpensiveOrders(@Param("amount") BigDecimal amount);
}
```

```java
}
```

---

#### 8.3 OrderJpaAdapter

**Arquivo:** `src/main/java/com/example/adapter/output/persistence/OrderJpaAdapter.java`

```java
package com.example.adapter.output.persistence;

import com.example.domain.model.Order;
import com.example.domain.ports.output.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;
```

**Explicação:**

- Importa porta do domínio (`OrderRepositoryPort`)
- Importa entidade de domínio (`Order`)
- **Não** importa nada de JPA no domínio!

```java
@Component
```

**Explicação:**

- Marca como componente Spring genérico
- **Alternativas:**
  - `@Service` - Para lógica de negócio
  - `@Repository` - Para acesso a dados (mas aqui seria redundante)
  - `@Component` - Genérico, funciona para adapters

**Por que @Component?**

- Spring registra como bean
- Permite injeção em outras classes
- Detectado pelo `@ComponentScan`

```java
@RequiredArgsConstructor
```

**Explicação:**

- Lombok gera construtor com campos final
- Spring injeta `OrderSpringJpaRepository` automaticamente

```java
public class OrderJpaAdapter implements OrderRepositoryPort {
```

**Explicação:**

- **Implementa a porta** definida no domínio
- Spring injeta essa implementação quando alguém pede `OrderRepositoryPort`

**Inversão de Dependência:**

```
Domain (OrderRepositoryPort interface)
   ↑ depende
Adapter (OrderJpaAdapter implementation)
```

```java
    private final OrderSpringJpaRepository jpaRepository;
```

**Explicação:**

- Dependência do repositório Spring Data
- `final` - Injetado via construtor (imutável)
- **Tipo:** Repositório específico do Spring, não a porta!

---

```java
    @Override
    public Order save(Order order) {
```

**Explicação:**

- Implementa método da porta
- **Contrato:** Recebe e retorna `Order` (domínio)
- **NÃO** recebe ou retorna `OrderEntity`!

```java
        OrderEntity entity = OrderEntity.from(order);
```

**Explicação:**

- **Passo 1:** Converte Domain → Entity
- Usa factory method `from()`
- **Por que?** JPA trabalha com entities, não com objetos de domínio

```java
        OrderEntity saved = jpaRepository.save(entity);
```

**Explicação:**

- **Passo 2:** Salva no banco via Spring Data
- `save()` é idempotente:
  - Se ID não existe → INSERT
  - Se ID existe → UPDATE
- **Retorna:** Entity salva (pode ter campos atualizados pelo banco)

**SQL gerado (INSERT):**

```sql
INSERT INTO orders (id, client_id, total, status, created_at) 
VALUES (?, ?, ?, ?, ?);
```

**SQL gerado (UPDATE):**

```sql
UPDATE orders 
SET client_id = ?, total = ?, status = ?, created_at = ? 
WHERE id = ?;
```

```java
        return saved.toDomain();
```

**Explicação:**

- **Passo 3:** Converte Entity → Domain
- Retorna objeto de domínio
- **Por que?** Adapter esconde detalhes de JPA do domínio

**Fluxo completo:**

```
Order (domain)
    ↓ OrderEntity.from()
OrderEntity (JPA)
    ↓ jpaRepository.save()
Database (SQL INSERT/UPDATE)
    ↓ 
OrderEntity (retornado)
    ↓ .toDomain()
Order (domain)
```

```java
    }
```

---

```java
    @Override
    public Optional<Order> findById(String id) {
```

**Explicação:**

- Busca por ID
- **Parâmetro:** `String id` (flexível)
- **Retorno:** `Optional<Order>` (pode não existir)

```java
        return jpaRepository.findById(UUID.fromString(id))
```

**Explicação:**

- `UUID.fromString(id)` - Converte String → UUID
  - **Exemplo:** `"550e8400-e29b-41d4-a716-446655440000"` → UUID
  - **Lança:** `IllegalArgumentException` se formato inválido
  
- `jpaRepository.findById(...)` - Busca no banco
  - **Retorna:** `Optional<OrderEntity>`
  - **SQL gerado:**

  ```sql
  SELECT * FROM orders WHERE id = ?;
  ```

```java
            .map(OrderEntity::toDomain);
```

**Explicação:**

- **Map funcional:** Se Optional não está vazio, aplica função
- `OrderEntity::toDomain` - Method reference
  - Equivalente a: `.map(entity -> entity.toDomain())`
  
**Comportamento:**

```java
// Se encontrou:
Optional<OrderEntity> entity = Optional.of(entityFromDb);
Optional<Order> order = entity.map(e -> e.toDomain()); // Optional.of(order)

// Se não encontrou:
Optional<OrderEntity> entity = Optional.empty();
Optional<Order> order = entity.map(e -> e.toDomain()); // Optional.empty()
```

**Por que map?**

- ✅ Conciso (uma linha)
- ✅ Funcional (sem if/else)
- ✅ Safe (não lança exceção se vazio)

**Alternativa imperativa:**

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

```java
    }
```

---

```java
    @Override
    public void delete(String id) {
        jpaRepository.deleteById(UUID.fromString(id));
    }
```

**Explicação:**

- Simples delegação ao Spring Data
- `void` - Não retorna nada
- **SQL gerado:**

```sql
DELETE FROM orders WHERE id = ?;
```

**⚠️ Nota:** Se ID não existir, Spring lança `EmptyResultDataAccessException`

**Alternativa segura:**

```java
@Override
public void delete(String id) {
    jpaRepository.findById(UUID.fromString(id))
        .ifPresent(jpaRepository::delete);
}
```

```java
}
```

---

### 9. Adapter Output - Cliente Externo

#### 9.1 PaymentClientAdapter (Cliente Real)

**Arquivo:** `src/main/java/com/example/adapter/output/external/PaymentClientAdapter.java`

```java
package com.example.adapter.output.external;

import com.example.domain.ports.output.PaymentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
```

**Explicação dos imports:**

- `PaymentPort` - Porta que será implementada
- `RestTemplate` - Cliente HTTP do Spring
- `@Component` - Marca como bean Spring

```java
@Component
```

**Explicação:**

- Registra como bean Spring
- **Problema potencial:** Dois adapters implementam `PaymentPort`!
  - `PaymentClientAdapter`
  - `PaymentMockAdapter`
- **Solução:** `PaymentMockAdapter` usa `@ConditionalOnProperty`

```java
@RequiredArgsConstructor
public class PaymentClientAdapter implements PaymentPort {
    private final RestTemplate restTemplate;
    private static final String PAYMENT_SERVICE_URL = "http://localhost:8081/payments";
```

**Explicação:**

- `RestTemplate` - Cliente HTTP para chamadas REST
  - Alternativa moderna: `WebClient` (reativo)
  
- `static final String` - Constante da URL do serviço
  - **Em produção:** Vir de `application.properties`

  ```java
  @Value("${payment.service.url}")
  private String paymentServiceUrl;
  ```

```java
    @Override
    public boolean process(String orderId, BigDecimal amount) {
```

**Explicação:**

- Implementa porta de pagamento
- **Contrato:** Retorna `true` se aprovado, `false` se recusado

```java
        try {
```

**Explicação:**

- Try-catch para tratar falhas de rede/API
- **Por que?** Chamadas externas podem falhar:
  - Serviço indisponível
  - Timeout
  - Resposta inválida

```java
            PaymentRequest request = new PaymentRequest(orderId, amount);
```

**Explicação:**

- Cria DTO para enviar ao serviço externo
- `record` define estrutura simples

```java
            PaymentResponse response = restTemplate.postForObject(
                PAYMENT_SERVICE_URL,
                request,
                PaymentResponse.class
            );
```

**Explicação linha por linha:**

- `restTemplate.postForObject(...)` - Faz POST HTTP e retorna objeto
  
- **Parâmetros:**
  1. `PAYMENT_SERVICE_URL` - URL destino
  2. `request` - Body da requisição (serializado para JSON)
  3. `PaymentResponse.class` - Tipo esperado na resposta (desserializado de JSON)

**O que acontece internamente:**

1. Serializa `request` para JSON:

   ```json
   {
     "orderId": "123",
     "amount": 100.50
   }
   ```

2. Faz POST HTTP:

   ```http
   POST http://localhost:8081/payments
   Content-Type: application/json
   
   {"orderId":"123","amount":100.50}
   ```

3. Recebe resposta JSON:

   ```json
   {"approved": true}
   ```

4. Desserializa para `PaymentResponse`

**Alternativas do RestTemplate:**

```java
// POST sem retorno
restTemplate.postForLocation(url, request); // Retorna URI (Location header)

// POST com ResponseEntity (acesso a headers/status)
ResponseEntity<PaymentResponse> response = restTemplate.postForEntity(url, request, PaymentResponse.class);

// Métodos para outros verbos HTTP
restTemplate.getForObject(url, Type.class);
restTemplate.put(url, request);
restTemplate.delete(url);
```

```java
            return response != null && response.approved();
```

**Explicação:**

- Verifica se resposta existe e foi aprovado
- **Por que `response != null`?** Resposta HTTP vazia retorna null
- `response.approved()` - Getter do record

```java
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
```

**Explicação:**

- Qualquer exceção = pagamento falhou
- `e.printStackTrace()` - Log básico (em produção, usar logger)
- **Retorna `false`** - Assume que pagamento foi recusado

**⚠️ Em produção, melhorar:**

```java
} catch (HttpClientErrorException e) {
    // Erro 4xx (problema na requisição)
    log.error("Payment client error: {}", e.getStatusCode());
    return false;
} catch (HttpServerErrorException e) {
    // Erro 5xx (problema no servidor)
    log.error("Payment server error: {}", e.getStatusCode());
    throw new PaymentServiceUnavailableException();
} catch (ResourceAccessException e) {
    // Timeout, conexão recusada
    log.error("Payment service unavailable", e);
    throw new PaymentServiceUnavailableException();
}
```

**Melhor ainda: Circuit Breaker (Resilience4j)**

```java
@CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
public boolean process(String orderId, BigDecimal amount) {
    // ...
}

private boolean paymentFallback(String orderId, BigDecimal amount, Exception e) {
    log.error("Payment circuit open, using fallback");
    return false;
}
```

```java
    }
```

---

```java
    record PaymentRequest(String orderId, BigDecimal amount) {}
    record PaymentResponse(boolean approved) {}
}
```

**Explicação:**

- DTOs internos do adapter
- **Privados:** Apenas este adapter usa
- **Records:** Sintaxe compacta para DTOs

---

#### 9.2 PaymentMockAdapter (Mock para Desenvolvimento)

**Arquivo:** `src/main/java/com/example/adapter/output/external/PaymentMockAdapter.java`

```java
package com.example.adapter.output.external;

import com.example.domain.ports.output.PaymentPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
```

**Explicação:**

- `@ConditionalOnProperty` - Ativa bean condicionalmente

```java
@Component
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "true")
```

**Explicação detalhada:**

- `@ConditionalOnProperty` - Bean só é criado se propriedade existir
  
- **Parâmetros:**
  - `name = "payment.mock.enabled"` - Nome da propriedade em `application.properties`
  - `havingValue = "true"` - Valor esperado para ativar

**Como funciona:**

**application.properties:**

```properties
# Mock ativado (desenvolvimento)
payment.mock.enabled=true
```

→ Spring cria `PaymentMockAdapter`, **NÃO** cria `PaymentClientAdapter`

```properties
# Mock desativado (produção)
payment.mock.enabled=false
# Ou simplesmente não definir a propriedade
```

→ Spring cria `PaymentClientAdapter`, **NÃO** cria `PaymentMockAdapter`

**Por que isso funciona?**

- Ambos implementam `PaymentPort`
- Spring só cria **um** dos dois
- Quem usa `PaymentPort` não sabe qual implementação está ativa!

**Outras opções de @Conditional:**

```java
@ConditionalOnClass(name = "ClassName") // Se classe existir
@ConditionalOnMissingBean(Type.class)   // Se bean não existir
@ConditionalOnProfile("dev")            // Se profile ativo
@ConditionalOnExpression("${prop} > 10") // Expressão SpEL
```

```java
public class PaymentMockAdapter implements PaymentPort {

    @Override
    public boolean process(String orderId, BigDecimal amount) {
        System.out.println("🔄 [MOCK] Processing payment: " + orderId + " - USD " + amount);
        return true;
    }
}
```

**Explicação:**

- **Sempre retorna `true`** - Simula aprovação
- `System.out.println` - Log simples (em produção, usar logger)
- **Útil para:**
  - Desenvolvimento local sem serviço externo
  - Testes de integração
  - Demos

**Versão melhorada:**

```java
@Override
public boolean process(String orderId, BigDecimal amount) {
    log.info("🔄 [MOCK] Processing payment: {} - USD {}", orderId, amount);
    
    // Simular falha ocasional
    if (Math.random() < 0.1) {
        log.warn("🔄 [MOCK] Payment declined");
        return false;
    }
    
    log.info("🔄 [MOCK] Payment approved");
    return true;
}
```

---

### 10. Configuração de Beans

**Arquivo:** `src/main/java/com/example/config/BeanConfig.java`

```java
package com.example.config;

import com.example.domain.ports.output.PaymentPort;
import com.example.domain.ports.output.OrderRepositoryPort;
import com.example.domain.service.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
```

**Explicação:**

- Importa portas e service do domínio
- Importa anotações do Spring

```java
@Configuration
```

**Explicação:**

- Marca classe como **fonte de configuração** Spring
- Spring procura métodos `@Bean` aqui
- **Equivalente:** XML antigo do Spring

```java
public class BeanConfig {
```

**Explicação:**

- Classe de configuração centralizada
- **Convenção:** Uma classe por tipo de configuração
  - `BeanConfig` - Beans gerais
  - `SecurityConfig` - Segurança
  - `DatabaseConfig` - Banco de dados

```java
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
```

**Explicação detalhada:**

- `@Bean` - Registra retorno como bean Spring
- Método público que retorna objeto
- **Nome do bean:** Nome do método (`restTemplate`)

**O que Spring faz:**

1. Chama método `restTemplate()`
2. Armazena retorno no ApplicationContext
3. Injeta em quem precisar:

```java
@RequiredArgsConstructor
public class PaymentClientAdapter {
    private final RestTemplate restTemplate; // Spring injeta aqui!
}
```

**Por que criar bean para RestTemplate?**

- `RestTemplate` é classe externa (não tem `@Component`)
- Queremos configurá-lo antes de usar
- Centraliza criação em um lugar

**Configuração avançada:**

```java
@Bean
public RestTemplate restTemplate() {
    RestTemplate template = new RestTemplate();
    
    // Configurar timeouts
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(5000);
    template.setRequestFactory(factory);
    
    // Adicionar interceptors
    template.setInterceptors(List.of(new LoggingInterceptor()));
    
    return template;
}
```

---

```java
    @Bean
    public OrderService orderService(
            OrderRepositoryPort repository,
            PaymentPort paymentPort) {
```

**Explicação linha por linha:**

- `@Bean` - Registra `OrderService` como bean

- **Parâmetros do método:**
  - `OrderRepositoryPort repository`
  - `PaymentPort paymentPort`
  
- **Injeção automática:** Spring busca beans desses tipos e passa como argumentos!

**Como Spring resolve:**

```
1. Preciso criar OrderService
2. Método pede OrderRepositoryPort
   → Busca bean que implementa: OrderJpaAdapter ✓
3. Método pede PaymentPort
   → Busca bean que implementa: PaymentClientAdapter ou PaymentMockAdapter ✓
4. Chama método com os beans encontrados
5. Armazena OrderService retornado
```

**Cenários:ente de persistência Spring

- **O que faz?**
  - Spring registra como bean# 🏗️ Arquitetura Hexagonal com Spring Boot - Guia Completo
