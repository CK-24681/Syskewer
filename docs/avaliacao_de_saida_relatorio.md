# Atividade 6 — Avaliação de Saída (*Test Completion*) · Syskewer API

> **Disciplina:** Testes de Software · **Tema do Grupo:** nº 6 — *Avaliação de Saída (CTFL 4.0 / ISTQB)*  
> **Sistema sob Teste:** Syskewer API (Java 21 · Spring Boot 3.3.1 · Spring Security · JPA/Hibernate · H2/PostgreSQL)  
> **Integrantes da Equipe:** [Integrante 1] · [Integrante 2] · [Integrante 3] · [Integrante 4] · [Integrante 5]  
> **Data de Emissão:** 12 de Outubro de 2026 · **Status:** Documento Oficial de Fechamento de Testes

---

## 0. Resumo Executivo (Painel da Apresentação)

| Indicador / Pergunta | Resultado Medido no Projeto | Significado para o Negócio |
|---|---|---|
| **Os testes acabaram?** | **NÃO podemos declarar encerramento formal.** | 5 de 8 critérios foram atingidos, 1 parcialmente e 2 foram reprovados. |
| **Decisão recomendada (Veredito)** | 🔴 **NO-GO para Produção** | O sistema não pode entrar em produção devido a **riscos financeiros diretos** nos módulos de comanda e fiado. |
| **Total de Testes Automatizados** | **110 testes** (era 20 na linha de base) | **98 testes ativos 100% aprovados** (`BUILD SUCCESS`) + **12 testes de defeitos conhecidos** (`@Disabled` com evidência). |
| **Taxa de Sucesso dos Testes Ativos** | **100 %** (98 de 98 testes passando) | Todas as suítes ativas de regras de negócio e camada HTTP estão verdes. |
| **Cobertura de Código (Linhas - JaCoCo)** | **62,1 % Global** · **99 % a 100 % no Núcleo** | `BillService` (99,2%), `OrderService` (99,2%), `KitchenService` (100%), `SecurityConfig` (100%). |
| **Escore de Mutação (PITest)** | **55 % Global** · **84 % a 88 % no Núcleo** | `KitchenService` (88%), `BillService` (84%), `OrderService` (63%). Revela onde os testes são fortes e onde apenas "tocam" o código. |
| **Defeitos Encontrados** | **11 defeitos de software + 1 de ambiente resolvido** | **2 de Severidade Alta** (DEF-01 e DEF-03), 5 Médios, 4 Baixos. Todos com teste automatizado reproduzível. |
| **Ambiente de Testes** | **Estabilizado (100% Reprodutível)** | Resolvido com H2 em memória em modo PostgreSQL (`test`), eliminando dependência externa de infraestrutura. |

---

## 1. O que é e Para que Serve o Tópico 6 ("Avaliação de Saída")

### 1.1 Contexto no Processo Fundamental de Testes (CTFL 4.0 / ISTQB)
O padrão internacional **CTFL 4.0 (Certified Tester Foundation Level)** estabelece que o teste de software não é uma ação isolada que ocorre apenas no final da codificação, mas sim um processo estruturado em **7 atividades fundamentais**:

1. **Planejamento e Controle:** Define os objetivos, escopo, estratégias, recursos e os *Critérios de Saída*.
2. **Análise de Teste:** Analisa os requisitos e a base de testes para responder: *"O que testar?"*.
3. **Modelagem / Design de Teste:** Cria os casos de teste conceituais para responder: *"Como testar?"*.
4. **Implementação de Teste:** Codifica as suítes concretas, dados de teste e automações.
5. **Execução de Testes:** Roda os testes manuais e automatizados, comparando o resultado obtido contra o esperado.
6. **Avaliação de Saída (*Test Completion* / Avaliação dos Critérios de Parada):** **[TEMA DO NOSSO GRUPO]**
7. **Encerramento de Teste:** Arquiva o *testware*, registra lições aprendidas e finaliza o ciclo.

---

### 1.2 Para que Serve a Avaliação de Saída?
A **Avaliação de Saída** é a atividade em que a equipe de QA e a liderança técnica respondem à pergunta mais importante de um projeto de software:

> **"Já testamos o suficiente para liberar o software com segurança para o cliente?"**

Essa atividade serve para:
1. **Evitar a Falácia do "Acabou o Tempo":** Em projetos sem maturidade, os testes terminam simplesmente quando a data limite do calendário chega. A Avaliação de Saída combate isso exigindo comprovação matemática de **Critérios de Parada (Exit Criteria)**.
2. **Comparar Resultados Reais com Metas Pré-definidas:** Confronta as métricas coletadas na execução (cobertura JaCoCo, testes aprovados, escore de mutação PITest, defeitos pendentes) contra as metas estipuladas lá no Planejamento (Atividade 1).
3. **Avaliar Riscos Residuais:** Mapeia quais partes do sistema continuam não testadas ou possuem defeitos conhecidos não corrigidos.
4. **Produzir o Relatório Resumido de Testes (*Test Summary Report*):** Gera o artefato formal que documenta todo o esforço de teste, anomalias encontradas e conclusões para os *stakeholders*.
5. **Emitir a Recomendação de Decisão Executiva:** Recomenda formalmente se o projeto deve ter:
   - **GO (Liberado):** Todos os critérios de saída foram cumpridos.
   - **NO-GO (Bloqueado):** Critérios essenciais falharam (por exemplo, defeitos graves em aberto). O lançamento deve ser adiado até a correção.
   - **GO Condicional:** O software é liberado com restrições operacionais conhecidas e com aceite explícito e assinado de risco por parte do Product Owner / Diretoria.

---

### 1.3 Por que o Veredito do Syskewer é NO-GO?
No Syskewer API, nosso veredito técnico é categoricamente **NO-GO**, pelos seguintes motivos:
- **Violação do Critério CS-06 (Zero Defeitos Críticos/Altos):** Encontramos **2 defeitos de Severidade Alta** que impactam diretamente o caixa do restaurante:
  - **DEF-01:** Ao remover o couvert artístico em mesas com rateio, o cálculo divide o valor indevidamente, gerando **saldo devedor negativo (ex: R$ -0,83)**. O cliente sai sem pagar ou o restaurante fica com saldo corrompido.
  - **DEF-03:** A operação de rateio/racha de itens permite enviar partes da conta para comandas em status **Fiado (`IN_DEBT`)**, alterando uma dívida antiga que já havia sido arquivada e cobrada.
