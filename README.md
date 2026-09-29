# MyPetz

MyPetz é um aplicativo Android para organizar a rotina de atendimento de banho e tosa, reunindo agenda, clientes, serviços, produtos e informações financeiras em um só lugar.

## Sobre o projeto

O aplicativo foi criado para ajudar profissionais e estabelecimentos de cuidado animal a acompanhar os atendimentos e os dados importantes da operação. A proposta é centralizar o cadastro de clientes e pets, o planejamento da agenda, o catálogo de serviços e produtos e o controle financeiro, reduzindo a necessidade de controles dispersos.

## Tecnologias utilizadas

- Kotlin
- Android (minSdk 26)
- Jetpack Compose e Material 3
- Gradle com Kotlin DSL e catálogo de versões
- Room para persistência local
- Firebase Authentication para autenticação por e-mail e Google
- Cloud Firestore para sincronização de dados por conta
- AndroidX Credentials e Google Identity para o fluxo de autenticação Google
- Kotlin Coroutines e Flow para operações assíncronas e observação de dados
- JUnit, Robolectric e testes de interface do Compose para testes

## Funcionalidades

- Cadastro, edição e remoção de clientes e informações dos pets, incluindo dados de contato, observações e preços de serviços.
- Agenda diária para criar, consultar, editar, cancelar e finalizar atendimentos.
- Associação de cliente, pet, serviços e produtos a um agendamento, com horário, transporte, descontos, forma de pagamento e observações.
- Lembretes locais de agendamentos por notificação.
- Catálogo para cadastrar serviços de banho e tosa e produtos, com preços; os produtos também possuem custo e quantidade em estoque.
- Controle financeiro com receitas ligadas a atendimentos pagos e lançamentos financeiros manuais.
- Autenticação com e-mail ou conta Google e recuperação de senha por e-mail.
- Armazenamento local dos dados com Room e sincronização com o Cloud Firestore quando a configuração Firebase estiver disponível.
- Tour de apresentação inicial das áreas do aplicativo.

## Como executar o projeto

1. Instale o Android Studio e abra a pasta raiz do projeto, onde estão `settings.gradle.kts` e `gradlew`.
2. Aguarde a sincronização do Gradle e instale os componentes do Android SDK solicitados pelo Android Studio.
3. Para usar autenticação e sincronização na nuvem, configure um projeto Firebase compatível e coloque o arquivo de configuração Android `google-services.json` em `app/`. Esse arquivo é local e não deve ser enviado ao GitHub.
4. Selecione um emulador ou dispositivo Android com API 26 ou superior e execute a configuração `app`.

Sem a configuração Firebase, as funcionalidades que dependem dos serviços Firebase podem não estar disponíveis; a configuração do projeto permite que o Google Services Plugin avise quando ela estiver ausente.

## Estrutura do projeto

- `app/src/main/java/com/example/`: código Kotlin do aplicativo.
- `app/src/main/java/com/example/ui/`: telas Compose, fluxo de autenticação, estado e tema visual.
- `app/src/main/java/com/example/data/`: entidades, DAOs, banco Room, autenticação e repositório de dados/sincronização.
- `app/src/main/java/com/example/notifications/`: agendamento e apresentação de notificações.
- `app/src/main/res/`: ícones, imagens e recursos Android.
- `app/src/test/` e `app/src/androidTest/`: testes locais e instrumentados.
- `gradle/`, `build.gradle.kts` e `settings.gradle.kts`: configuração e dependências do Gradle.

## Segurança

Não envie informações sensíveis ao GitHub. Arquivos como `.env`, `local.properties`, `google-services.json`, chaves de assinatura (`.jks` e `.keystore`) e outras credenciais devem permanecer locais ou ser armazenados em um mecanismo seguro apropriado. Confira o `.gitignore` antes de preparar alterações para commit.

## Status do projeto

O projeto contém uma versão Android funcional em desenvolvimento, com telas e operações implementadas para agenda, clientes, catálogo e controle financeiro. A versão configurada atualmente é `1.0` (código de versão `1`); a integração com Firebase depende da configuração local do projeto Firebase.

## Autor

Claudio Costa
