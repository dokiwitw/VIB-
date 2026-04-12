# 🗺️ VIB! — O Waze do Rolê

O **VIB!** é uma plataforma de inteligência urbana em tempo real que utiliza a **Google Places API**, **Firestore** e **Geofencing** para oferecer uma visão térmica da "vibe" da cidade. O app resolve o problema de incerteza sobre locais (bares, baladas, eventos), utilizando dados híbridos (Google + Crowdsourcing + Presença Real) para garantir que você nunca chegue em um lugar "miado".

---

## 📋 Requisitos para Rodar o App
Para compilar e executar o VIB! com sucesso, certifique-se de que seu ambiente atenda aos seguintes critérios:

### 🛠️ Ambiente de Desenvolvimento
*   **Android Studio:** Ladybug (2024.2.1) ou superior.
*   **JDK:** Versão 17 (Necessário para compatibilidade com Gradle 8+).
*   **Gradle:** 8.4 ou superior.
*   **Kotlin:** 1.9.0+.

### 📱 Dispositivo/Emulador
*   **Android:** Versão 8.0 (API 26) ou superior.
*   **Google Play Services:** Versão atualizada (obrigatório para Maps, Places e Geofencing).
*   **Hardware:** Dispositivo físico recomendado para testes de **Haptic Feedback** e transições de GPS.

### 🔑 Credenciais Necessárias
*   **Google Maps API Key:** Com Maps SDK for Android e Places API (New) ativadas.
*   **Firebase:** Configuração de projeto Android com `google-services.json` e Firestore habilitado em modo Nativo.

---

## ⚡ Guia Rápido de Instalação
1.  **Clone o projeto** e abra no Android Studio.
2.  **Configuração de API:** No arquivo `local.properties`, adicione: `MAPS_API_KEY=AIzaSy...`.
3.  **Firebase:** Coloque o arquivo `google-services.json` na pasta `/app`.
4.  **Sync & Run:** Aguarde a sincronização do Gradle e execute no seu dispositivo.

---

## 📁 Arquitetura e Estrutura Limpa
O projeto segue o padrão MVVM (Model-View-ViewModel) com foco em reatividade e baixo acoplamento:

```text
VIB!/app/src/main/java/com/mediquest/app/
├── MainActivity.kt              — Orquestrador de permissões críticas e entrada.
├── VIBApplication.kt            — Init global: Firebase Persistence & Maps SDK.
├── data/                        — Core de Inteligência & Repositórios
│   ├── Models.kt                — Entidades: Hotspot, User, VibeState, Ranking.
│   ├── HotspotRepository.kt     — O "Cérebro": Fusão de dados e cálculo de Vibe.
│   ├── UserRepository.kt        — Gestão de Ghost Mode, XP e Persistência.
│   ├── GeofenceManager.kt       — Configuração e gestão de cercas virtuais.
│   ├── GeofenceBroadcastReceiver.kt — Listener passivo para entrada/saída.
│   └── SyncHotspotsWorker.kt    — Sincronização em background via WorkManager.
└── ui/                          — Camada de Apresentação (Jetpack Compose)
    ├── theme/                   — Design System: Inter font, Sharp shapes, Cyber colors.
    ├── AppNavigation.kt         — Grafo de navegação e transições de tela.
    ├── VIBViewModel.kt          — State holder reativo (StateFlow) e Business Logic UI.
    ├── MapScreen.kt             — Heatmap dinâmico com camadas de animação.
    ├── ProfileScreen.kt         — Dashboard de Status, Foto Base64 e Ghost Mode.
    ├── RankingScreen.kt         — Leaderboard global com sistema de medalhas.
    ├── HotspotDetailSheet.kt    — Painel reativo com gráficos de lotação e reportes.
    └── ViberMarkers.kt          — Custom Markers dinâmicos com avatares de usuários.
```

---

## 🌡️ Algoritmo do Heatmap (The Vibe Score)
O cálculo do brilho e cor de cada local é baseado em uma fórmula de **Fusão de Dados Ponderada ($V_s$)**:

$$V_s = (C_c \times 0.6) + (R_u \times 0.3) + (H_g \times 0.1)$$

