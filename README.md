# Syskewer API

Bem-vindo ao repositório backend do Syskewer. Este é o núcleo do nosso ecossistema de gestão inteligente para bares, restaurantes e espetinhos. O sistema foi desenhado para orquestrar operações complexas do setor gastronômico de forma simples, segura e escalável, automatizando desde a entrada do cliente até o fechamento do caixa.

---

## 1. Sobre o Projeto

O Syskewer atua como o cérebro do restaurante. Ele gerencia o salão, a cozinha e o fluxo de caixa, garantindo que os garçons consigam realizar pedidos de forma ágil e que a cozinha receba as ordens de preparo de forma organizada.

### Principais Funcionalidades

* **Autenticação e Autorização:** Login seguro via JWT (JSON Web Token) com controle rigoroso de acessos (cargos de Administrador e Garçom).
* **Gestão de Salão:** Controle em tempo real da ocupação de mesas e status das comandas.
* **Gestão de Pedidos:** Registro de pedidos com suporte a itens parciais, cálculo dinâmico de subtotais e congelamento de preços no momento da venda.
* **Integração com Cozinha (KDS):** Fila de preparo e acompanhamento do status dos itens para envio direto às praças de produção.
* **Catálogo de Produtos:** Gerenciamento centralizado de categorias, produtos e seus respectivos locais de preparo.
* **Fluxo de Caixa e Pagamentos:** Fechamento de comandas, aplicação de taxas de entrega, couvert artístico e divisão de contas.

---

## 2. Arquitetura e Estrutura de Pastas

Para manter o projeto manutenível a longo prazo, adotamos o padrão Domain-Driven Design (DDD). Em vez de agruparmos arquivos por tipo técnico (todos os controllers em uma única pasta, por exemplo), nós os organizamos pelo contexto de negócio.

### Árvore de Diretórios (Visão Geral)

    src/main/java/com/syskewer/api/
    ├── config/              # Configurações globais (Segurança, CORS, Beans e Inicialização)
    ├── exception/           # Interceptadores e manipuladores de erro globais
    ├── model/               # Entidades JPA (Nossas tabelas do banco de dados)
    │   ├── auth/
    │   ├── product/
    │   ├── salon/
    │   └── user/
    ├── repository/          # Interfaces de comunicação com o PostgreSQL
    ├── service/             # Camada de regras de negócio (O cérebro da aplicação)
    ├── controller/          # Endpoints REST (Portas de entrada da API)
    └── dto/                 # Objetos de Transferência de Dados (Isolamento das entidades)

### O papel de cada camada

* **Model:** Representa nossas tabelas no banco de dados e carrega o estado da aplicação. Todas as entidades principais herdam de uma classe base para padronização de identificadores.
* **Repository:** Camada de persistência que se comunica diretamente com o banco de dados.
* **DTO (Data Transfer Object):** Garante que não expomos nossa estrutura de banco de dados diretamente nas requisições. São as portas de entrada e saída de dados.
* **Service:** Onde as regras de negócio complexas vivem. É aqui que calculamos totais, validamos regras e disparamos ações.
* **Controller:** Recebe as requisições HTTP, delega o processamento para o Service correspondente e devolve a resposta com o Status Code adequado.

---

## 3. Fluxo da Aplicação

Para ilustrar a arquitetura em funcionamento, este é o ciclo de vida de uma requisição típica, como realizar um pedido para uma mesa:

1. **Requisição:** O cliente (aplicativo do garçom) faz um POST para a rota de pedidos enviando o ID da comanda e os itens desejados.
2. **Filtro de Segurança:** A requisição é interceptada pelo nosso filtro de segurança. Ele extrai o token JWT do cabeçalho, valida a assinatura matemática e autentica o usuário no contexto da aplicação.
3. **Controller:** O controlador de pedidos recebe o DTO validado e aciona o serviço de pedidos.
4. **Regra de Negócio (Service):**
   * O sistema verifica se a comanda informada está realmente aberta.
   * Busca os produtos no catálogo.
   * Executa uma regra de segurança financeira: captura o preço atual do produto e o congela no item do pedido. Isso garante que alterações futuras no cardápio não afetem o histórico desta mesa.
