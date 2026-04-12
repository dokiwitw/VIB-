# 🗺️ VIB! — O Waze do Rolê

O **VIB!** é um MVP de mapa de hotspots em tempo real que utiliza a **Google Places API**, **Firestore** e **Geofencing** para oferecer uma visão térmica da "vibe" da cidade. O app foca em resolver o problema de chegar em um local e ele estar vazio ou excessivamente lotado, utilizando dados híbridos (Google + Crowdsourcing).

---

## 📋 Requisitos para Rodar o App

Para compilar e executar o **VIB!** com sucesso, certifique-se de que seu ambiente atenda aos seguintes critérios:

### 🛠️ Ambiente de Desenvolvimento
*   **Android Studio:** Jellyfish (2023.3.1) ou superior.
*   **JDK:** Versão 17.
*   **Gradle:** 8.4 ou superior.

### 📱 Dispositivo/Emulador
*   **Android:** Versão 8.0 (API 26) ou superior.
*   **Google Play Services:** Atualizado (necessário para Maps, Places e Geofencing).
*   **Hardware (Recomendado):** Dispositivo físico com GPS e conexão estável para testes de **Geofencing** e **Localização em Segundo Plano**.

### 🔑 Credenciais Necessárias
*   **Google Maps API Key:** Com as APIs *Maps SDK for Android* e *Places API* ativadas no Console do Google Cloud.
*   **Firebase:** Projeto configurado no Console do Firebase com o arquivo `google-services.json` devidamente posicionado em `/app`.

---

## ⚡ Guia Rápido de Instalação

1. **Extraia** o ZIP do projeto.
2. Abra o **Android Studio** → `File → Open` → selecione a pasta `VIB!`.
3. Aguarde o **Gradle sync** e a indexação.
4. Adicione sua **Google Maps API Key** no arquivo `local.properties`:
   ```properties
   MAPS_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXX
   ```
5. Certifique-se de ter o arquivo `google-services.json` na pasta `/app` (configurado para `com.mediquest.app`).
6. Pressione **▶ Run** em um dispositivo físico (recomendado para Geofencing) ou emulador.

---

## 📁 Arquitetura e Estrutura Limpa

O projeto segue uma estrutura simplificada voltada para escalabilidade e fácil manutenção, dividida em duas grandes camadas:

```text
VIB!/
├── app/src/main/
│   ├── java/com/mediquest/app/
│   │   ├── MainActivity.kt              — Ponto de entrada e gestão de permissões críticas.
│   │   ├── VIBApplication.kt            — Inicialização global (Firestore Offline, Maps SDK).
│   │   ├── data/ (Camada de Dados e Lógica de Infra)
│   │   │   ├── Models.kt                — Entidades de domínio (Hotspot, User, Reporte).
│   │   │   ├── HotspotRepository.kt     — "Cérebro" do app: Fetch, Cache 24h e Heatmap Logic.
│   │   │   ├── UserRepository.kt        — Gestão de XP, Nível e Perfil no Firestore.
│   │   │   ├── GeofenceManager.kt       — Configuração das cercas virtuais nos locais.
│   │   │   ├── GeofenceBroadcastReceiver.kt — Listener passivo para entrada em locais.
│   │   │   └── SyncHotspotsWorker.kt    — Sincronização em background otimizada (12h).
│   │   └── ui/ (Camada de Interface e Interação)
│   │       ├── theme/                   — Identidade visual "Cyber-Night" (VIBTheme).
│   │       ├── AppNavigation.kt         — Grafo de navegação Jetpack Compose.
│   │       ├── VIBViewModel.kt          — Gestão de estado (StateFlow) e eventos da UI.
│   │       ├── SplashScreen.kt          — Splash screen animada.
│   │       ├── MapScreen.kt             — Heatmap dinâmico em tempo real.
│   │       └── ...                      — Telas de Ranking, Perfil e Detalhes.
```

---

## 🌡️ Algoritmo do Heatmap (The Vibe Score)

Diferente de mapas convencionais, o VIB! calcula a intensidade do calor (Alpha/Cor) através de uma fórmula ponderada:

1.  **Popularidade (Google Places):** Extraímos o volume de avaliações e nota média para definir o "peso base" do local.
2.  **Status Real (Horários):** Se o Google indica que o local está fechado, o peso é reduzido a zero, evitando brilho falso no mapa.
3.  **Voz do Usuário (Reportes):** Reportes de usuários sobre lotação (`VAZIO`, `IDEAL`, `LOTADO`) ajustam o peso dinamicamente.
4.  **Peso de Confiança:** Reportes de usuários de **Nível Alto (Veteranos)** têm mais influência no mapa do que usuários novos, prevenindo spam ou dados imprecisos.
5.  **Geofencing Crowd-Count:** Utilizamos cercas virtuais (Geofencing) para detectar passivamente a densidade de usuários do app presentes em cada local, permitindo uma estimativa em tempo real do volume de pessoas mesmo sem reportes manuais.

---

## 🚀 Soluções Técnicas Implementadas

### 1. Fix de Inicialização do Firestore
Para evitar `IllegalStateException`, a persistência offline e as configurações globais do Firebase foram centralizadas no `VIBApplication`, garantindo que o repositório nunca tente acessar o banco antes dele estar configurado.

### 2. Performance e UI Fluid (Non-blocking)
O processo de atualização do mapa (`HotspotRepository.fetchFromGoogle`) foi otimizado:
*   **Dispatchers.IO:** Toda a computação pesada e rede acontece fora da Main Thread.
*   **Firestore Write Batch:** As atualizações de múltiplos hotspots no banco são feitas em lotes únicos, reduzindo drasticamente o consumo de bateria e evitando travamentos na interface.

### 3. Cache de 24 Horas
Implementamos um mecanismo de cache inteligente. O app consulta a Google Places API apenas uma vez a cada 24 horas para cada local, salvando o estado no Firestore. Isso garante **baixo custo de API** e **alta disponibilidade offline**.

### 4. Geofencing Inteligente
O app monitora até 100 locais simultaneamente. Ao detectar que você "estacionou" em um local por mais de 10 minutos (DWELL), o sistema dispara uma notificação interativa solicitando seu feedback sobre a lotação, retroalimentando o heatmap.

---

## 🛠️ Tecnologias Utilizadas

*   **Linguagem:** Kotlin
*   **UI:** Jetpack Compose (Modern Toolkit)
*   **Backend:** Firebase (Firestore + Analytics + Auth)
*   **Maps:** Google Maps SDK + Places SDK
*   **Background:** WorkManager (Sync resiliente)
*   **Arquitetura:** MVVM (Model-View-ViewModel)

---

## 🔑 Segurança e Permissões

O VIB! respeita a privacidade do usuário e exige:
*   `ACCESS_FINE_LOCATION`: Para precisão do heatmap.
*   `POST_NOTIFICATIONS`: Para interações de geofencing (Android 13+).
*   `ACCESS_BACKGROUND_LOCATION`: Necessário para que as Geofences funcionem com o celular no bolso (essencial para a "vibe" automática).

---