- **Violação do Critério CS-03 (Cobertura Global):** Atingimos **62,1 %** global contra a meta de 70 %, pois os módulos de recuperação de senha por e-mail, geração de tokens JWT e inicialização de dados não foram cobertos por testes automatizados.
- **Conclusão:** Liberar o Syskewer no estado atual geraria prejuízo financeiro e falhas graves de integridade contábil para o estabelecimento.

---

## 2. Simulação de um Projeto Real de 6 Meses

Para cumprir a exigência acadêmica de demonstrar a atividade ao longo de um ciclo de 6 meses, estruturamos a atuação da Avaliação de Saída nas seguintes etapas:

```
+---------------------------------------------------------------------------------------------------+
|                            CRONOGRAMA DE 6 MESES - PROJETO SYSKEWER                               |
+---------------------+-------------------------------+---------------------+-----------------------+
| MÊS 1               | MESES 2, 3 e 4                | MÊS 5               | MÊS 6                 |
| Planejamento        | Sprints Funcionais            | Ciclo de Sistema    | Avaliação Final       |
| Definição dos       | Avaliações Parciais           | Regressão Completa, | Relatório Resumido,   |
| Critérios de Saída  | de Saída ao fim de cada       | Testes de Segurança | Veredito GO/NO-GO     |
| e Ferramentas       | Sprint (Sprints 1 a 6)        | e Integração Web    | e Fechamento          |
+---------------------+-------------------------------+---------------------+-----------------------+
```

### O papel da Avaliação de Saída em cada fase:
- **Mês 1 (Planejamento & Baseline):** A equipe de teste define no Plano de Testes os 8 critérios de parada (CS-01 a CS-08) e instala as ferramentas de medição objetiva (Maven Surefire, JaCoCo, PITest).
- **Meses 2, 3 e 4 (Sprints de Desenvolvimento):** Ao término de cada sprint de 15 dias, a Avaliação de Saída parcial é executada como parte do *Definition of Done (DoD)*. Se a sprint não atinge os critérios daquela fatia funcional, o débito técnico é registrado.
- **Mês 5 (Testes de Sistema e Integração HTTP):** Execução da regressão completa, validação de segurança RBAC e simulação de fluxos integrados de pedidos e cozinha.
- **Mês 6 (Avaliação Final de Conclusão):** Reunião formal de encerramento, compilação de todas as métricas, homologação dos 110 testes, emissão do Relatório Resumido de Testes e tomada de decisão sobre o lançamento da versão 1.0.

---

## 3. Critérios de Saída Definidos para o Syskewer

| ID | Nome do Critério | Limiar Exigido (Meta) | Categoria | Justificativa Técnica |
|---|---|---|---|---|
| **CS-01** | **Execução dos Testes** | 100 % dos testes ativos | Execução | Nenhum teste planejado pode ser esquecido ou deixado sem rodar. |
| **CS-02** | **Taxa de Aprovação** | ≥ 95 % dos testes | Qualidade | Garante que o código compila e obedece aos contratos esperados. |
| **CS-03** | **Cobertura Global de Linhas** | ≥ 70 % | Abrangência | Garante que pelo menos 7 em cada 10 linhas do projeto foram exercitadas. |
| **CS-04** | **Cobertura do Núcleo de Negócio** | Linhas ≥ 90 % · Ramos ≥ 80 % | Confiabilidade | O coração financeiro (`service.salon`) não pode ter pontos cegos. |
| **CS-05** | **Escore de Mutação (PITest)** | ≥ 70 % nas classes críticas | Força do Teste | Mede se os testes realmente detectam erros se o código for adulterado. |
| **CS-06** | **Defeitos Críticos / Altos em Aberto** | **0 defeitos** | Risco de Negócio | Erros com impacto financeiro ou que quebram o fluxo bloqueiam o release. |
| **CS-07** | **Rastreabilidade de Regras de Negócio**| 100 % das regras do README | Conformidade | Tudo o que foi prometido na especificação deve possuir teste associado. |
| **CS-08** | **Reprodutibilidade do Ambiente** | Build 100% verde sem setup manual | Engenharia | O comando `./mvnw test` deve passar limpo em qualquer computador ou CI. |

---

## 4. Avaliação dos Critérios — Resultados Medidos

### 4.1 Comparativo da Execução: Antes vs. Depois

| Métrica de Execução | Linha de Base Inicial | Resultado Final da Atividade 6 | Evolução |
|---|---|---|---|
| **Classes de Teste** | 6 classes | **12 classes** | + 100 % |
| **Total de Testes Criados** | 20 testes | **110 testes** | + 450 % |
| **Testes Ativos Aprovados** | 19 testes | **98 testes** | + 415 % |
| **Testes de Defeitos Conhecidos** | 0 testes | **12 testes** (`@Disabled` documentados) | + 12 evidências |
| **Falhas no Build Padrão** | 1 erro de ambiente (`ENV-01`) | **0 erros / 0 falhas (`BUILD SUCCESS`)** | Ambiente corrigido com H2 |
| **Tempo de Execução da Suíte** | ≈ 24 segundos | **≈ 30 segundos** | Alta velocidade de feedback |

---

### 4.2 Cobertura de Código por Módulo (JaCoCo)

| Pacote / Classe Analisada | Cobertura de Linhas (Antes) | Cobertura de Linhas (Final) | Cobertura de Ramos (Final) | Status |
|---|---|---|---|---|
| **Global da Aplicação** | 25,0 % (291/1165) | **62,1 %** (723/1165) | **65,9 %** (191/290) | ❌ Abaixo da meta (70%) |
| **`BillService` (Comandas/Pagamentos)** | 36,2 % | **99,2 %** | **92,1 %** | ✅ Meta superada |
| **`OrderService` (Pedidos/Rateios)** | 30,0 % | **99,2 %** | **95,8 %** | ✅ Meta superada |
| **`KitchenService` (Esteira da Cozinha)** | 0,0 % | **100,0 %** | **88,9 %** | ✅ Meta superada |
| **`SecurityConfig` (Filtros de Segurança)** | 0,0 % | **100,0 %** | — | ✅ Totalmente coberto |
| **`GlobalExceptionHandler` (Erros HTTP)** | 0,0 % | **81,8 %** | — | ✅ Meta superada |
| **`BillController` / `OrderController`** | 0,0 % | **45,5 %** / **50,0 %** | — | 🟡 Cobertura parcial |
| **`UserService` / `MenuService`** | 39,7 % / 32,8 % | **39,7 %** / **32,8 %** | 22,7 % / 11,5 % | ❌ Não expandido |
| **`TokenService` / `PasswordResetService`**| 0,0 % | **0,0 %** | 0,0 % | ❌ Risco residual |