5. **Persistência:** O repositório salva o pedido e atualiza o saldo devedor da comanda.
6. **Resposta:** A API devolve um status de criação com os dados do pedido formatados.

---

## 4. Pré-requisitos

Antes de iniciar o desenvolvimento, certifique-se de ter as seguintes ferramentas instaladas em seu ambiente:

* **Java 21 (JDK):** Versão base do nosso projeto.
* **PostgreSQL:** Versão 14 ou superior recomendada.
* **Git:** Para versionamento de código.

Não é necessário instalar o Maven globalmente na máquina, pois utilizamos o Maven Wrapper embutido no repositório.

---

## 5. Configuração e Instalação

Siga este passo a passo para configurar o projeto localmente.

### Passo 1: Clonar o repositório

    git clone https://github.com/seu-usuario/syskewer-api.git
    cd syskewer-api

### Passo 2: Configurar o Banco de Dados

Abra o seu terminal do PostgreSQL ou sua ferramenta de gestão preferida (pgAdmin, DBeaver) e crie um banco de dados em branco para o projeto:

    CREATE DATABASE syskewer_db;

### Passo 3: Criar o arquivo de propriedades

Por questões rígidas de segurança, o arquivo de propriedades principal não é enviado para o repositório. Você precisará criá-lo manualmente na sua máquina.

1. Navegue até o diretório: `src/main/resources/`
2. Crie um arquivo chamado exatamente: `application.properties`
3. Cole o conteúdo abaixo no arquivo, substituindo as credenciais do banco e do e-mail pelas suas:

    # Configurações do Banco de Dados e Spring
    spring.application.name=api
    
    spring.datasource.url=jdbc:postgresql://localhost:5432/syskewer_db
    spring.datasource.username=seu_usuario_postgres
    spring.datasource.password=sua_senha_postgres
    
    spring.jpa.hibernate.ddl-auto=validate
    spring.jpa.show-sql=true
    
    # Configurações de Segurança e JWT
    # Este é um hash MD5 padrão utilizado para o ambiente de desenvolvimento local
    api.security.token.secret=SEU-TOKEN-(32 dígitos)
    api.security.token.expiration=172800
    
    # Configurações de E-mail (Serviço de Recuperação de Senhas)
    spring.mail.host=smtp.gmail.com
    spring.mail.port=587
    spring.mail.username=seu-email-aqui@gmail.com
    # Utilize uma Senha de App gerada nas configurações de segurança da sua conta
    spring.mail.password=sua_senha_de_app_gerada
    spring.mail.properties.mail.smtp.auth=true
    spring.mail.properties.mail.smtp.starttls.enable=true
    spring.mail.properties.mail.smtp.starttls.required=true
    
    # Credenciais Iniciais do Sistema
    # Define a senha para o primeiro usuário Administrador criado pela inicialização automática
    api.security.admin.default-password=sua-senha-aleatoria

---

## 6. Como Executar

Com o banco de dados criado e o arquivo de propriedades devidamente configurado, o sistema utilizará o Flyway para criar todas as tabelas automaticamente assim que for iniciado.

Para iniciar o servidor, abra o terminal na raiz do projeto e execute:

**No Linux ou macOS:**

    ./mvnw spring-boot:run

**No Windows:**

    mvnw.cmd spring-boot:run

O console exibirá o banner de inicialização do Spring Boot. Quando visualizar a mensagem indicando que a aplicação iniciou com sucesso, a API estará escutando na porta 8080.

---

## 7. Testes

O projeto acompanha uma suíte de testes automatizados para garantir a integridade dos serviços críticos (como gestão de usuários e produtos).

Para rodar todos os testes da aplicação:

**No Linux ou macOS:**

    ./mvnw test

**No Windows:**

    mvnw.cmd test