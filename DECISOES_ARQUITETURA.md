# Decisões de arquitetura

[Início](./Readme.md) · [Bancos de dados](./BANCOS_DE_DADOS.md) · [Teste do fluxo](./TESTE_FLUXO_COMPLETO.md) · [Galeria de imagens](./GALERIA_DE_IMAGENS.md)

Este documento registra como o SGCO está organizado hoje, por que essas escolhas foram feitas e quais são suas consequências. Ele descreve o comportamento atual do código; não significa que todas as limitações listadas sejam requisitos permanentes.

## 1. Microserviços separados por responsabilidade

**Decisão atual:** o sistema possui quatro serviços:

| Serviço | Responsabilidade |
| --- | --- |
| `course` | Cursos e lições |
| `enrollment` | Matrículas e seus estados |
| `progress` | Progresso e conclusão das lições |
| `certificate` | Emissão e validação de certificados |

**Motivo:** separar as responsabilidades e permitir que cada serviço mantenha seus próprios modelos e regras.

**Consequência:** uma operação que envolve mais de um domínio não é necessariamente concluída em uma única chamada. O cliente pode precisar consultar mais de uma API e aguardar o processamento dos eventos.

## 2. Um banco por serviço

**Decisão atual:** cada serviço é dono de seu banco SQLite:

- `course.db`
- `enrollment.db`
- `progress.db`
- `certificate.db`

**Motivo:** evitar que um serviço leia ou altere diretamente as tabelas pertencentes a outro.

**Consequência:** não existem chaves estrangeiras entre os bancos. Quando um serviço precisa se referir a dados de outro domínio, guarda o identificador correspondente, como `courseId` ou `studentId`.

Veja a estrutura de tabelas em [BANCOS_DE_DADOS.md](./BANCOS_DE_DADOS.md).

## 3. Ligação entre matrícula e curso por `courseId`

**Decisão atual:** a matrícula guarda o `courseId` enviado pelo cliente. Esse valor identifica o curso que está no banco do serviço `course`; não é uma relação JPA nem uma chave estrangeira entre bancos.

**O que acontece na criação:** o serviço `enrollment` verifica se os IDs são positivos, mas **não consulta o serviço `course` para confirmar que o curso existe ou está ativo**. Portanto, hoje é possível criar uma matrícula com um `courseId` que não corresponda a um curso válido.

**Como manter a referência correta no fluxo atual:**

1. Criar o curso pela API do serviço `course`.
2. Copiar o `id` retornado.
3. Enviar esse mesmo valor como `courseId` ao criar a matrícula.

O roteiro Postman segue essa ordem em [TESTE_FLUXO_COMPLETO.md](./TESTE_FLUXO_COMPLETO.md).

**Consequência:** os serviços continuam independentes, mas a validade da referência não é garantida no momento da matrícula. Se o sistema precisar dessa garantia, uma decisão futura será necessária: validar o curso por uma chamada síncrona ao serviço `course`, ou criar outro mecanismo de sincronização.

## 4. Identificação do aluno na matrícula

**Decisão atual:** a tabela `tb_enrollment` guarda somente o `studentId` como referência ao aluno. A API de matrícula não recebe nem armazena nome ou dados de perfil do aluno.

**Motivo:** manter o serviço focado no fluxo de matrícula e progresso sem introduzir uma área de cadastro de alunos.

**Consequência:** dados de perfil do aluno ficam fora do escopo atual e poderão ser tratados por uma área própria no futuro.

## 5. Eventos assíncronos pelo RabbitMQ

**Decisão atual:** os serviços publicam eventos JSON no exchange `SGCO.exchange`, do tipo `DirectExchange`. Cada evento usa uma routing key; o RabbitMQ encaminha a mensagem para a fila vinculada a essa chave. O serviço seguinte consome a mensagem e atualiza seu próprio banco.

**Limitação atual:** todos os serviços declaram somente esse `DirectExchange`. Se a entrega exigir pelo menos dois tipos de exchange, esse requisito ainda precisa ser implementado; este documento descreve o estado atual, não uma funcionalidade já atendida.

### Caminho das mensagens

```text
Cliente
  └─ cria matrícula ─> enrollment
                         └─ publica enrollment.created
                              └─ fila progress.enrollment-created.queue
                                   └─ progress cria progresso inicial

Cliente
  └─ conclui lição ─> course
                         └─ publica lesson.completed
                              └─ fila progress.lesson-completed.queue
                                   └─ progress atualiza o progresso
                                        └─ ao chegar a 100%, publica course.completed
                                             └─ fila certificate.course-completed.queue
                                                  └─ certificate emite o certificado
                                                       └─ publica certificate.issued
                                                            └─ fila enrollment.certificate-issued.queue
                                                                 └─ enrollment conclui a matrícula
```