---

### 4.3 Análise de Testes de Mutação (PITest) — "Os testes são realmente fortes?"
O teste de mutação insere pequenas alterações propositais no bytecode (mutantes) para checar se a suíte de testes acusa a falha ("mata o mutante").

- **Mutantes Gerados:** 338 mutantes.
- **Mutantes Mortos:** 186 mutantes (55 % global).
- **Mutantes Sobreviventes:** 47 mutantes.
- **Mutantes sem Cobertura:** 105 mutantes.
- **Força Real dos Testes (Mutantes Mortos ÷ Cobertos):** **80 %**.

| Classe | Mutantes Mortos | Mutantes Sobreviventes | Escore de Mutação | Avaliação da Força do Teste |
|---|---|---|---|---|
| **`KitchenService`** | 15 | 2 | **88 %** | Excelente. Testes altamente eficazes contra mutações. |
| **`BillService`** | 108 | 12 | **84 %** | Muito Forte. Quase todos os desvios de cálculo são mortos. |
| **`OrderService`** | 47 | 27 | **63 %** | ⚠️ Alerta de Qualidade: 99% de cobertura, mas 63% de mutação. |

> **Lição Crítica para a Apresentação:** No `OrderService`, temos 99% de cobertura de linhas, mas os mutantes sobreviveram porque os testes validavam o **valor financeiro total** do pedido, mas deixavam de assinalar campos secundários mutados (como observações do prato, flags de `isToGo` e local de preparo). **Cobertura alta NÃO é sinônimo de teste rigoroso!**

---

### 4.4 Veredito Detalhado por Critério de Saída

| ID | Critério | Meta | Medido no Projeto | Veredito |
|---|---|---|---|---|
| **CS-01** | Execução de Testes | 100 % dos ativos | 98 de 98 testes ativos executados | ✅ **APROVADO** |
| **CS-02** | Taxa de Aprovação | ≥ 95 % | 100 % dos testes ativos (98/98) | ✅ **APROVADO** |
| **CS-03** | Cobertura Global | ≥ 70 % de linhas | 62,1 % atingido | ❌ **REPROVADO** |
| **CS-04** | Cobertura do Núcleo | Linhas ≥ 90% / Ramos ≥ 80% | Bill: 99/92 · Order: 99/96 · Kitchen: 100/89 | ✅ **APROVADO** |
| **CS-05** | Mutação do Núcleo | ≥ 70 % nas classes | Kitchen (88%) ✅ · Bill (84%) ✅ · Order (63%) ❌ | 🟡 **PARCIAL** |
| **CS-06** | Defeitos Críticos / Altos | 0 abertos | **2 defeitos Altos em aberto (DEF-01 e DEF-03)** | ❌ **REPROVADO** |
| **CS-07** | Rastreabilidade das Regras| 100 % das regras do README | Salão/Pedidos/Cozinha cobertos · Auth/Senha pendentes | 🟡 **PARCIAL** |
| **CS-08** | Ambiente Reprodutível | Build 100% verde | `BUILD SUCCESS` limpo com H2 em memória | ✅ **APROVADO** |

---

## 5. Catálogo Completo dos 110 Testes Executados

Abaixo está o inventário minucioso de **todos os 110 testes automatizados**, detalhando classe por classe, o nome de cada método, a regra de negócio validada e a situação do teste:

---

### 5.1 `BillServiceExtendedTest` (36 Testes — Regras Financeiras e Salão)
Esta suíte valida todas as regras de negócio de comandas, cálculos de saldos, taxas de serviço, couvert artístico, débitos de fiado e operações entre mesas.

| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 1 | `closeTab_withBalance_throws` | Impede o fechamento de uma comanda que ainda possui saldo devedor pendente. | Aprovado |
| 2 | `closeTab_paid_releasesTable` | Garante que, ao fechar uma conta quitada, a mesa física é liberada no salão se não houver outras comandas vinculadas. | Aprovado |
| 3 | `closeTab_paid_keepsTableOccupiedIfAnotherBillOpen` | Mantém a mesa como ocupada se outra comanda ainda estiver aberta nela. | Aprovado |
| 4 | `closeTab_alreadyClosed_throws` | Bloqueia a tentativa de fechar uma comanda que já se encontra com status fechado. | Aprovado |
| 5 | `closeTab_notFound_throws` | Retorna erro 404 de negócio (`ResourceNotFoundException`) se o ID da comanda não existir. | Aprovado |
| 6 | `toggleCoverCharge_singleBill_addsCover` | Adiciona o couvert artístico integral de R$ 5,00 quando existe apenas uma comanda na mesa. | Aprovado |
| 7 | `toggleCoverCharge_alreadyHasCover_throws` | Impede adicionar couvert artístico em uma conta que já possui couvert ativo. | Aprovado |
| 8 | `toggleCoverCharge_twoBills_splitsCover` | Divide o valor do couvert de R$ 5,00 igualmente entre 2 comandas da mesma mesa (R$ 2,50 para cada). | Aprovado |
| 9 | `toggleCoverCharge_threeBills_roundingHalfUp` | Valida a divisão de R$ 5,00 entre 3 comandas com arredondamento `HALF_UP` (R$ 1,67 para cada). | Aprovado |
| 10 | `toggleCoverCharge_billNotInDebt` | Bloqueia a cobrança ou alteração de couvert em comandas que foram arquivadas em fiado. | Aprovado |
| 11 | `removeCoverCharge_notFound_throws` | Retorna erro de recurso não encontrado ao tentar remover couvert de comanda inexistente. | Aprovado |
| 12 | `removeCoverCharge_withoutCover_throws` | Impede a remoção de couvert de uma conta que não possuía couvert cobrado. | Aprovado |
| 13 | `removeCoverCharge_singleBill_removesFull` | Remove o valor integral de R$ 5,00 e abate do total quando havia apenas uma conta. | Aprovado |
| 14 | `removeCoverCharge_twoBills_restoresRemaining` | Ao remover o couvert de uma das contas, reajusta a conta remanescente para o valor integral de R$ 5,00. | Aprovado |
| 15 | `archiveAsCredit_success_setsInDebt` | Arquiva uma comanda com saldo pendente como débito de fiado (`IN_DEBT`) e libera a mesa. | Aprovado |
| 16 | `archiveAsCredit_alreadyClosed_throws` | Impede arquivar como fiado uma conta que já está fechada. | Aprovado |
| 17 | `archiveAsCredit_alreadyInDebt_throws` | Impede arquivar como fiado uma conta que já se encontra em fiado. | Aprovado |
| 18 | `openTab_tableOccupied_throws` | Bloqueia a abertura de uma nova comanda em uma mesa que já está ocupada por outro grupo. | Aprovado |
| 19 | `openTab_tableNotFound_throws` | Bloqueia a abertura de comanda se o número da mesa informado não existir no cadastro do salão. | Aprovado |
| 20 | `openTab_tableFree_occupiesAndTrimsName` | Abre comanda em mesa livre, marca a mesa como ocupada e remove espaços em branco extras do nome do cliente. | Aprovado |
| 21 | `openTab_balcao_withoutName_setsDefault` | Ao abrir comanda do tipo Balcão sem fornecer nome, atribui automaticamente o nome 'Cliente Balcão'. | Aprovado |
| 22 | `registerPayment_partial_reducesBalance` | Registra pagamento parcial, reduz o saldo devedor e mantém a comanda aberta para novos lançamentos. | Aprovado |
| 23 | `registerPayment_full_closesTabAndReleasesTable` | Registra pagamento total, quita a dívida, fecha a comanda e libera a mesa física. | Aprovado |
| 24 | `registerPayment_amountGreaterThanBalance_throws` | Bloqueia tentativas de pagar um valor maior do que o saldo devedor atual da comanda. | Aprovado |
| 25 | `registerPayment_tabAlreadyClosed_throws` | Bloqueia tentativas de registrar pagamento em uma comanda que já está encerrada. | Aprovado |
| 26 | `registerPayment_withDiscount_adminRole_success` | Permite que usuários com papel de Administrador concedam desconto financeiro e quitem a conta. | Aprovado |
| 27 | `registerPayment_billNotFound_throws` | Retorna erro 404 ao tentar registrar pagamento em comanda inexistente. | Aprovado |
| 28 | `openDeliveryTab_appliesDeliveryFee` | Comanda de entrega nasce automaticamente com a taxa de entrega de R$ 5,00 somada ao saldo devedor. | Aprovado |
| 29 | `cancelTab_empty_deletesAndReleasesTable` | Permite o cancelamento de comanda vazia (sem consumo), excluindo-a e liberando a mesa. | Aprovado |
| 30 | `cancelTab_withItems_throws` | Bloqueia o cancelamento de comanda que já possui consumo lançado ou pedidos vinculados. | Aprovado |
| 31 | `transferTab_freeTable_transfersAndSwapsOccupation` | Transfere a comanda para uma mesa livre, ocupando a mesa de destino e liberando a mesa de origem. | Aprovado |
| 32 | `transferTab_occupiedOrNotFoundTable_throws` | Bloqueia a transferência de comanda se a mesa de destino estiver ocupada ou não existir. | Aprovado |
| 33 | `payGroupedTabs_withChange_calculatesChange` | Realiza o pagamento em lote de múltiplas comandas e calcula o troco corretamente se o valor pago for maior. | Aprovado |
| 34 | `payGroupedTabs_insufficientOrEmpty_throws` | Bloqueia pagamento agrupado se o montante pago for insuficiente para cobrir todas as contas selecionadas. | Aprovado |
| 35 | `reopenTab_freeTable_reoccupiesTable` | Reabre uma comanda fechada caso a mesa original ainda esteja livre, reocupando-a. | Aprovado |
| 36 | `reopenTab_notClosed_throws` | Bloqueia tentativa de reabertura em uma conta que já está aberta. | Aprovado |

---

### 5.2 `OrderServiceExtendedTest` (17 Testes — Pedidos, Estoque e Rateio)
Esta suíte valida as regras de lançamento de pedidos, proteção de preços contra aumentos de cardápio, controle de insumos e o algoritmo de divisão/racha de contas.

| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 37 | `placeOrder_storeClosed_throws` | Bloqueia novos pedidos se o estabelecimento estiver marcado como fechado nas configurações da loja. | Aprovado |
| 38 | `placeOrder_billNotFound` | Retorna erro 404 ao tentar lançar pedido em uma comanda inexistente. | Aprovado |
| 39 | `placeOrder_billClosedOrInDebt_throws` | Bloqueia lançamentos de novos itens em comandas fechadas ou arquivadas em fiado. | Aprovado |
| 40 | `placeOrder_freezesPrice_accumulatesTotal` | Congela o preço histórico do prato no atributo `soldPrice` e incrementa o total da conta. | Aprovado |
| 41 | `placeOrder_queuedStatus_withOrigin` | O pedido novo é cadastrado com status inicial `QUEUED` (na fila) e com a origem informada. | Aprovado |
| 42 | `placeOrder_inactiveOrNotFoundProduct_throws` | Bloqueia pedidos com produtos inexistentes ou com itens marcados como inativos no cardápio. | Aprovado |
| 43 | `placeOrder_outOfStock_throws` | Bloqueia pedidos caso algum dos ingredientes da ficha técnica esteja com estoque zerado/insuficiente. | Aprovado |
| 44 | `cancelOrderItem_garcom_queuedItem_ok` | Garçom pode cancelar itens que ainda estão na fila de espera (`QUEUED`), abatendo o valor da conta. | Aprovado |
| 45 | `cancelOrderItem_garcom_closedTab_throws` | Garçom é impedido de realizar estorno ou cancelamento de itens em comandas já fechadas. | Aprovado |
| 46 | `cancelOrderItem_notFound_throws` | Retorna erro 404 ao tentar cancelar um item de pedido que não existe no banco de dados. | Aprovado |
| 47 | `reduceQuantity_invalidValues_throws` | Bloqueia redução de quantidade se o valor informado for nulo, zero, negativo ou maior que o pedido. | Aprovado |
| 48 | `reduceQuantity_garcom_preparingItem_throws` | Garçom é impedido de reduzir a quantidade de um item que já entrou em preparo na chapa/cozinha. | Aprovado |
| 49 | `reduceQuantity_closedTab_adminRefundsPaid` | Administrador pode reduzir itens de conta já fechada, realizando o estorno do valor financeiro pago. | Aprovado |
| 50 | `splitItem_threeWays_exactSum` | Racha de item de R$ 30,00 entre 3 clientes: gera R$ 10,00 para cada um, preservando o valor total. | Aprovado |
| 51 | `splitItem_threeWays_withRemainder_preservesTotal` | Racha com dízima: R$ 10,00 entre 3 pessoas preserva a invariante contábil sem sumir centavos. | Aprovado |
| 52 | `splitItem_emptyTargetsOrClosedTab_throws` | Bloqueia operação de racha se a lista de contas destino estiver vazia ou com conta fechada. | Aprovado |
| 53 | `splitItem_itemNotFound_throws` | Retorna erro 404 ao tentar ratear um item que não existe. | Aprovado |

---

### 5.3 `KitchenServiceTest` (6 Testes — Esteira de Produção da Cozinha)
Valida o fluxo kanban da cozinha, impedindo retrocessos indevidos e filtrando pedidos por praça de preparo (bar, chapa, sobremesa).

| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 54 | `advance_forward_ok` | Permite o avanço de status do pedido na esteira de `QUEUED` para `PREPARING` e persiste no banco. | Aprovado |
| 55 | `advance_backward_throws` | Bloqueia retrocessos indevidos de status (por exemplo, tentar voltar de `DELIVERED` para `QUEUED`). | Aprovado |
| 56 | `advance_nullStatusOrNotFound_throws` | Rejeita requisições com status nulo ou para pedidos inexistentes. | Aprovado |
| 57 | `getQueue_filtersByPrepLocation` | Filtra a tela da cozinha por praça de preparo (ex: Chapa), ocultando pedidos de outras praças (Bar). | Aprovado |
| 58 | `getQueue_noFilter_formatsBalcaoAndAutoatendimento` | Na fila geral, formata comandas de balcão como 'Tab <id>' e pedidos sem garçom como 'Autoatendimento'.| Aprovado |
| 59 | `getQueue_tableOrder_showsTableNumber` | Na fila de pedidos de mesa, exibe claramente o número da mesa física para os cozinheiros. | Aprovado |

---

### 5.4 `ApiWebLayerTest` (19 Testes — Camada Web, HTTP, Segurança e Validação)
Testes de integração HTTP utilizando `MockMvc` e o contexto de segurança real do Spring Security.

| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 60 | `unauthenticated_isRejected` | Garante que requisições sem token JWT para rotas protegidas sejam rejeitadas (4xx) sem acionar o serviço.| Aprovado |
| 61 | `publicRoutes_doNotRequireToken` | Garante que as rotas públicas `/login` e `/password/forgot` não exijam token JWT. | Aprovado |
| 62 | `garcom_canListOpenBills` | Usuário com papel `GARCOM` devidamente autenticado consegue listar comandas abertas (HTTP 200). | Aprovado |
| 63 | `garcom_cannotCreateUser_isForbidden` | Garçom tentando criar usuário (`POST /users`) recebe HTTP 403 Forbidden pelo filtro de segurança. | Aprovado |
| 64 | `admin_canToggleStore` | Administrador consegue acessar com sucesso o endpoint de alternância do status da loja (HTTP 200). | Aprovado |
| 65 | `admin_canArchiveFiado` | Administrador consegue acessar com sucesso o endpoint de arquivamento de conta em fiado (HTTP 200). | Aprovado |
| 66 | `openTab_missingConsumptionType_returns400WithErrors` | Envio de abertura de conta sem tipo de consumo retorna HTTP 400 com lista de campos inválidos. | Aprovado |
| 67 | `openTab_negativeOrZeroTable_returns400` | Envio de mesa com número zero ou negativo é barrado pelo Bean Validation com HTTP 400. | Aprovado |
| 68 | `openTab_valid_returns201` | Abertura de conta válida com payload correto retorna HTTP 201 Created e o corpo da comanda. | Aprovado |
| 69 | `payment_zeroOrNegativeAmount_returns400` | Pagamento com valor zero ou negativo é barrado pelo Bean Validation com HTTP 400. | Aprovado |
| 70 | `payment_negativeDiscount_returns400` | Pagamento informando desconto financeiro negativo retorna HTTP 400 Bad Request. | Aprovado |
| 71 | `openDeliveryTab_missingFields_returns400` | Criação de delivery sem nome do cliente ou sem endereço retorna HTTP 400 apontando os campos. | Aprovado |
| 72 | `placeOrder_emptyItems_returns400` | Envio de pedido sem itens ou com quantidade zerada retorna HTTP 400 pelo Bean Validation. | Aprovado |
| 73 | `placeOrder_valid_returns201` | Pedido válido com itens e mesa correta retorna HTTP 201 Created. | Aprovado |
| 74 | `businessRuleException_mappedTo400` | Garante que exceções de negócio (`BusinessRuleException`) sejam capturadas e retornem HTTP 400 amigável.| Aprovado |
| 75 | `resourceNotFoundException_mappedTo404` | Garante que exceções de recurso inexistente (`ResourceNotFoundException`) retornem HTTP 404 Not Found. | Aprovado |
| 76 | `genericException_mappedTo500_withoutLeak` | Exceções inesperadas do sistema retornam HTTP 500 padronizado sem vazar o stack trace interno. | Aprovado |
| 77 | `cancelBill_success_returns204` | Cancelamento de conta com sucesso retorna HTTP 204 No Content sem corpo. | Aprovado |
| 78 | `transferBill_forwardsTargetTable` | Transferência de conta encaminha corretamente o número da mesa de destino para a camada de serviço. | Aprovado |

