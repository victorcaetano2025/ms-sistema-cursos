# Teste completo do fluxo de mensagens

[Início](./Readme.md) · [Bancos de dados](./BANCOS_DE_DADOS.md) · [Decisões de arquitetura](./DECISOES_ARQUITETURA.md) · [Galeria de imagens](./GALERIA_DE_IMAGENS.md)

Este roteiro descreve como verificar manualmente o fluxo de ponta a ponta: criar um curso e uma matrícula, concluir as lições e consultar o progresso, o registro do certificado e a atualização da matrícula. As mensagens são JSON e trafegam pelo exchange `SGCO.exchange`, do tipo `DirectExchange`.

## Preparar o teste

1. Inicie o RabbitMQ e os quatro microserviços. Para este roteiro, use o aluno `1001`.
2. No Postman, crie as requisições indicadas abaixo. Não é necessário criar Environment, Collection ou scripts.
3. Após cada criação, copie o `id` da resposta e cole no lugar indicado nas próximas URLs ou corpos.
4. Em requisições com JSON, selecione **Body > raw > JSON**.

> Cada execução cria novos dados nos bancos SQLite. Use um aluno ainda não matriculado no curso de teste.

## Sequência de requisições

### 1. Criar curso

- Método: `POST`
- URL: `http://localhost:8082/api/courses`
- Body:

  ```json
  {
    "title": "Teste Postman - fluxo completo",
    "description": "Curso criado para validar os eventos do RabbitMQ"
  }
  ```

- Envie e copie o campo `id` da resposta. Esse é o **ID_CURSO**.
- Resultado esperado: `201 Created`.

### 2. Criar duas lições

Faça as duas requisições usando o ID_CURSO copiado na etapa anterior.

**Lição 1**

- Método: `POST`
- URL: `http://localhost:8082/api/courses/ID_CURSO/lessons`
- Substitua `ID_CURSO` pelo número real, sem aspas.
- Body:

  ```json
  {
    "title": "Lição 1",
    "videoUrl": "https://example.com/licao-1",
    "orderIndex": 1
  }
  ```

- Envie e copie o campo `id` da resposta. Esse é o **ID_LICAO_1**.

**Lição 2**

- Use o mesmo método e URL da Lição 1.
- Body:

  ```json
  {
    "title": "Lição 2",
    "videoUrl": "https://example.com/licao-2",
    "orderIndex": 2
  }
  ```

- Envie e copie o campo `id` da resposta. Esse é o **ID_LICAO_2**.
- Resultado esperado para as duas criações: `201 Created`.

### 3. Criar matrícula

- Método: `POST`
- URL: `http://localhost:8081/api/enrollments`
- Body: substitua `ID_CURSO` pelo número real copiado na etapa 1.

  ```json
  {
    "studentId": 1001,
    "courseId": ID_CURSO
  }
  ```

- Envie e copie o campo `id` da resposta. Esse é o **ID_MATRICULA**.
- Resultado esperado: `201 Created` e status `ACTIVE`.

Essa chamada publica `enrollment.created`. O serviço `progress` consome a mensagem e cria o progresso inicial.

### 4. Conferir o progresso inicial

- Método: `GET`
- URL: `http://localhost:8083/api/progress/student/1001/course/ID_CURSO`
- Substitua `ID_CURSO` pelo número real.
- Resultado esperado: `200 OK` e `completionPercentage` igual a `0`.

Como o processamento é assíncrono, se receber `404`, aguarde alguns segundos e envie novamente.

### 5. Concluir a primeira lição

- Método: `POST`
- URL: `http://localhost:8082/api/courses/ID_CURSO/lessons/ID_LICAO_1/complete?studentId=1001`
- Substitua `ID_CURSO` e `ID_LICAO_1` pelos números copiados anteriormente.
- Não precisa de body. Resultado esperado: `200 OK`.

O serviço `course` publica `lesson.completed` e o serviço `progress` consome a mensagem.

Consulte novamente o progresso usando a URL da etapa 4. O esperado é `completionPercentage` igual a `50` e `isCourseCompleted` igual a `false`.

### 6. Concluir a segunda lição

- Método: `POST`
- URL: `http://localhost:8082/api/courses/ID_CURSO/lessons/ID_LICAO_2/complete?studentId=1001`
- Substitua `ID_CURSO` e `ID_LICAO_2` pelos números copiados anteriormente.
- Não precisa de body. Resultado esperado: `200 OK`.

Consulte novamente o progresso até aparecer `completionPercentage` igual a `100` e `isCourseCompleted` igual a `true`.

### 7. Consultar e validar o certificado

**Consultar certificado**

- Método: `GET`
- URL: `http://localhost:8084/api/certificates/student/1001/course/ID_CURSO`
- Substitua `ID_CURSO` pelo número real.
- Se receber `404`, aguarde alguns segundos e envie novamente.
- Resultado esperado: `200 OK`. Copie o valor de `validationCode` da resposta.

**Validar certificado**

- Método: `GET`
- URL: `http://localhost:8084/api/certificates/validate/CODIGO_VALIDACAO`
- Substitua `CODIGO_VALIDACAO` pelo valor copiado da resposta anterior.
- Resultado esperado: `200 OK`.

### 8. Conferir matrícula concluída

- Método: `GET`
- URL: `http://localhost:8081/api/enrollments/ID_MATRICULA`
- Substitua `ID_MATRICULA` pelo número copiado na etapa 3.
- Resultado esperado: status `COMPLETED` e `certificateUrl` preenchida.

## Eventos do RabbitMQ e resultados esperados

As requisições Postman chamam as APIs HTTP; elas **não enviam diretamente ao RabbitMQ**. Os serviços publicam e consomem os eventos automaticamente. Para acompanhar filas, acesse http://localhost:15672 e entre com `admin` / `admin123`.

| Etapa | Routing key | Publicador | Fila / consumidor | Resultado esperado na API |
| --- | --- | --- | --- | --- |
| Criar matrícula | `enrollment.created` | `enrollment` | `progress.enrollment-created.queue` → `progress` | Progresso criado com `0%` |
| Concluir cada lição | `lesson.completed` | `course` | `progress.lesson-completed.queue` → `progress` | Percentual atualizado |
| Concluir todas as lições | `course.completed` | `progress` | `certificate.course-completed.queue` → `certificate` | Certificado disponível |
| Emitir certificado | `certificate.issued` | `certificate` | `enrollment.certificate-issued.queue` → `enrollment` | Matrícula `COMPLETED` e URL salva |

Os corpos publicados pelos serviços correspondem a estes dados:

| Routing key | Exemplo de payload JSON |
| --- | --- |
| `enrollment.created` | `{"enrollmentId": 1, "studentId": 1001, "courseId": 1}` |
| `lesson.completed` | `{"studentId": 1001, "courseId": 1, "lessonId": 1, "totalLessons": 2}` |
| `course.completed` | `{"studentId": 1001, "courseId": 1}` |
| `certificate.issued` | `{"studentId": 1001, "courseId": 1, "pdfUrl": "https://meu-storage.s3.aws.com/certificados/1/1001.pdf", "validationCode": "código-gerado-pelo-serviço"}` |

## Resultado esperado

- Matrícula criada com status `ACTIVE`.
- Progresso inicial em `0%`, depois `50%` e finalmente `100%`.
- Certificado disponível e validável pelo `validationCode`.
- Matrícula atualizada para `COMPLETED` com `certificateUrl`.

O PDF ainda é representado por uma URL demonstrativa; a geração e o armazenamento reais do arquivo não fazem parte deste teste.