*   **1. Crowd-Count (60%):** Dados em tempo real de presença física via Geofencing. Se o VIB! detecta usuários no local, o calor sobe instantaneamente.
*   **2. Social Wisdom (30%):** Reportes manuais de usuários. O impacto de cada reporte é multiplicado pelo **Nível de XP** (Veteranos > Novatos).
*   **3. Google Intelligence (10%):** Dados históricos de popularidade do Google como base de backup.
*   **4. Filtro de Status:** Se o local está marcado como "Closed" no Google, o algoritmo zera o brilho para evitar "vibe fantasma".

---

## 🚀 Soluções Técnicas Implementadas

*   **🛡️ Ghost Mode (Privacidade por Design):** Implementamos uma política de privacidade radical. Ao ativar o Ghost Mode, o app não apenas oculta o usuário, mas executa uma **deleção física e imediata** do registro de presença no Firestore, garantindo anonimato real.
*   **📈 Gamificação & XP System:** Usuários ganham XP por reportes precisos e tempo de presença em locais. Isso cria uma camada de confiança onde reportes de "Vibers" veteranos têm mais peso no algoritmo do mapa.
*   **⚡ Haptic Micro-interactions:** Integração profunda com o motor vibratório do Android. Cada ação (check-in, reporte, clique no mapa) possui um padrão de vibração único, aumentando a imersão sensorial.
*   **🖼️ Base64 Avatar Engine:** Para maximizar a performance e reduzir custos de infraestrutura, as fotos de perfil são processadas localmente e armazenadas em String Base64 diretamente no documento do usuário no Firestore.
*   **💰 Token-Saving Architecture (Google API Optimization):** Para garantir a viabilidade econômica do MVP, implementamos uma camada de cache distribuído. O app não consulta a Google Places API para cada usuário; em vez disso, o primeiro "Viber" a interagir com uma zona aciona uma atualização no Firestore com um TTL (Time-To-Live) de 24 horas. Todos os outros usuários consomem os dados diretamente do nosso banco, reduzindo o consumo de tokens em até 95%.
*   **📡 Sincronização Offline & Eficiência de Dados:** Uso da persistência nativa do Firestore e `WorkManager` para garantir que o mapa e o perfil do usuário funcionem perfeitamente mesmo em ambientes de baixa conectividade (comum em baladas e subsolos), evitando requisições redundantes de rede.
*   **✨ UI Cyber-Sharp:** Design System proprietário focado em performance:
    *   **Fonte Inter:** Máxima legibilidade em ambientes escuros.
    *   **Sharp Borders (4dp):** Visual industrial e tecnológico.
    *   **Animações Orgânicas:** Uso de `AnimatedVisibility` e `animateFloatAsState` para transições fluidas no mapa e legendas.

---

## 📈 Análise de Viabilidade Econômica

O VIB! foi desenhado para ser extremamente barato de operar, permitindo escala global com baixo investimento:

| Recurso | Sem Otimização VIB! | Com Arquitetura VIB! | Economia |
| :--- | :--- | :--- | :--- |
| **Google Places Tokens** | ~$12.750 /mês* | **$102 /mês** | **99.2%** |
| **Firebase Storage** | ~$50 /mês | **$0 (Base64)** | **100%** |
| **Custo Final por Usuário** | R$ 12,50 | **R$ 0,11** | **99%** |

*\*Projeção para 5.000 usuários ativos e 200 hotspots urbanos.*

---

## 🔑 Segurança e Permissões
Para que a "mágica" do VIB! aconteça, o app solicita:
*   `ACCESS_FINE_LOCATION`: Precisão do GPS para o heatmap social.
*   `ACCESS_BACKGROUND_LOCATION`: Necessário para o Crowd-Count funcionar com o celular no bolso.
*   `POST_NOTIFICATIONS`: Alertas inteligentes baseados no seu trajeto e locais próximos.

---

## 🔮 Roadmap Futuro
*   **VIB! Pass:** Benefícios reais em estabelecimentos para usuários de XP alto.
*   **IA de Predição:** Algoritmo que prevê a lotação de um local nas próximas 2 horas baseado no fluxo histórico.
*   **Heatmap de Trajeto:** Visualização da "vibe" das ruas entre os hotspots.

---

**VIB! — Sinta a cidade, escolha seu ritmo.**

---
*Nota: Este projeto utilizou Inteligência Artificial como ferramenta de auxílio na otimização de código e documentação, porém todas as soluções de negócio, design system, arquitetura de back-end e lógica proprietária foram projetadas e validadas pela equipe Technomixs.*