---

### 5.5 `KnownDefectsTest` (7 Testes — Evidências de Defeitos de Negócio `@Disabled`)
Testes automatizados que reproduzem fielmente os defeitos de produto encontrados durante os testes de regras de negócio.

| Nº | Método de Teste | Defeito Reproduzido / O que o Teste Revela | Situação |
|---|---|---|---|
| 79 | `removeCoverCharge_threeBills_eachRefundMatchesCharge` | **DEF-01 (Alta):** Ao remover couvert sucessivamente em mesa com rateio, gera saldo devedor negativo (R$ -0,83). | `@Disabled` |
| 80 | `removeCoverCharge_balcao_shouldRemoveCharge` | **DEF-02 (Média):** Remoção de couvert em conta de balcão não executa a baixa do valor. | `@Disabled` |
| 81 | `splitItem_targetInDebt_shouldThrow` | **DEF-03 (Alta):** Permite ratear prato para uma conta já arquivada em fiado (`IN_DEBT`), corrompendo a dívida. | `@Disabled` |
| 82 | `splitItem_targetContainsSourceBill_shouldThrow` | **DEF-04 (Baixa):** Aceita a própria comanda de origem na lista de rateio de um item. | `@Disabled` |
| 83 | `payGroupedTabs_missingId_shouldThrow` | **DEF-05 (Média):** Ignora em silêncio IDs inexistentes informados no pagamento agrupado de contas. | `@Disabled` |
| 84 | `toggleCoverCharge_threeBills_sumShouldEqualFiveReais` | **DEF-06 (Baixa):** Arredondamento de couvert dividido por 3 resulta em 3 x 1,67 = R$ 5,01 (um centavo a mais). | `@Disabled` |
| 85 | `registerPayment_negativeAmount_serviceShouldThrow` | **DEF-07 (Baixa):** O serviço aceita valor negativo se invocado diretamente sem o DTO da API. | `@Disabled` |

---

### 5.6 `WebLayerKnownDefectsTest` (5 Testes — Evidências de Defeitos HTTP/Segurança `@Disabled`)
Testes automatizados que reproduzem defeitos encontrados na camada de exposição de endpoints HTTP e segurança.

| Nº | Método de Teste | Defeito Reproduzido / O que o Teste Revela | Situação |
|---|---|---|---|
| 86 | `garcom_toggleStore_shouldBe403` | **DEF-08 (Média):** Garçom acessando rota de admin com `@PreAuthorize` recebe HTTP 500 em vez de 403 Forbidden. | `@Disabled` |
| 87 | `garcom_archiveAsCredit_shouldBe403` | **DEF-08b (Média):** Garçom tentando arquivar fiado recebe HTTP 500 porque o handler captura o erro de acesso. | `@Disabled` |
| 88 | `unauthenticated_shouldBe401` | **DEF-09 (Baixa):** Requisição sem token JWT recebe HTTP 403 Forbidden em vez de HTTP 401 Unauthorized. | `@Disabled` |
| 89 | `notFoundRoute_shouldBe404` | **DEF-11 (Baixa):** Rota inexistente na API devolve HTTP 500 porque o handler genérico engole o erro do Spring. | `@Disabled` |
| 90 | `openBalcaoTab_withoutTableNumber_shouldBe201` | **DEF-10 (Média):** Comanda de Balcão exige obrigatoriamente número de mesa no DTO, violando a regra do README. | `@Disabled` |

---

### 5.7 Testes Unitários de Linha de Base e Inicialização (20 Testes)
Testes pré-existentes no repositório original que foram preservados e integrados à suíte geral.

#### `BillServiceTest` (10 Testes)
| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 91 | `openTab_AutoNames` | Gera nome automático sequencial se o garçom não informar o nome do cliente. | Aprovado |
| 92 | `registerPayment_WithDiscount_ThrowsIfGarcom` | Bloqueia a concessão de descontos por usuários com papel de garçom. | Aprovado |
| 93 | `payGroupedTabs_Success` | Realiza pagamento agrupado com sucesso liquidando 2 comandas. | Aprovado |
| 94 | `reopenTab_Safely` | Reabre conta com segurança verificando se a mesa original está ocupada. | Aprovado |
| 95 | `patchTab_UpdatesCustomerName` | Atualiza o nome do cliente associado à comanda. | Aprovado |
| 96 | `registerPayment_DiscountGreaterThanBalance_Throws` | Impede concessão de desconto superior ao saldo devedor da conta. | Aprovado |
| 97 | `archiveAsCredit_NoBalance_Throws` | Bloqueia arquivamento em fiado se a conta estiver com saldo zerado. | Aprovado |
| 98 | `cancelTab_ClosedTab_Throws` | Bloqueia tentativa de cancelamento de conta já fechada. | Aprovado |
| 99 | `transferTab_ClosedTab_Throws` | Bloqueia tentativa de transferência de conta fechada. | Aprovado |
| 100| `payGroupedTabs_ZeroAmount_Throws` | Bloqueia pagamento agrupado com valor zerado ou negativo. | Aprovado |

#### `OrderServiceTest` (3 Testes)
| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 101| `reduceItemQuantity_Success` | Garçom reduz a quantidade do prato e abate o valor correspondente da mesa. | Aprovado |
| 102| `cancelOrderItem_ByAdmin_WhenTabClosed` | Administrador pode cancelar itens e estornar valor mesmo com a mesa já fechada. | Aprovado |
| 103| `cancelOrderItem_ThrowsException_WhenGarcomCancelsPreppingItem` | Bloqueia tentativa do garçom de cancelar prato que já está sendo preparado na chapa. | Aprovado |

#### `TableServiceTest` (2 Testes)
| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 104| `createTable_Success` | Cria uma nova mesa no salão com sucesso e número único. | Aprovado |
| 105| `createTable_ThrowsException_WhenNumberExists` | Bloqueia a criação de mesa com número duplicado no salão. | Aprovado |

