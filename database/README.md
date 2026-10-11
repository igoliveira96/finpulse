# Base local do FinPulse

O dump `finpulse.sql` contém o esquema e os mesmos dados ilustrativos de
`MockMarketData`: um investidor, um patrimônio com 26 pontos de histórico,
seis ações com 11 pontos cada e três favoritas. Valores monetários são inteiros
em centavos. A ordem dos gráficos usa `sample_index`; os pontos do mock não têm datas.

O arquivo `app/src/main/assets/database/finpulse.db` é a base SQLite empacotada.
`FinPulseApplication` fornece uma instância compartilhada do banco e do
`RoomHomeRepository`. A primeira consulta assíncrona da Home abre o Room fora da
thread principal.
`createFromAsset` copia e valida a base apenas quando o banco local ainda não existe.
Abrir novamente não sobrescreve edições nem duplica registros.

A Home recebe o `RoomHomeRepository` pelo `HomeViewModel.Factory`. O repositório
observa as seis tabelas e monta o `HomeSnapshot` em uma transação, incluindo
favoritas e históricos ordenados. Alterações persistidas são refletidas na tela.
O investidor local tem id `1`; uma futura sessão poderá fornecer outro id.

`MockFinPulseApi` não participa do carregamento da Home. O mock é mantido como
referência para previews e testes. Falhas de leitura ou ausência do investidor ou
patrimônio levam ao estado de erro, sem substituir dados locais pelo mock.

## Regerar a base

1. Edite o dump SQL. Para alterações estruturais, mantenha entidades, versão e
   schema exportado em `app/schemas` consistentes e planeje a migração.
2. Na raiz do projeto, execute:

   ```sh
   python3 tools/build_seed_database.py
   ./gradlew :app:assembleDebug :app:connectedDebugAndroidTest
   ```

O gerador usa somente a biblioteca padrão do Python e verifica integridade,
chaves estrangeiras e versão antes de substituir o asset. O teste instrumentado
compara todos os dados do dump com o mock e verifica a validação real pelo Room.
O dump não contém `room_master_table`: o Room valida a estrutura antes de
registrar sua identidade na cópia privada do app.

O asset atualizado afeta somente bases novas. Não há fallback destrutivo nem
reseeding em cada abertura. Os testes usam nomes de banco exclusivos e não
apagam a base do aplicativo.

Referências: [Room](https://developer.android.com/jetpack/androidx/releases/room)
e [preenchimento inicial](https://developer.android.com/training/data-storage/room/prepopulate).
