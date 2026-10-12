# Trabalho de Testes Funcionais - Syskewer API

**Integrantes do Grupo:**
1. [Nome Completo do Integrante 1]
2. [Nome Completo do Integrante 2]
3. [Nome Completo do Integrante 3]
4. [Nome Completo do Integrante 4]
5. [Nome Completo do Integrante 5]

---

## 1. Website ou API Escolhida para Testes
**Nome:** Syskewer API

## 2. Breve Descrição da API Escolhida
A Syskewer API é o backend de um sistema de gestão inteligente para estabelecimentos gastronômicos, como bares e restaurantes. Desenvolvida em Java 21 com Spring Boot 3 e PostgreSQL, a API é responsável por orquestrar operações complexas de forma segura. Suas principais funcionalidades incluem a gestão de comandas e mesas (abertura, fechamento, transferência, cálculo de couvert), gestão de pedidos e itens (lançamento, cancelamento, controle de estoque), controle de configurações de funcionamento da loja e gestão de usuários com autenticação e autorização via tokens JWT (JSON Web Tokens).

## 3. Tipos de Testes (Funcionais) que Podem Ser Aplicados
Para garantir que a API atenda a todos os requisitos e regras de negócio exigidos, os seguintes tipos de testes funcionais serão aplicados:

*   **Teste de Regras de Negócio (Business Logic Testing):** 
    *   *O que será feito:* Validar as lógicas centrais da aplicação. Serão feitos testes para garantir que: uma comanda não possa ser fechada se houver saldo devedor; o preço de um produto seja "congelado" no valor do momento do pedido, mesmo que mude no cardápio depois; e que o valor do couvert artístico seja dividido corretamente entre as comandas de uma mesma mesa.
*   **Teste de Integração de API (API Integration Testing):** 
    *   *O que será feito:* Testar fluxos completos e a comunicação entre os módulos. Um exemplo de fluxo que será testado: Autenticar (gerar JWT) -> Abrir comanda para uma mesa -> Lançar pedidos na comanda -> Registrar pagamento -> Fechar a comanda, verificando as alterações no banco a cada etapa.
*   **Teste de Controle de Acesso / Segurança Funcional:** 
    *   *O que será feito:* Verificar se os papéis de acesso (`ADMINISTRADOR` e `GARCOM`) são respeitados. Será testado, por exemplo, se um usuário com token de "Garçom" recebe um erro ao tentar cancelar um item de pedido que já está em preparo, já que essa é uma ação restrita a administradores.
*   **Teste de Validação e Tratamento de Erros:** 
    *   *O que será feito:* Enviar payloads com dados inválidos (ex: abrir comanda para uma mesa já ocupada, ou produto sem estoque) e verificar se o sistema (através do `GlobalExceptionHandler`) bloqueia a ação e retorna o status HTTP correto (como `400 Bad Request` ou `404 Not Found`) junto com a mensagem de erro padronizada.

## 4. Frameworks de Automação de Testes que Podem Ser Aplicados
Para automatizar os testes descritos acima, será utilizada a seguinte stack tecnológica baseada no ecossistema Java e ferramentas de API:

*   **JUnit 5 & Mockito:** 
    *   *O que será feito:* O JUnit será o executor principal de testes. O Mockito será utilizado nos testes de regras de negócio (camada de Serviço) para simular o banco de dados (mock dos repositórios). Isso permitirá isolar e testar rapidamente cálculos (como divisão de contas e coberturas) sem precisar de um banco de dados real rodando.
*   **RestAssured (com Spring Boot Test):** 
    *   *O que será feito:* O RestAssured será utilizado para automatizar os testes de integração e de controle de acesso. O framework fará requisições HTTP reais (GET, POST, PUT, DELETE) para os endpoints da Syskewer API, enviando headers com JWT e validando se o JSON de resposta e os Status Codes estão corretos conforme o esperado para cada fluxo.
*   **Testcontainers:** 
    *   *O que será feito:* Será acoplado aos testes de integração (junto com o RestAssured) para instanciar, de forma automatizada via Docker, um banco de dados PostgreSQL temporário durante a execução da esteira de testes. Isso garantirá que a API seja testada em um ambiente idêntico ao de produção, mas isolado.
*   **Postman / Newman:** 
    *   *O que será feito:* O Postman será utilizado para criar uma coleção completa de requisições de todos os endpoints e salvar cenários funcionais. O Newman (CLI do Postman) será usado para automatizar a execução dessa coleção, validando fluxos inteiros da API sem necessidade de escrever código Java, servindo também como uma documentação executável dos testes funcionais.