#### `UserServiceTest` (2 Testes)
| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 106| `registerUser_Success` | Registra um novo usuário no sistema com credenciais válidas. | Aprovado |
| 107| `registerUser_ThrowsException_WhenEmailExists` | Bloqueia o cadastro de novos usuários com e-mail duplicado. | Aprovado |

#### `MenuServiceTest` (2 Testes)
| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 108| `saveProduct_Success` | Cadastra novo prato no cardápio associando-o à sua categoria correspondente. | Aprovado |
| 109| `saveProduct_ThrowsException_WhenCategoryNotFound` | Falha ao tentar salvar produto vinculado a uma categoria inexistente. | Aprovado |

#### `ApiApplicationTests` (1 Teste)
| Nº | Método de Teste | O que é / Regra de Negócio que Valida | Situação |
|---|---|---|---|
| 110| `contextLoads` | Inicializa o contexto completo do Spring Boot e conexões JPA com banco em memória H2. | Aprovado |

---

## 6. Catálogo de Defeitos Encontrados

| ID | Severidade | Módulo / Onde Ocorre | Comportamento Observado (Falha) | Comportamento Esperado | Impacto no Negócio |
|---|---|---|---|---|---|
| **DEF-01** | 🔴 **Alta** | `BillService.removeCoverCharge` | Ao remover o couvert sucessivamente em mesa com rateio, divide o valor pelas contas restantes em vez do valor cobrado, gerando **saldo devedor negativo (R$ -0,83)**. | A conta deve voltar exatamente a R$ 0,00 ou ao valor original sem couvert. | **Perda financeira direta:** o restaurante perde dinheiro e o cliente sai com saldo negativo. |
| **DEF-02** | 🟠 **Média** | `BillService.removeCoverCharge` | Em comanda de **balcão** (sem mesa), a remoção de couvert é ignorada silenciosamente e o valor continua cobrado. | O couvert deve ser abatido do saldo do cliente de balcão. | Cobrança indevida de cliente que pediu cancelamento do couvert no balcão. |
| **DEF-03** | 🔴 **Alta** | `OrderService.splitItem` | Permite ratear itens para comandas com status de fiado (`IN_DEBT`), alterando dívidas antigas já consolidadas. | O sistema deve rejeitar o rateio com `BusinessRuleException`. | **Fraude / Inconsistência Contábil:** permite adulterar dívidas já fechadas de clientes. |
| **DEF-04** | 🟡 **Baixa** | `OrderService.splitItem` | Aceita a própria comanda de origem na lista de contas de destino do rateio. | Bloquear a seleção da própria conta de origem com exceção. | Criação de movimentações financeiras redundantes e confusas. |
| **DEF-05** | 🟠 **Média** | `BillService.payGroupedTabs` | Se um ID inexistente for passado na lista de comandas, o sistema ignora o erro e fecha apenas as que encontrou. | Rejeitar a transação inteira se houver comanda inexistente na lista. | Pagamento parcial não intencional sem alerta ao operador do caixa. |
| **DEF-06** | 🟡 **Baixa** | `BillService.toggleCoverCharge` | Ao dividir couvert de R$ 5,00 por 3 com arredondamento comercial: 3 x 1,67 = **R$ 5,01** (cobra 1 centavo a mais). | O sistema deve calibrar a última parcela para que a soma seja exatamente R$ 5,00. | Cobrança a maior de centavos que, somados em milhares de contas, gera inconsistência contábil. |
| **DEF-07** | 🟡 **Baixa** | `BillService.registerPayment` | A camada de serviço aceita pagamento com valor negativo se chamada diretamente (sem passar pelo DTO). | O método do serviço deve possuir defesa em profundidade rejeitando valor <= 0. | Fragilidade de segurança contra injeção direta de chamadas na camada de serviço. |
| **DEF-08** | 🟠 **Média** | `GlobalExceptionHandler` + Security | Garçom tentando acessar endpoints restritos a Admin recebe HTTP 500 em vez de HTTP 403 Forbidden. | A API deve retornar HTTP 403 Forbidden informando acesso não autorizado. | O cliente HTTP interpreta como falha de servidor (queda do backend) em vez de restrição de acesso. |
| **DEF-09** | 🟡 **Baixa** | `SecurityConfig` | Requisições HTTP sem token JWT recebem HTTP 403 em vez de HTTP 401 Unauthorized. | Retornar HTTP 401 Unauthorized com cabeçalho `WWW-Authenticate`. | Dificulta o tratamento automático de expiração de token em aplicativos frontend. |
| **DEF-10** | 🟠 **Média** | `BillOpenDto` | O DTO exige obrigatoriamente número de mesa `@NotNull @Positive`, impedindo abrir comanda de BALCÃO sem mesa fictícia. | O campo `tableNumber` deve ser opcional para consumos do tipo `BALCAO`. | Operador de balcão é forçado a inventar números de mesa inexistentes no sistema. |
| **DEF-11** | 🟡 **Baixa** | `GlobalExceptionHandler` | Chamada a rotas inexistentes retorna HTTP 500 genérico em vez de HTTP 404 Not Found. | Retornar HTTP 404 Not Found padrão do Spring. | Poluição dos logs de monitoramento com falsos alertas de erro interno de servidor. |
| **ENV-01** | 🟢 **Resolvido** | `ApiApplicationTests` / Banco | Teste de contexto quebrava em máquinas limpas por exigir PostgreSQL ativo localmente. | O build do Maven deve rodar em qualquer ambiente sem necessidade de setup manual. | **Resolvido nesta atividade:** configurado banco de dados H2 em memória no perfil de teste. |

---

## 7. Como Rodar na Hora da Apresentação (Guia Prático Passo a Passo)

Para apresentar ao vivo para o professor e os colegas de sala, siga o roteiro de comandos abaixo diretamente no terminal:

### Passo 1: Executar a Suíte Completa e Mostrar os 110 Testes Verdes
Abra o terminal na pasta raiz do projeto (`Syskewer`) e execute:

```bash
./mvnw test
```

**O que apontar na tela para o professor:**
- Mostre o console final imprimindo:
  ```text
  [INFO] Results:
  [INFO] 
  [WARNING] Tests run: 110, Failures: 0, Errors: 0, Skipped: 12
  [INFO] 
  [INFO] BUILD SUCCESS
  ```