### Mensagens publicadas e consumidas

| Ordem | Routing key | Publicador | Fila de destino | Consumidor | Efeito |
| ---: | --- | --- | --- | --- | --- |
| 1 | `enrollment.created` | `enrollment` | `progress.enrollment-created.queue` | `progress` | Cria o progresso inicial |
| 2 | `lesson.completed` | `course` | `progress.lesson-completed.queue` | `progress` | Marca a lição como concluída e recalcula o percentual |
| 3 | `course.completed` | `progress` | `certificate.course-completed.queue` | `certificate` | Inicia a emissão do certificado |
| 4 | `certificate.issued` | `certificate` | `enrollment.certificate-issued.queue` | `enrollment` | Salva a URL e marca a matrícula como concluída |

Os eventos carregam os IDs necessários para os consumidores relacionarem seus próprios dados; dados de perfil do aluno não fazem parte das mensagens atuais.

### Conteúdo de cada mensagem

Os exemplos são JSON ilustrativos; os IDs e o código de validação são gerados ou recebidos durante a execução.

#### 1. `enrollment.created`

O serviço `enrollment` publica depois de salvar a matrícula. O serviço `progress` consome para criar o progresso com `0%`.

```json
{
  "enrollmentId": 1,
  "studentId": 1001,
  "courseId": 42
}
```

#### 2. `lesson.completed`

O serviço `course` publica quando o cliente solicita a conclusão de uma lição existente no curso. O serviço `progress` consome para atualizar o estado da lição e calcular o percentual.

```json
{
  "studentId": 1001,
  "courseId": 42,
  "lessonId": 7,
  "totalLessons": 2
}
```

#### 3. `course.completed`

O serviço `progress` publica quando todas as lições estão concluídas. O serviço `certificate` consome para emitir e salvar o certificado.

```json
{
  "studentId": 1001,
  "courseId": 42
}
```

#### 4. `certificate.issued`

O serviço `certificate` publica depois de salvar o registro do certificado. O serviço `enrollment` consome para localizar a matrícula pelo `studentId` e `courseId`, salvar `pdfUrl` e alterar seu estado para `COMPLETED`. Atualmente, `pdfUrl` é demonstrativa; o serviço não gera nem armazena um PDF real.

```json
{
  "studentId": 1001,
  "courseId": 42,
  "pdfUrl": "https://meu-storage.s3.aws.com/certificados/42/1001.pdf",
  "validationCode": "código-gerado-pelo-serviço"
}
```

**Motivo:** os serviços comunicam mudanças relevantes sem compartilhar tabelas nem depender de uma resposta imediata do próximo serviço.

**Consequência:** o processamento é eventual. Logo após uma requisição, o dado atualizado por outro serviço pode ainda não estar disponível; uma consulta imediata pode precisar ser repetida. Todos os produtores encaminham seus eventos ao RabbitMQ somente após a transação local ser confirmada. No `course`, essa transação é de leitura: o serviço valida a aula e o curso, mas não persiste a conclusão, que é registrada pelo `progress`. A implementação ainda não usa o padrão Outbox; uma falha no broker depois do commit pode exigir recuperação manual do evento. Os consumidores atuais também tratam reentregas de forma idempotente para evitar repetir alguns efeitos.

## 6. Cliente acessa as APIs dos serviços diretamente

**Decisão atual:** o cliente REST chama as APIs de `course`, `enrollment`, `progress` e `certificate` diretamente. Não há, no código atual, um BFF que agregue essas APIs.

**Motivo:** manter a arquitetura simples enquanto cada serviço oferece os dados e operações do seu próprio domínio.

**Consequência:** o cliente precisa conhecer os endpoints e, quando necessário, combinar respostas e aguardar os fluxos assíncronos. Um BFF pode ser considerado se isso se tornar complexo para a interface.

## 7. SQLite e atualização automática do esquema

**Decisão atual:** os serviços usam bancos SQLite locais e o Hibernate está configurado com `spring.jpa.hibernate.ddl-auto=update`.

**Motivo:** facilitar a execução e o desenvolvimento sem exigir um servidor de banco separado para cada serviço.

**Consequência:** é uma configuração simples para desenvolvimento, mas a aplicação de mudanças de esquema não é controlada por scripts de migração versionados. A escolha de bancos e migrações para produção pode ser avaliada separadamente.

## Resumo da ligação entre os dados

```text
course.db
  tb_course.id
       ↑ mesmo valor de ID
enrollment.db
  tb_enrollment.courseId

enrollment.db
  tb_enrollment.id
       ↑ enrollmentId enviado pelo evento
progress.db
  tb_student_progress.enrollmentId
```

As setas representam referências por valor, não relacionamentos de banco entre serviços.
