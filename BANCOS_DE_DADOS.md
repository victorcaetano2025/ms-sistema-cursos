# Bancos de dados

[Início](./Readme.md) · [Decisões de arquitetura](./DECISOES_ARQUITETURA.md) · [Teste do fluxo](./TESTE_FLUXO_COMPLETO.md) · [Galeria de imagens](./GALERIA_DE_IMAGENS.md)

O sistema usa um banco SQLite independente em cada microserviço. Os arquivos são criados pelo serviço quando ele inicia, no diretório de trabalho do processo. O Hibernate atualiza a estrutura das tabelas automaticamente (`spring.jpa.hibernate.ddl-auto=update`).
Essa atualização não remove do banco colunas que tenham sido retiradas dos modelos; bancos existentes podem manter colunas obsoletas até uma migração ou recriação do banco.

Os campos abaixo correspondem aos atributos dos modelos. Nas relações, `course_id` e `student_progress_id` são as colunas explicitamente mapeadas pelas entidades.

## Visão geral

| Microserviço | Arquivo SQLite | Tabelas |
| --- | --- | --- |
| `course` | `course.db` | `tb_course`, `tb_lesson` |
| `enrollment` | `enrollment.db` | `tb_enrollment` |
| `progress` | `progress.db` | `tb_student_progress`, `tb_student_lesson_status` |
| `certificate` | `certificate.db` | `tb_certificate` |

Cada serviço é responsável por ler e gravar somente no próprio banco. Informações que identificam dados de outro serviço, como `courseId`, `studentId` e `enrollmentId`, são armazenadas como referências; não há chaves estrangeiras entre bancos. A comunicação entre serviços acontece por APIs e eventos RabbitMQ.

## `course.db`

Armazena cursos e suas lições.

### `tb_course`

| Campo | Descrição |
| --- | --- |
| `id` | Identificador do curso |
| `title` | Título |
| `description` | Descrição |
| `isActive` | Indica se o curso está ativo |

### `tb_lesson`

| Campo | Descrição |
| --- | --- |
| `id` | Identificador da lição |
| `title` | Título |
| `videoUrl` | URL do vídeo |
| `orderIndex` | Posição da lição no curso |
| `course_id` | Curso ao qual a lição pertence |

Um curso pode ter várias lições. `course_id` relaciona `tb_lesson` a `tb_course`.

## `enrollment.db`

Armazena as matrículas de alunos.

### `tb_enrollment`

| Campo | Descrição |
| --- | --- |
| `id` | Identificador da matrícula |
| `studentId` | Identificador do aluno |
| `courseId` | Identificador do curso |
| `status` | Estado da matrícula (`ACTIVE`, `CANCELED` ou `COMPLETED`) |
| `certificateUrl` | URL do certificado, quando disponível |
| `enrolledAt` | Data e hora da matrícula |

`studentId` e `courseId` são referências aos respectivos dados, mantidos por outros serviços.
O cadastro e os dados de perfil do aluno não fazem parte do escopo deste serviço.

## `progress.db`

Armazena o progresso do aluno no curso e o estado de conclusão de cada lição.

### `tb_student_progress`

| Campo | Descrição |
| --- | --- |
| `id` | Identificador do registro de progresso |
| `enrollmentId` | Identificador da matrícula no serviço `enrollment` |
| `studentId` | Identificador do aluno |
| `courseId` | Identificador do curso |
| `completionPercentage` | Percentual concluído |
| `isCourseCompleted` | Indica se o curso foi concluído |
| `completedAt` | Data e hora da conclusão do curso |

### `tb_student_lesson_status`

| Campo | Descrição |
| --- | --- |
| `id` | Identificador do estado da lição |
| `student_progress_id` | Registro de progresso relacionado |
| `lessonId` | Identificador da lição no serviço `course` |
| `isLessonCompleted` | Indica se a lição foi concluída |
| `completedAt` | Data e hora da conclusão da lição |

Um registro de progresso pode conter vários estados de lição. `student_progress_id` relaciona `tb_student_lesson_status` a `tb_student_progress`.

## `certificate.db`

Armazena certificados emitidos.

### `tb_certificate`

| Campo | Descrição |
| --- | --- |
| `id` | Identificador do certificado |
| `validationCode` | Código único usado para validar o certificado |
| `studentId` | Identificador do aluno |
| `courseId` | Identificador do curso |
| `pdfUrl` | URL do arquivo do certificado |
| `issuedAt` | Data e hora de emissão |

Atualmente, `pdfUrl` contém uma URL demonstrativa: o serviço não gera nem armazena o arquivo PDF.

## RabbitMQ

O RabbitMQ é usado para troca de mensagens entre os microserviços; ele não é um dos bancos de dados SQLite da aplicação. Suas configurações e dados persistidos são independentes dos arquivos listados acima.