- **Explique:** *"Professor, construímos 110 testes automatizados. Os 98 testes ativos cobrem todas as regras válidas e estão 100% aprovados com `BUILD SUCCESS`. Os 12 testes 'Skipped' não foram esquecidos: são testes automatizados que criamos para documentar e reproduzir os 12 defeitos encontrados no sistema!"*

---

### Passo 2: Executar com Relatório de Cobertura JaCoCo
Execute o comando Maven com o perfil de qualidade ativado:

```bash
rm -f target/jacoco.exec
./mvnw test -Pquality
```

Em seguida, abra o relatório HTML no navegador:
```bash
# No Linux / WSL:
xdg-open target/site/jacoco/index.html
# Ou abra o arquivo target/site/jacoco/index.html no navegador
```

**O que mostrar na tela:**
- Mostre que as classes fundamentais do restaurante (`BillService`, `OrderService`, `KitchenService`) estão com **99% a 100% de linhas cobertas**.
- Mostre a cobertura global de **62,1%** e explique por que ela reprovou o critério CS-03 (faltou cobrir autenticação e e-mail).

---

### Passo 3: O "Gran Finale" — Demonstrar o Defeito Crítico ao Vivo!
Para provar que o teste de defeito realmente pega o bug de cálculo financeiro (DEF-01):

1. Abra o arquivo `src/test/java/com/syskewer/api/KnownDefectsTest.java`.
2. Comente ou remova temporariamente a linha 49 (`@Disabled`).
3. No terminal, execute apenas o teste do DEF-01:

```bash
./mvnw test -Dtest=KnownDefectsTest#removeCoverCharge_threeBills_eachRefundMatchesCharge
```

**O que vai acontecer na tela:**
- O teste vai **FALHAR**, mostrando exatamente a anomalia contábil:
  ```text
  org.opentest4j.AssertionFailedError: 
  Expected: 0.00
  Actual  : -0.83
  ```
- **Explique com convicção:** *"Aqui está a prova do nosso veredito NO-GO: se esse sistema fosse para produção hoje, ao cancelar o couvert de uma mesa com 3 amigos, o sistema deixaria o saldo de um deles com valor negativo de R$ -0,83. Nosso teste automatizado capturou o bug antes que ele chegasse ao cliente!"*
- *(Ao terminar, recoloque o `@Disabled` para manter o build verde).*

---

### Passo 4: Rodar Fatias Específicas de Testes (Demonstração Rápida)
Se o professor pedir para ver apenas a camada Web ou apenas a esteira da cozinha:

- **Para rodar apenas os testes da Camada Web e Segurança:**
  ```bash
  ./mvnw test -Dtest=ApiWebLayerTest
  ```
- **Para rodar apenas as regras financeiras:**
  ```bash
  ./mvnw test -Dtest=BillServiceExtendedTest
  ```
- **Para rodar apenas a esteira da Cozinha:**
  ```bash
  ./mvnw test -Dtest=KitchenServiceTest
  ```

---

## 8. Roteiro e Divisão de Apresentação da Equipe (~10 Minutos)

| Tempo | Integrante | Tema da Fala | Pontos Chave para Dizer |
|---|---|---|---|
| **00:00 - 02:00** | **Integrante 1** | **O que é o Tópico 6 no CTFL 4.0** | - Explicar as 7 atividades do ISTQB.<br>- Definir que Avaliação de Saída verifica os *Critérios de Parada* definidos no início.<br>- Frisar: *"Não paramos de testar porque o tempo acabou, mas sim porque métricas objetivas foram atendidas."* |
| **02:00 - 04:00** | **Integrante 2** | **Simulação dos 6 Meses e Critérios** | - Mostrar a linha do tempo (Gantt dos 6 meses).<br>- Apresentar a tabela com os 8 critérios de saída (CS-01 a CS-08).<br>- Explicar por que definimos limiares de 70% de cobertura e 0 defeitos críticos. |
| **04:00 - 06:30** | **Integrante 3** | **Execução ao Vivo dos 110 Testes** | - Compartilhar o terminal e rodar `./mvnw test`.<br>- Mostrar os 110 testes rodando em 30 segundos.<br>- Abrir o JaCoCo e explicar a diferença entre 62% global e 99% no núcleo de negócio. |
| **06:30 - 08:30** | **Integrante 4** | **Demonstração do Bug ao Vivo e Mutação** | - Executar o teste do DEF-01 e mostrar o saldo negativo de R$ -0,83.<br>- Explicar o teste de mutação no `OrderService` (63%): *"Cobertura alta não garante teste forte!"* |
| **08:30 - 10:00** | **Integrante 5** | **Veredito Oficial (NO-GO) e Conclusão** | - Apresentar a decisão: **NO-GO para Produção**.<br>- Defender a postura ética da equipe de testes em barrar o lançamento.<br>- Apresentar o Relatório Resumido de Testes e responder às dúvidas da banca. |

---

## 9. Lições Aprendidas pelo Grupo

1. **A Ferramenta de Medição Pode Enganar:** O JaCoCo por padrão acumula dados de execuções anteriores no arquivo `target/jacoco.exec`. Na nossa primeira medição, o relatório indicou 64% na linha de base com apenas 20 testes porque ele leu execuções anteriores sujas. Aprendemos que sempre se deve limpar o ambiente (`rm -f target/jacoco.exec` ou `mvn clean`) antes de aferir métricas oficiais.
2. **Cobertura Alta de Linhas Não É Garantia de Teste Forte:** O `OrderService` alcançou 99,2% de cobertura de código, mas seu escore de mutação foi de apenas 63%. Isso ensina que os testes estavam executando as linhas, mas não estavam assinalando todas as alterações de estado.
3. **Critérios de Parada Precisam Ser Quantitativos:** Dizer que "o sistema precisa ser bem testado" é subjetivo. Estabelecer que "nenhum defeito de severidade Alta pode estar aberto e a cobertura do núcleo deve ser ≥ 90%" permitiu uma tomada de decisão matemática e indiscutível.
4. **Isolamento de Ambiente É Requisito Fundamental:** Testes automatizados que dependem de bancos externos ou containers quebram o pipeline em máquinas de outros desenvolvedores. O uso do H2 em memória permitiu reprodutibilidade absoluta da suíte.

---
