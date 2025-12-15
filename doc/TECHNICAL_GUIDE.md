# Przewodnik Techniczny: Java Lambda z SnapStart i AWS Bedrock

## Spis treści
1. [Wprowadzenie](#wprowadzenie)
2. [AWS Lambda SnapStart - Szczegółowe wyjaśnienie](#aws-lambda-snapstart---szczegółowe-wyjaśnienie)
3. [Java w AWS Lambda](#java-w-aws-lambda)
4. [AWS Bedrock - Generatywna AI](#aws-bedrock---generatywna-ai)
5. [Architektura projektu](#architektura-projektu)
6. [Analiza kodu](#analiza-kodu)
7. [Konfiguracja i deployment](#konfiguracja-i-deployment)
8. [Testowanie](#testowanie)

---

## Wprowadzenie

Ten projekt demonstruje zaawansowane wykorzystanie AWS Lambda z trzema kluczowymi technologiami:
- **AWS Lambda SnapStart** - optymalizacja czasu zimnego startu
- **Java 21** - nowoczesna wersja Javy z wirtualnymi wątkami
- **AWS Bedrock** - usługa generatywnej AI od AWS

### Cel projektu
Stworzenie wydajnej funkcji serverless, która wykorzystuje modele AI do odpowiadania na pytania użytkowników, minimalizując jednocześnie czas odpowiedzi dzięki SnapStart.

---

## AWS Lambda SnapStart - Szczegółowe wyjaśnienie

### Czym jest SnapStart?

AWS Lambda SnapStart to przełomowa funkcja wprowadzona przez AWS, która radykalnie zmniejsza czas **zimnego startu** (cold start) funkcji Lambda napisanych w Javie.

### Problem: Zimny start w Lambda

#### Tradycyjny cykl życia funkcji Lambda (bez SnapStart):

```
1. Wywołanie funkcji
   ↓
2. AWS tworzy nowe środowisko wykonawcze (kontener)
   ↓
3. Pobieranie kodu funkcji
   ↓
4. Inicjalizacja JVM (Java Virtual Machine)
   ↓
5. Ładowanie klas Java
   ↓
6. Inicjalizacja frameworka (Spring Boot)
   ↓
7. Inicjalizacja zależności (AWS SDK, Bedrock client)
   ↓
8. DOPIERO TERAZ: Wykonanie kodu funkcji
```

**Czas trwania kroków 2-7: często 5-15 sekund dla aplikacji Java/Spring!**

### Rozwiązanie: Jak działa SnapStart?

SnapStart wykorzystuje technologię **CRaC** (Coordinated Restore at Checkpoint) do tworzenia "migawek" (snapshots) zainicjalizowanego środowiska.

#### Proces z SnapStart:

**Faza 1: Deployment (jednorazowo)**
```
1. Wdrożenie funkcji Lambda
   ↓
2. AWS automatycznie:
   - Uruchamia funkcję w środowisku testowym
   - Inicjalizuje JVM
   - Ładuje wszystkie klasy
   - Inicjalizuje Spring Context
   - Inicjalizuje AWS SDK i Bedrock client
   ↓
3. AWS tworzy SNAPSHOT (migawkę) pamięci
   - Zapisuje stan JVM
   - Zapisuje załadowane klasy
   - Zapisuje zainicjalizowane obiekty
   ↓
4. Snapshot jest zapisany i gotowy do użycia
```

**Faza 2: Wywołanie funkcji (każde wywołanie)**
```
1. Wywołanie funkcji
   ↓
2. AWS przywraca snapshot (restore)
   - Ładuje zapisany stan pamięci
   - Przywraca JVM do stanu po inicjalizacji
   ↓
3. NATYCHMIAST: Wykonanie kodu funkcji

Czas: ~200-500ms zamiast 5-15 sekund!
```

### Kluczowe korzyści SnapStart:

1. **Redukcja czasu zimnego startu o 90%**
   - Bez SnapStart: 5-15 sekund
   - Z SnapStart: 200-500 milisekund

2. **Przewidywalność wydajności**
   - Każde wywołanie jest szybkie
   - Brak losowych opóźnień

3. **Oszczędność kosztów**
   - Mniej czasu inicjalizacji = mniejsze koszty
   - Lepsza efektywność zasobów

4. **Lepsza obsługa ruchu**
   - Szybsze skalowanie
   - Lepsza reakcja na nagłe wzrosty ruchu

### Implementacja w projekcie

W pliku `terraform/main.tf`:

```hcl
resource "aws_lambda_function" "java_snapstart_function" {
  # ... inne konfiguracje ...
  
  # KLUCZOWE: Włączenie SnapStart
  snap_start {
    apply_on = "PublishedVersions"  # SnapStart dla opublikowanych wersji
  }
  
  publish = true  # WYMAGANE: Publikowanie wersji
  
  environment {
    variables = {
      # Optymalizacja JVM dla SnapStart
      JAVA_TOOL_OPTIONS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
  }
}
```

### Optymalizacje JVM dla SnapStart:

```bash
-XX:+TieredCompilation      # Włącza kompilację wielopoziomową
-XX:TieredStopAtLevel=1     # Zatrzymuje na poziomie 1 (C1 compiler)
```

**Dlaczego?**
- Poziom 1 kompilacji jest szybszy
- Snapshot jest tworzony szybciej
- Mniejszy rozmiar snapshota
- Szybsze przywracanie

---

## Java w AWS Lambda

### Dlaczego Java w Lambda?

#### Zalety:
1. **Ekosystem** - bogaty zestaw bibliotek i frameworków
2. **Spring Framework** - łatwa integracja z AWS
3. **Typowanie statyczne** - mniej błędów w runtime
4. **Wydajność** - po rozgrzaniu JVM jest bardzo szybka
5. **Enterprise-ready** - sprawdzona w dużych systemach

#### Wyzwania:
1. **Zimny start** - rozwiązany przez SnapStart
2. **Rozmiar pakietu** - większy niż Python/Node.js
3. **Zużycie pamięci** - JVM wymaga więcej RAM

### Java 21 w projekcie

Projekt wykorzystuje **Java 21** - najnowszą wersję LTS (Long Term Support).

#### Kluczowe funkcje Java 21:

1. **Virtual Threads (Project Loom)**
   ```java
   // Tradycyjne wątki - ciężkie, ograniczone
   Thread thread = new Thread(() -> {
       // kod
   });
   
   // Virtual Threads - lekkie, miliony możliwe
   Thread.startVirtualThread(() -> {
       // kod
   });
   ```

2. **Pattern Matching**
   ```java
   // Stary sposób
   if (obj instanceof String) {
       String s = (String) obj;
       System.out.println(s.length());
   }
   
   // Java 21
   if (obj instanceof String s) {
       System.out.println(s.length());
   }
   ```

3. **Record Patterns**
   ```java
   record Point(int x, int y) {}
   
   // Dekonstrukcja w pattern matching
   if (obj instanceof Point(int x, int y)) {
       System.out.println("x: " + x + ", y: " + y);
   }
   ```

### Spring Cloud Function

Projekt używa **Spring Cloud Function** - abstrakcji dla funkcji serverless.

#### Dlaczego Spring Cloud Function?

1. **Niezależność od providera**
   ```java
   // Ten sam kod działa na:
   // - AWS Lambda
   // - Azure Functions
   // - Google Cloud Functions
   // - Lokalnie (Spring Boot)
   ```

2. **Prostota**
   ```java
   @Bean
   public Function<String, String> askAi() {
       return question -> bedrockService.askBedrock(question);
   }
   ```

3. **Integracja z Spring Boot**
   - Dependency Injection
   - Auto-configuration
   - Testing support

#### Jak to działa?

```
Wywołanie Lambda
    ↓
FunctionInvoker (AWS Adapter)
    ↓
Spring Cloud Function Context
    ↓
Nasza funkcja @Bean
    ↓
BedrockService
    ↓
AWS Bedrock API
```

---

## AWS Bedrock - Generatywna AI

### Czym jest AWS Bedrock?

AWS Bedrock to w pełni zarządzana usługa, która zapewnia dostęp do **Foundation Models** (modeli podstawowych) od wiodących firm AI poprzez jedno API.

### Dostępne modele w Bedrock:

1. **Amazon Titan** - modele Amazona
2. **Anthropic Claude** - używany w tym projekcie
3. **AI21 Labs Jurassic**
4. **Cohere Command**
5. **Meta Llama 2**
6. **Stability AI Stable Diffusion** (obrazy)

### Dlaczego Bedrock?

#### Zalety:
1. **Zarządzany przez AWS**
   - Brak infrastruktury do zarządzania
   - Automatyczne skalowanie
   - Integracja z AWS

2. **Bezpieczeństwo**
   - Dane nie są używane do treningu modeli
   - Szyfrowanie w tranzycie i spoczynku
   - VPC endpoints

3. **Wybór modeli**
   - Różne modele do różnych zadań
   - Łatwa zmiana modelu
   - Porównywanie wyników

4. **Koszt**
   - Pay-per-use
   - Brak opłat za infrastrukturę
   - Przewidywalne ceny

### Jak działa Bedrock w projekcie?

#### 1. Konfiguracja klienta (BedrockService.java)

```java
@Service
public class BedrockService {
    private final BedrockRuntimeClient bedrockClient;
    
    public BedrockService() {
        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.US_EAST_1)
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
    }
}
```

**Kluczowe decyzje:**
- `UrlConnectionHttpClient` zamiast Netty/Apache
  - Mniejszy rozmiar
  - Szybszy zimny start
  - Lepszy dla SnapStart

#### 2. Przygotowanie żądania

```java
String modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";

// Struktura żądania dla Claude
String requestBody = String.format("""
    {
        "anthropic_version": "bedrock-2023-05-31",
        "max_tokens": 1000,
        "messages": [
            {
                "role": "user",
                "content": "%s"
            }
        ]
    }
    """, question.replace("\"", "\\\""));
```

**Parametry:**
- `anthropic_version` - wersja API Claude
- `max_tokens` - maksymalna długość odpowiedzi
- `messages` - historia konwersacji

#### 3. Wywołanie modelu

```java
InvokeModelRequest request = InvokeModelRequest.builder()
    .modelId(modelId)
    .contentType("application/json")
    .accept("application/json")
    .body(SdkBytes.fromUtf8String(requestBody))
    .build();

InvokeModelResponse response = bedrockClient.invokeModel(request);
```

**Proces:**
1. Żądanie jest serializowane do JSON
2. Wysyłane do Bedrock API
3. Model przetwarza pytanie
4. Zwraca odpowiedź w JSON

#### 4. Parsowanie odpowiedzi

```java
String responseBody = response.body().asUtf8String();
JsonNode jsonResponse = objectMapper.readTree(responseBody);

// Struktura odpowiedzi Claude:
// {
//   "content": [
//     {
//       "type": "text",
//       "text": "Odpowiedź modelu..."
//     }
//   ]
// }

String answer = jsonResponse
    .path("content")
    .get(0)
    .path("text")
    .asText();
```

### Model Claude 3.5 Sonnet

Projekt używa **Claude 3.5 Sonnet** - najbardziej zaawansowanego modelu Anthropic.

#### Charakterystyka:
- **Kontekst**: 200,000 tokenów (bardzo długi)
- **Wydajność**: Szybki i dokładny
- **Zastosowania**: 
  - Analiza dokumentów
  - Generowanie kodu
  - Odpowiadanie na pytania
  - Tłumaczenia
  - Podsumowania

#### Koszt (us-east-1):
- Input: $3.00 / 1M tokenów
- Output: $15.00 / 1M tokenów

**Przykład:**
- Pytanie: 20 tokenów
- Odpowiedź: 200 tokenów
- Koszt: ~$0.003 (mniej niż 1 cent)

---

## Architektura projektu

### Struktura katalogów

```
POC_java_snapstart_bedrock/
├── src/
│   ├── main/
│   │   └── java/org/example/
│   │       ├── Main.java                    # Punkt wejścia (nieużywany w Lambda)
│   │       ├── function/
│   │       │   └── BedrockFunctionConfig.java  # Definicja funkcji
│   │       └── service/
│   │           └── BedrockService.java      # Logika Bedrock
│   └── test/
│       └── java/org/example/function/
│           └── BedrockFunctionTest.java     # Testy
├── terraform/
│   └── main.tf                              # Infrastruktura jako kod
├── pom.xml                                  # Zależności Maven
└── doc/
    └── TECHNICAL_GUIDE.md                   # Ten dokument
```

### Przepływ danych

```
┌─────────────────┐
│  Użytkownik     │
│  (AWS CLI/API)  │
└────────┬────────┘
         │ Pytanie: "What is AWS Lambda?"
         ↓
┌─────────────────────────────────────────┐
│  AWS Lambda                             │
│  ┌───────────────────────────────────┐ │
│  │ SnapStart Snapshot                │ │
│  │ - JVM zainicjalizowana            │ │
│  │ - Spring Context gotowy           │ │
│  │ - Bedrock Client gotowy           │ │
│  └───────────────────────────────────┘ │
│                                         │
│  ┌───────────────────────────────────┐ │
│  │ FunctionInvoker                   │ │
│  │ (Spring Cloud Function Adapter)   │ │
│  └──────────────┬────────────────────┘ │
│                 ↓                       │
│  ┌───────────────────────────────────┐ │
│  │ BedrockFunctionConfig             │ │
│  │ @Bean askAi()                     │ │
│  └──────────────┬────────────────────┘ │
│                 ↓                       │
│  ┌───────────────────────────────────┐ │
│  │ BedrockService                    │ │
│  │ - Przygotowanie żądania           │ │
│  │ - Wywołanie Bedrock               │ │
│  │ - Parsowanie odpowiedzi           │ │
│  └──────────────┬────────────────────┘ │
└─────────────────┼───────────────────────┘
                  │ JSON Request
                  ↓
┌─────────────────────────────────────────┐
│  AWS Bedrock                            │
│  ┌───────────────────────────────────┐ │
│  │ Claude 3.5 Sonnet                 │ │
│  │ - Analiza pytania                 │ │
│  │ - Generowanie odpowiedzi          │ │
│  └──────────────┬────────────────────┘ │
└─────────────────┼───────────────────────┘
                  │ JSON Response
                  ↓
         Odpowiedź do użytkownika
```

### Komponenty systemu

#### 1. BedrockFunctionConfig
**Rola:** Definicja funkcji Lambda jako Spring Bean

```java
@Configuration
public class BedrockFunctionConfig {
    
    @Bean
    public Function<String, String> askAi() {
        return question -> {
            LOG.info("Received question: {}", question);
            return bedrockService.askBedrock(question);
        };
    }
}
```

**Kluczowe aspekty:**
- `@Configuration` - klasa konfiguracyjna Spring
- `@Bean` - rejestruje funkcję w kontekście Spring
- `Function<String, String>` - typ funkcji (input → output)
- Nazwa `askAi` - używana w zmiennej środowiskowej

#### 2. BedrockService
**Rola:** Komunikacja z AWS Bedrock

**Odpowiedzialności:**
1. Inicjalizacja klienta Bedrock
2. Formatowanie żądań
3. Wywołanie API
4. Parsowanie odpowiedzi
5. Obsługa błędów

**Optymalizacje:**
```java
// Użycie UrlConnectionHttpClient zamiast Netty
.httpClient(UrlConnectionHttpClient.builder().build())

// Dlaczego?
// - Mniejszy rozmiar JAR (~50MB mniej)
// - Szybszy zimny start
// - Lepszy dla SnapStart (mniej klas do załadowania)
```

#### 3. Terraform Configuration
**Rola:** Definicja infrastruktury

**Zasoby:**
1. **IAM Role** - uprawnienia dla Lambda
2. **IAM Policies** - dostęp do Bedrock
3. **Lambda Function** - sama funkcja
4. **SnapStart Config** - konfiguracja SnapStart

---

## Analiza kodu

### pom.xml - Zależności Maven

#### Spring Cloud Function
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-function-context</artifactId>
</dependency>
```
**Cel:** Rdzeń Spring Cloud Function - zarządzanie funkcjami

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-function-adapter-aws</artifactId>
</dependency>
```
**Cel:** Adapter dla AWS Lambda - tłumaczy wywołania Lambda na wywołania funkcji Spring

#### AWS Bedrock
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>bedrockruntime</artifactId>
    <exclusions>
        <exclusion>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>netty-nio-client</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
**Cel:** Klient Bedrock Runtime
**Exclusions:** Usuwamy Netty (ciężki klient HTTP)

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>url-connection-client</artifactId>
</dependency>
```
**Cel:** Lekki klient HTTP oparty na standardowym `HttpURLConnection`

#### CRaC (Coordinated Restore at Checkpoint)
```xml
<dependency>
    <groupId>org.crac</groupId>
    <artifactId>crac</artifactId>
    <version>1.4.0</version>
</dependency>
```
**Cel:** API dla SnapStart - pozwala na hooks podczas snapshot/restore

### Maven Shade Plugin

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <configuration>
        <shadedClassifierName>aws</shadedClassifierName>
        <transformers>
            <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                <resource>META-INF/spring.handlers</resource>
            </transformer>
            <transformer implementation="org.springframework.boot.maven.PropertiesMergingResourceTransformer">
                <resource>META-INF/spring.factories</resource>
            </transformer>
        </transformers>
    </configuration>
</plugin>
```

**Cel:** Tworzenie "uber-jar" - jednego JAR z wszystkimi zależnościami

**Transformers:**
- Łączą pliki konfiguracyjne Spring z różnych JARów
- Zapobiegają konfliktom
- Zapewniają poprawne działanie Spring w Lambda

### BedrockService - Szczegółowa analiza

```java
@Service
public class BedrockService {
    private static final Logger LOG = LoggerFactory.getLogger(BedrockService.class);
    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;

    public BedrockService() {
        // Inicjalizacja w konstruktorze - wykonana podczas snapshot
        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.US_EAST_1)
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
        this.objectMapper = new ObjectMapper();
        
        LOG.info("BedrockService initialized with UrlConnectionHttpClient");
    }
```

**Kluczowe decyzje projektowe:**

1. **Inicjalizacja w konstruktorze**
   - Wykonana podczas tworzenia snapshota
   - Klient jest gotowy od razu po restore
   - Oszczędność czasu przy każdym wywołaniu

2. **UrlConnectionHttpClient**
   - Prosty, lekki klient HTTP
   - Brak dodatkowych zależności
   - Idealny dla SnapStart

3. **ObjectMapper jako pole**
   - Reużywalny między wywołaniami
   - Nie trzeba tworzyć za każdym razem

```java
public String askBedrock(String question) {
    try {
        String modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";
        
        // Przygotowanie żądania w formacie Claude
        String requestBody = String.format("""
            {
                "anthropic_version": "bedrock-2023-05-31",
                "max_tokens": 1000,
                "messages": [
                    {
                        "role": "user",
                        "content": "%s"
                    }
                ]
            }
            """, question.replace("\"", "\\\""));
```

**Format żądania Claude:**
- `anthropic_version` - wymagana wersja API
- `max_tokens` - limit długości odpowiedzi (1000 tokenów ≈ 750 słów)
- `messages` - tablica wiadomości (umożliwia konwersację)
- `role: user` - wiadomość od użytkownika

**Escape'owanie:**
```java
question.replace("\"", "\\\"")
```
- Zabezpieczenie przed złamaniem JSON
- Przykład: `What is "AI"?` → `What is \"AI\"?`

```java
        InvokeModelRequest request = InvokeModelRequest.builder()
            .modelId(modelId)
            .contentType("application/json")
            .accept("application/json")
            .body(SdkBytes.fromUtf8String(requestBody))
            .build();

        LOG.info("Invoking Bedrock model: {}", modelId);
        InvokeModelResponse response = bedrockClient.invokeModel(request);
```

**Wywołanie synchroniczne:**
- Blokuje do otrzymania odpowiedzi
- Proste w użyciu
- Wystarczające dla większości przypadków

**Alternatywa (asynchroniczna):**
```java
// Dla bardzo długich operacji
CompletableFuture<InvokeModelResponse> future = 
    bedrockClient.invokeModelAsync(request);
```

```java
        String responseBody = response.body().asUtf8String();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        
        // Struktura odpowiedzi Claude:
        // {
        //   "content": [
        //     {
        //       "type": "text",
        //       "text": "Odpowiedź..."
        //     }
        //   ],
        //   "usage": {
        //     "input_tokens": 20,
        //     "output_tokens": 150
        //   }
        // }
        
        String answer = jsonResponse
            .path("content")
            .get(0)
            .path("text")
            .asText();
```

**Parsowanie JSON:**
- `path()` - bezpieczne nawigowanie (nie rzuca wyjątku jeśli brak)
- `get(0)` - pierwszy element tablicy `content`
- `asText()` - konwersja do String

```java
        LOG.info("Received response from Bedrock (length: {} chars)", answer.length());
        return answer;
        
    } catch (Exception e) {
        LOG.error("Error calling Bedrock: {}", e.getMessage(), e);
        return "Error: " + e.getMessage();
    }
}
```

**Obsługa błędów:**
- Logowanie szczegółów błędu
- Zwracanie przyjaznej wiadomości
- Nie przerywa działania Lambda

---

## Konfiguracja i deployment

### Terraform - Infrastruktura jako kod

#### IAM Role dla Lambda

```hcl
resource "aws_iam_role" "lambda_role" {
  name = "java_snapstart_bedrock_role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}
```

**Wyjaśnienie:**
- `assume_role_policy` - kto może przyjąć tę rolę
- `Service: lambda.amazonaws.com` - tylko Lambda może użyć tej roli
- `sts:AssumeRole` - akcja przyjęcia roli

#### Uprawnienia podstawowe

```hcl
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
  role       = aws_iam_role.lambda_role.name
}
```

**AWSLambdaBasicExecutionRole zawiera:**
- `logs:CreateLogGroup`
- `logs:CreateLogStream`
- `logs:PutLogEvents`

**Umożliwia:** Zapisywanie logów do CloudWatch

#### Uprawnienia do Bedrock

```hcl
resource "aws_iam_policy" "bedrock_policy" {
  name        = "bedrock_invoke_policy"
  description = "Allow Lambda to invoke Bedrock models"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = [
          "bedrock:InvokeModel"
        ]
        Effect   = "Allow"
        Resource = "*"
      }
    ]
  })
}
```

**Akcje:**
- `bedrock:InvokeModel` - wywołanie modelu

**Resource: "*"**
- Dostęp do wszystkich modeli
- W produkcji: ograniczyć do konkretnych ARN modeli

**Przykład ograniczenia:**
```hcl
Resource = [
  "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-5-sonnet-20241022-v2:0"
]
```

#### Definicja funkcji Lambda

```hcl
resource "aws_lambda_function" "java_snapstart_function" {
  filename      = "../target/poc-java-snapstart-1.0.0-SNAPSHOT-aws.jar"
  function_name = "java-snapstart-bedrock-poc"
  role          = aws_iam_role.lambda_role.arn
  handler       = "org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest"
  runtime       = "java21"
  timeout       = 30
  memory_size   = 2048
  publish       = true
```

**Parametry:**

1. **filename**
   - Ścieżka do JAR
   - Względna do katalogu terraform/

2. **handler**
   - `FunctionInvoker::handleRequest` - adapter Spring Cloud Function
   - Automatycznie znajduje funkcję `askAi`

3. **runtime**
   - `java21` - najnowsza Java LTS
   - Wymagana dla SnapStart

4. **timeout**
   - 30 sekund
   - Bedrock może potrzebować czasu na długie odpowiedzi

5. **memory_size**
   - 2048 MB (2 GB)
   - Więcej pamięci = szybszy CPU
   - Ważne dla SnapStart (szybsze restore)

6. **publish**
   - `true` - WYMAGANE dla SnapStart
   - Tworzy wersję funkcji

```hcl
  snap_start {
    apply_on = "PublishedVersions"
  }
```

**SnapStart configuration:**
- `PublishedVersions` - snapshot dla opublikowanych wersji
- Alternatywa: `None` (wyłączone)

```hcl
  environment {
    variables = {
      JAVA_TOOL_OPTIONS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      SPRING_CLOUD_FUNCTION_DEFINITION = "askAi"
    }
  }
}
```

**Zmienne środowiskowe:**

1. **JAVA_TOOL_OPTIONS**
   - Opcje JVM
   - `-XX:+TieredCompilation` - kompilacja wielopoziomowa
   - `-XX:TieredStopAtLevel=1` - zatrzymaj na C1 (szybszy start)

2. **SPRING_CLOUD_FUNCTION_DEFINITION**
   - Nazwa funkcji do wywołania
   - Musi odpowiadać nazwie @Bean

### Proces deployment

#### 1. Build projektu
```bash
mvn clean package -DskipTests
```

**Rezultat:**
- `target/poc-java-snapstart-1.0.0-SNAPSHOT.jar` - Spring Boot JAR
- `target/poc-java-snapstart-1.0.0-SNAPSHOT-aws.jar` - Shaded JAR dla Lambda

#### 2. Terraform init (jednorazowo)
```bash
cd terraform
terraform init
```

**Co się dzieje:**
- Pobieranie providera AWS
- Inicjalizacja backend
- Przygotowanie workspace

#### 3. Terraform plan
```bash
terraform plan
```

**Co się dzieje:**
- Analiza zmian
- Porównanie z aktualnym stanem
- Wyświetlenie planu działań

#### 4. Terraform apply
```bash
terraform apply -auto-approve
```

**Co się dzieje:**
1. Tworzenie IAM role i policies
2. Upload JAR do Lambda
3. Tworzenie funkcji Lambda
4. Konfiguracja SnapStart
5. **AWS automatycznie:**
   - Uruchamia funkcję
   - Inicjalizuje środowisko
   - Tworzy snapshot
   - Zapisuje snapshot

**Czas:** ~2-3 minuty

#### 5. Weryfikacja
```bash
aws lambda get-function \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --query "Configuration.SnapStart"
```

**Oczekiwany output:**
```json
{
    "ApplyOn": "PublishedVersions",
    "OptimizationStatus": "On"
}
```

---

## Testowanie

### Test lokalny (opcjonalny)

```bash
# Uruchomienie jako Spring Boot app
mvn spring-boot:run
```

**Endpoint:**
```bash
curl -X POST http://localhost:8080/askAi \
  -H "Content-Type: text/plain" \
  -d "What is AWS Lambda?"
```

### Test w AWS Lambda

#### Podstawowe wywołanie

```bash
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '"What is AWS Lambda SnapStart?"' \
  --cli-binary-format raw-in-base64-out \
  response.json

cat response.json
```

**Wyjaśnienie parametrów:**
- `--payload` - dane wejściowe (JSON string)
- `--cli-binary-format raw-in-base64-out` - format danych
- `response.json` - plik z odpowiedzią

#### Test z różnymi pytaniami

```bash
# Pytanie techniczne
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '"Explain Java virtual threads"' \
  --cli-binary-format raw-in-base64-out \
  response1.json

# Pytanie o kod
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '"Write a Java function to reverse a string"' \
  --cli-binary-format raw-in-base64-out \
  response2.json

# Pytanie analityczne
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '"Compare AWS Lambda with traditional servers"' \
  --cli-binary-format raw-in-base64-out \
  response3.json
```

### Analiza wydajności

#### Sprawdzenie czasu wykonania

```bash
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '"Hello"' \
  --cli-binary-format raw-in-base64-out \
  response.json \
  --log-type Tail \
  --query 'LogResult' \
  --output text | base64 -d
```

**Output zawiera:**
```
INIT_START Runtime Version: java:21.v20
START RequestId: abc-123
Received question: Hello
Invoking Bedrock model: anthropic.claude-3-5-sonnet-20241022-v2:0
Received response from Bedrock (length: 234 chars)
END RequestId: abc-123
REPORT RequestId: abc-123
Duration: 1234.56 ms
Billed Duration: 1235 ms
Memory Size: 2048 MB
Max Memory Used: 456 MB
Init Duration: 234.56 ms  ← Czas inicjalizacji (z SnapStart: ~200-500ms)
Restore Duration: 123.45 ms  ← Czas restore snapshota
```

#### Porównanie z/bez SnapStart

**Bez SnapStart:**
```
Init Duration: 8000-15000 ms  (8-15 sekund!)
Duration: 1500 ms
Total: 9500-16500 ms
```

**Z SnapStart:**
```
Restore Duration: 200-500 ms
Duration: 1500 ms
Total: 1700-2000 ms
```

**Oszczędność: ~85-90%**

### Monitoring w CloudWatch

#### Wyświetlenie logów

```bash
aws logs tail /aws/lambda/java-snapstart-bedrock-poc \
  --region us-east-1 \
  --follow
```

#### Metryki

```bash
# Liczba wywołań
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=java-snapstart-bedrock-poc \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-02T00:00:00Z \
  --period 3600 \
  --statistics Sum \
  --region us-east-1

# Czas trwania
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Duration \
  --dimensions Name=FunctionName,Value=java-snapstart-bedrock-poc \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-02T00:00:00Z \
  --period 3600 \
  --statistics Average,Maximum \
  --region us-east-1
```

### Testy jednostkowe

```java
@SpringBootTest
class BedrockFunctionTest {
    
    @Autowired
    private Function<String, String> askAi;
    
    @Test
    void testAskAi() {
        String question = "What is 2+2?";
        String answer = askAi.apply(question);
        
        assertNotNull(answer);
        assertFalse(answer.isEmpty());
        assertTrue(answer.contains("4"));
    }
}
```

**Uruchomienie:**
```bash
mvn test
```

---

## Najlepsze praktyki

### 1. Optymalizacja SnapStart

#### ✅ DO:
- Inicjalizuj ciężkie obiekty w konstruktorach
- Używaj lekkich klientów HTTP (UrlConnectionHttpClient)
- Minimalizuj liczbę zależności
- Używaj Java 21 (najlepsza optymalizacja)

#### ❌ DON'T:
- Nie inicjalizuj połączeń bazodanowych w snapshot
- Nie używaj Netty/Apache HTTP Client (ciężkie)
- Nie ładuj dużych plików podczas inicjalizacji

### 2. Bezpieczeństwo

#### Secrets w Lambda:
```java
// ❌ ZŁE - hardcoded
String apiKey = "sk-1234567890";

// ✅ DOBRE - AWS Secrets Manager
String apiKey = getSecretFromSecretsManager("my-api-key");

// ✅ DOBRE - Zmienne środowiskowe (dla mniej wrażliwych)
String region = System.getenv("AWS_REGION");
```

#### IAM Policies:
```hcl
# ❌ ZŁE - zbyt szerokie uprawnienia
Resource = "*"

# ✅ DOBRE - konkretne zasoby
Resource = [
  "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-*"
]
```

### 3. Obsługa błędów

```java
public String askBedrock(String question) {
    try {
        // Walidacja input
        if (question == null || question.trim().isEmpty()) {
            return "Error: Question cannot be empty";
        }
        
        // Limit długości
        if (question.length() > 10000) {
            return "Error: Question too long (max 10000 characters)";
        }
        
        // Wywołanie Bedrock
        // ...
        
    } catch (BedrockException e) {
        LOG.error("Bedrock error: {}", e.awsErrorDetails().errorMessage());
        return "Error: Bedrock service error";
    } catch (SdkClientException e) {
        LOG.error("AWS SDK error: {}", e.getMessage());
        return "Error: AWS connection error";
    } catch (Exception e) {
        LOG.error("Unexpected error: {}", e.getMessage(), e);
        return "Error: Internal error";
    }
}
```

### 4. Koszty

#### Optymalizacja kosztów Lambda:
- Używaj odpowiedniej ilości pamięci (2048 MB to dobry balans)
- Timeout: nie za długi (płacisz za czas)
- SnapStart: zmniejsza czas = mniejsze koszty

#### Optymalizacja kosztów Bedrock:
```java
// Ogranicz max_tokens
"max_tokens": 500  // Zamiast 1000

// Używaj tańszych modeli dla prostych zadań
// Claude 3 Haiku: $0.25/$1.25 per 1M tokens (5x taniej!)
String modelId = "anthropic.claude-3-haiku-20240307-v1:0";
```

### 5. Monitoring

#### CloudWatch Alarms:
```hcl
resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "lambda-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = "60"
  statistic           = "Sum"
  threshold           = "5"
  alarm_description   = "Lambda function errors"
  
  dimensions = {
    FunctionName = aws_lambda_function.java_snapstart_function.function_name
  }
}
```

---

## Rozwiązywanie problemów

### Problem 1: Długi czas inicjalizacji mimo SnapStart

**Objawy:**
```
Init Duration: 5000 ms
Restore Duration: 4500 ms
```

**Przyczyny:**
1. Zbyt wiele zależności
2. Ciężki klient HTTP (Netty)
3. Inicjalizacja w złym miejscu

**Rozwiązanie:**
```xml
<!-- Usuń ciężkie zależności -->
<exclusions>
    <exclusion>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>netty-nio-client</artifactId>
    </exclusion>
</exclusions>

<!-- Dodaj lekki klient -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>url-connection-client</artifactId>
</dependency>
```

### Problem 2: Błąd "No qualifying bean"

**Objawy:**
```
NoSuchBeanDefinitionException: No qualifying bean of type 
'org.springframework.cloud.function.context.FunctionCatalog'
```

**Przyczyna:**
Brak zależności `spring-cloud-function-context`

**Rozwiązanie:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-function-context</artifactId>
</dependency>
```

### Problem 3: Timeout przy wywołaniu Bedrock

**Objawy:**
```
Task timed out after 30.00 seconds
```

**Przyczyny:**
1. Zbyt krótki timeout Lambda
2. Długa odpowiedź Bedrock
3. Problem z siecią

**Rozwiązanie:**
```hcl
resource "aws_lambda_function" "java_snapstart_function" {
  timeout = 60  # Zwiększ do 60 sekund
  
  # Lub ogranicz długość odpowiedzi
  environment {
    variables = {
      MAX_TOKENS = "500"  # Krótsza odpowiedź
    }
  }
}
```

### Problem 4: Wysokie koszty

**Objawy:**
Nieoczekiwanie wysokie rachunki

**Analiza:**
```bash
# Sprawdź liczbę wywołań
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=java-snapstart-bedrock-poc \
  --start-time $(date -u -d '1 day ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 86400 \
  --statistics Sum
```

**Rozwiązania:**
1. Dodaj caching odpowiedzi
2. Ogranicz max_tokens
3. Użyj tańszego modelu
4. Dodaj rate limiting

---

## Podsumowanie

### Kluczowe koncepty

1. **SnapStart = Snapshot + Restore**
   - Snapshot podczas deployment
   - Restore przy wywołaniu
   - 90% redukcja czasu zimnego startu

2. **Java w Lambda = możliwe dzięki SnapStart**
   - Wcześniej: zbyt wolne
   - Teraz: konkurencyjne z Python/Node.js

3. **Bedrock = Zarządzany dostęp do AI**
   - Bez infrastruktury
   - Wiele modeli
   - Bezpieczne

### Architektura

```
User → Lambda (SnapStart) → Bedrock → Claude → Response
         ↑
    Spring Cloud Function
```

### Korzyści tego podejścia

1. **Wydajność**
   - Szybki start (~300ms)
   - Szybka odpowiedź (~1-2s)

2. **Skalowalność**
   - Automatyczne skalowanie Lambda
   - Automatyczne skalowanie Bedrock

3. **Koszty**
   - Pay-per-use
   - Brak serwerów
   - Optymalizacja przez SnapStart

4. **Prostota**
   - Kod jak zwykła aplikacja Spring
   - Deployment przez Terraform
   - Testowanie przez AWS CLI

### Dalsze kroki

1. **Dodaj caching**
   ```java
   @Cacheable("bedrock-responses")
   public String askBedrock(String question) {
       // ...
   }
   ```

2. **Dodaj streaming**
   ```java
   // Dla długich odpowiedzi
   bedrockClient.invokeModelWithResponseStream(request)
   ```

3. **Dodaj konwersację**
   ```java
   // Przechowuj historię w DynamoDB
   List<Message> history = getHistory(sessionId);
   ```

4. **Dodaj więcej modeli**
   ```java
   // Wybór modelu w zależności od zadania
   String modelId = selectModel(taskType);
   ```

---

## Zasoby

### Dokumentacja AWS
- [Lambda SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html)
- [Bedrock User Guide](https://docs.aws.amazon.com/bedrock/)
- [Claude API Reference](https://docs.anthropic.com/claude/reference)

### Spring
- [Spring Cloud Function](https://spring.io/projects/spring-cloud-function)
- [Spring Cloud Function AWS](https://docs.spring.io/spring-cloud-function/docs/current/reference/html/aws.html)

### Java
- [Java 21 Features](https://openjdk.org/projects/jdk/21/)
- [CRaC Project](https://wiki.openjdk.org/display/crac)

### Narzędzia
- [AWS CLI](https://aws.amazon.com/cli/)
- [Terraform](https://www.terraform.io/)
- [Maven](https://maven.apache.org/)

---

**Autor:** Wygenerowano automatycznie  
**Data:** 2024  
**Wersja:** 1.0
