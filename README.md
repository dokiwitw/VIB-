# 🗺️ VIB! — O Waze das Baladas

MVP de mapa de hotspots em tempo real com Google Places API, Firestore, Geofencing e Gamificação.

---

## ⚡ Como abrir no Android Studio

1. **Extraia** o ZIP em qualquer pasta.
2. Abra o **Android Studio** → `File → Open` → selecione a pasta `VIB!`.
3. Aguarde o **Gradle sync** terminar.
4. Adicione sua **Google Maps API Key** em `local.properties`:
   ```properties
   MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXX
   ```
5. Pressione **▶ Run** no dispositivo ou emulador.

---

## 📁 Estrutura do Projeto

```text
VIB!/
├── app/src/main/
│   ├── java/com/mediquest/app/
│   │   ├── MainActivity.kt              — Ponto de entrada e gerenciamento de permissões
│   │   ├── MediQuestApplication.kt      — Configuração Global (Firestore + SDKs)
│   │   ├── model/
│   │   │   ├── Models.kt                — Entidades (Hotspot, User, Status)
│   │   │   ├── HotspotRepository.kt     — Lógica de Cache 24h + Google Places + Batch Writes
│   │   │   └── UserRepository.kt        — Persistência de usuários no Firestore
│   │   ├── viewmodel/
│   │   │   └── MediQuestViewModel.kt    — StateFlow + Integração Real-time
│   │   ├── geofence/
│   │   │   ├── GeofenceManager.kt       — Cercas virtuais (Android Limit: 100)
│   │   │   └── GeofenceBroadcastReceiver.kt — Detecção passiva e Notificações
│   │   ├── work/
│   │   │   └── SyncHotspotsWorker.kt    — Sync periódico em background (12h)
│   │   └── ui/
│   │       ├── theme/                   — Cores Neon e Identidade Visual (VIBTheme)
│   │       ├── AppNavigation.kt         — Grafo de navegação e transições
│   │       ├── SplashScreen.kt          — Tela de entrada animada
│   │       ├── MapScreen.kt             — Heatmap Dinâmico (A "Vibe")
│   │       ├── HotspotDetailSheet.kt    — Detalhes e Reporte de Lotação
│   │       ├── RankingScreen.kt         — Gamificação e Leaderboard
│   │       └── ProfileScreen.kt         — Progresso e XP do Usuário
```

---

## 🔑 Configuração das APIs (Google Cloud)

Para que a busca de locais funcione, siga estes passos:

1. Acesse o [Console do Google Cloud](https://console.cloud.google.com).
2. Ative as APIs:
   - **Maps SDK for Android**
   - **Places API** (Novo: Necessário para obter horários reais e popularidade)
3. Crie uma **API Key**.
4. (Opcional, mas Recomendado) Restrinja a chave para o pacote `com.mediquest.app` e use o SHA-1 de debug:
   `D1:EE:1E:BA:0E:04:95:D8:4B:8F:19:3E:D6:3E:89:AE:49:9C:D4:9F`
5. Insira a chave no arquivo `local.properties` do projeto.

---

## 🌡️ Lógica do Heatmap (The Vibe)

Diferente de mapas estáticos, o VIB! usa um algoritmo híbrido para calcular a intensidade do calor:

1. **Popularidade Histórica (Google):** Baseada no número de avaliações e nota (normalizado de 0.1 a 1.0). Locais como o "Pontão" ou "Setor Comercial" naturalmente brilham mais.
2. **Ocupação em Tempo Real (Crowdsourcing):** Reportes de usuários sobre a lotação atual.
3. **Geofencing (Presença Ativa):** O sistema utiliza cercas virtuais para detectar quando um usuário entra em um hotspot. Isso dispara notificações de feedback e garante que os dados do heatmap venham de pessoas fisicamente presentes, aumentando a precisão da "vibe".
4. **Filtro de Horário:** Se o local está fechado segundo a Places API, o peso cai para quase zero, limpando o mapa de "ruído" visual.

---

## 💾 Cache & Offline (Robustez)

*   **Otimização de Custos:** O app implementa um **Cache de 24 horas** no Firestore. A Google Places API é consultada apenas se os dados locais estiverem expirados, economizando quota de API.
*   **Firestore Offline:** O app funciona 100% sem internet para visualização. Os reportes são enfileirados e sincronizados automaticamente quando a conexão volta.
*   **Sincronização em Segundo Plano:** O `WorkManager` atualiza a base de dados a cada 12 horas de forma silenciosa.

---

## 🛡️ Permissões e Segurança

O app solicita permissões em cascata para garantir a melhor experiência:
1. **Localização Precisa:** Para o mapa.
2. **Notificações:** Para pedir seu feedback quando você entra em um local.
3. **Localização em Segundo Plano:** Essencial para o funcionamento das **Geofences** (detectar sua chegada mesmo com o celular no bolso).

---

## 📱 Funcionalidades de Gamificação

*   **Níveis:** Cada reporte bem-sucedido gera 10 XP.
*   **Peso de Confiança:** Usuários de nível alto têm mais "poder" no heatmap. O reporte de um veterano altera a cor do mapa mais rápido que o de um novato.
*   **Ranking:** Compita com outros exploradores para ver quem mais contribui para a comunidade de Brasília.
