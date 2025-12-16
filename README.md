# Serverless Spring Boot 3 API with AWS Lambda SnapStart & Bedrock (GenAI)

## Table of Contents
1. [Introduction](#introduction)
2. [AWS Lambda SnapStart - Detailed Explanation](#aws-lambda-snapstart---detailed-explanation)
3. [Java in AWS Lambda](#java-in-aws-lambda)
4. [AWS Bedrock - Generative AI](#aws-bedrock---generative-ai)
5. [Project Architecture](#project-architecture)
6. [Code Analysis](#code-analysis)
7. [Configuration and Deployment](#configuration-and-deployment)
8. [Testing](#testing)
9. [Observability with AWS Lambda Powertools (Java)](#observability-with-aws-lambda-powertools-java)

---

## Introduction

This project demonstrates a production-grade Serverless architecture combining **Spring Boot 3** with AWS cloud-native services. It addresses the critical "Cold Start" issue in Java applications by utilizing SnapStart and CRaC.

### Tech Stack & Key Technologies:
- **Core Framework:** Spring Boot 3.2 (Java 21)
- **Serverless Abstraction:** Spring Cloud Function
- **Optimization:** AWS Lambda SnapStart + CRaC (Coordinated Restore at Checkpoint)
- **Generative AI:** AWS Bedrock (Claude 3.5 Sonnet)
- **Infrastructure:** Terraform (IaC)

### Project Goal
Create an efficient serverless function that uses AI models to answer user questions while minimizing response time thanks to SnapStart.

---

## AWS Lambda SnapStart - Detailed Explanation

### What is SnapStart?

AWS Lambda SnapStart is a breakthrough feature introduced by AWS that radically reduces **cold start** time for Lambda functions written in Java.

### Problem: Cold Start in Lambda

#### Traditional Lambda function lifecycle (without SnapStart):

```
1. Function invocation
   ↓
2. AWS creates new execution environment (container)
   ↓
3. Downloading function code
   ↓
4. JVM (Java Virtual Machine) initialization
   ↓
5. Loading Java classes
   ↓
6. Framework initialization (Spring Boot)
   ↓
7. Dependencies initialization (AWS SDK, Bedrock client)
   ↓
8. ONLY NOW: Function code execution
```

**Duration of steps 2-7: often 5-15 seconds for Java/Spring applications!**

### Solution: How does SnapStart work?

SnapStart uses **CRaC** (Coordinated Restore at Checkpoint) technology to create "snapshots" of the initialized environment.

#### Process with SnapStart:

**Phase 1: Deployment (one-time)**
```
1. Lambda function deployment
   ↓
2. AWS automatically:
   - Runs function in test environment
   - Initializes JVM
   - Loads all classes
   - Initializes Spring Context
   - Initializes AWS SDK and Bedrock client
   ↓
3. AWS creates SNAPSHOT (memory snapshot)
   - Saves JVM state
   - Saves loaded classes
   - Saves initialized objects
   ↓
4. Snapshot is saved and ready to use
```

**Phase 2: Function invocation (each invocation)**
```
1. Function invocation
   ↓
2. AWS restores snapshot
   - Loads saved memory state
   - Restores JVM to post-initialization state
   ↓
3. IMMEDIATELY: Function code execution

Time: ~200-500ms instead of 5-15 seconds!
```

### Key SnapStart Benefits:

1. **90% cold start time reduction**
   - Without SnapStart: 5-15 seconds
   - With SnapStart: 200-500 milliseconds

2. **Performance predictability**
   - Every invocation is fast
   - No random delays

3. **Cost savings**
   - Less initialization time = lower costs
   - Better resource efficiency

4. **Better traffic handling**
   - Faster scaling
   - Better response to traffic spikes

### Implementation in the Project

In [`terraform/main.tf`](terraform/main.tf):

```hcl
resource "aws_lambda_function" "java_snapstart_function" {
  # ... other configurations ...
  
  # KEY: Enabling SnapStart
  snap_start {
    apply_on = "PublishedVersions"  # SnapStart for published versions
  }
  
  publish = true  # REQUIRED: Publishing versions
  
  environment {
    variables = {
      # JVM optimization for SnapStart
      JAVA_TOOL_OPTIONS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    }
  }
}
```

### JVM Optimizations for SnapStart:

```bash
-XX:+TieredCompilation      # Enables tiered compilation
-XX:TieredStopAtLevel=1     # Stops at level 1 (C1 compiler)
```

**Why?**
- Level 1 compilation is faster
- Snapshot is created faster
- Smaller snapshot size
- Faster restore

---

## Java in AWS Lambda

### Why Java in Lambda?

#### Advantages:
1. **Ecosystem** - rich set of libraries and frameworks
2. **Spring Framework** - easy AWS integration
3. **Static typing** - fewer runtime errors
4. **Performance** - very fast after JVM warmup
5. **Enterprise-ready** - proven in large systems

#### Challenges:
1. **Cold start** - solved by SnapStart
2. **Package size** - larger than Python/Node.js
3. **Memory usage** - JVM requires more RAM

### Java 21 in the Project

The project uses **Java 21** - the latest LTS (Long Term Support) version.

#### Key Java 21 Features:

1. **Virtual Threads (Project Loom)**
   ```java
   // Traditional threads - heavy, limited
   Thread thread = new Thread(() -> {
       // code
   });
   
   // Virtual Threads - lightweight, millions possible
   Thread.startVirtualThread(() -> {
       // code
   });
   ```

2. **Pattern Matching**
   ```java
   // Old way
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
   
   // Deconstruction in pattern matching
   if (obj instanceof Point(int x, int y)) {
       System.out.println("x: " + x + ", y: " + y);
   }
   ```

### Spring Cloud Function

The project uses **Spring Cloud Function** - an abstraction for serverless functions.

#### Why Spring Cloud Function?

1. **Provider independence**
   ```java
   // Same code works on:
   // - AWS Lambda
   // - Azure Functions
   // - Google Cloud Functions
   // - Locally (Spring Boot)
   ```

2. **Simplicity**
   ```java
   @Bean
   public Function<String, String> askAi() {
       return question -> bedrockService.askBedrock(question);
   }
   ```

3. **Spring Boot integration**
   - Dependency Injection
   - Auto-configuration
   - Testing support

#### How does it work?

```
Lambda Invocation
    ↓
FunctionInvoker (AWS Adapter)
    ↓
Spring Cloud Function Context
    ↓
Our @Bean function
    ↓
BedrockService
    ↓
AWS Bedrock API
```

---

## AWS Bedrock - Generative AI

### What is AWS Bedrock?

AWS Bedrock is a fully managed service that provides access to **Foundation Models** from leading AI companies through a single API.

### Available Models in Bedrock:

1. **Amazon Titan** - Amazon's models
2. **Anthropic Claude** - used in this project
3. **AI21 Labs Jurassic**
4. **Cohere Command**
5. **Meta Llama 2**
6. **Stability AI Stable Diffusion** (images)

### Why Bedrock?

#### Advantages:
1. **AWS-managed**
   - No infrastructure to manage
   - Automatic scaling
   - AWS integration

2. **Security**
   - Data not used for model training
   - Encryption in transit and at rest
   - VPC endpoints

3. **Model choice**
   - Different models for different tasks
   - Easy model switching
   - Result comparison

4. **Cost**
   - Pay-per-use
   - No infrastructure fees
   - Predictable pricing

### How does Bedrock work in the Project?

#### 1. Client Configuration ([`BedrockService.java`](src/main/java/org/example/service/BedrockService.java))

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

**Key decisions:**
- `UrlConnectionHttpClient` instead of Netty/Apache
  - Smaller size
  - Faster cold start
  - Better for SnapStart

#### 2. Request Preparation

```java
String modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";

// Request structure for Claude
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

**Parameters:**
- `anthropic_version` - Claude API version
- `max_tokens` - maximum response length
- `messages` - conversation history

#### 3. Model Invocation

```java
InvokeModelRequest request = InvokeModelRequest.builder()
    .modelId(modelId)
    .contentType("application/json")
    .accept("application/json")
    .body(SdkBytes.fromUtf8String(requestBody))
    .build();

InvokeModelResponse response = bedrockClient.invokeModel(request);
```

**Process:**
1. Request is serialized to JSON
2. Sent to Bedrock API
3. Model processes the question
4. Returns response in JSON

#### 4. Response Parsing

```java
String responseBody = response.body().asUtf8String();
JsonNode jsonResponse = objectMapper.readTree(responseBody);

// Claude response structure:
// {
//   "content": [
//     {
//       "type": "text",
//       "text": "Model response..."
//     }
//   ]
// }

String answer = jsonResponse
    .path("content")
    .get(0)
    .path("text")
    .asText();
```

### Claude 3.5 Sonnet Model

The project uses **Claude 3.5 Sonnet** - Anthropic's most advanced model.

#### Characteristics:
- **Context**: 200,000 tokens (very long)
- **Performance**: Fast and accurate
- **Use cases**: 
  - Document analysis
  - Code generation
  - Question answering
  - Translations
  - Summaries

#### Cost (us-east-1):
- Input: $3.00 / 1M tokens
- Output: $15.00 / 1M tokens

**Example:**
- Question: 20 tokens
- Answer: 200 tokens
- Cost: ~$0.003 (less than 1 cent)

---

## Project Architecture

### Directory Structure

```
POC_java_snapstart_bedrock/
├── src/
│   ├── main/
│   │   └── java/org/example/
│   │       ├── Main.java                    # Entry point (unused in Lambda)
│   │       ├── function/
│   │       │   └── BedrockFunctionConfig.java  # Function definition
│   │       └── service/
│   │           └── BedrockService.java      # Bedrock logic
│   └── test/
│       └── java/org/example/function/
│           └── BedrockFunctionTest.java     # Tests
├── terraform/
│   └── main.tf                              # Infrastructure as code
├── pom.xml                                  # Maven dependencies
└── doc/
    └── TECHNICAL_GUIDE.md                   # This document
```

### Data Flow

```
┌─────────────────┐
│  User           │
│  (AWS CLI/API)  │
└────────┬────────┘
         │ Question: "What is AWS Lambda?"
         ↓
┌─────────────────────────────────────────┐
│  AWS Lambda                             │
│  ┌───────────────────────────────────┐ │
│  │ SnapStart Snapshot                │ │
│  │ - JVM initialized                 │ │
│  │ - Spring Context ready            │ │
│  │ - Bedrock Client ready            │ │
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
│  │ - Request preparation             │ │
│  │ - Bedrock invocation              │ │
│  │ - Response parsing                │ │
│  └──────────────┬────────────────────┘ │
└─────────────────┼───────────────────────┘
                  │ JSON Request
                  ↓
┌─────────────────────────────────────────┐
│  AWS Bedrock                            │
│  ┌───────────────────────────────────┐ │
│  │ Claude 3.5 Sonnet                 │ │
│  │ - Question analysis               │ │
│  │ - Response generation             │ │
│  └──────────────┬────────────────────┘ │
└─────────────────┼───────────────────────┘
                  │ JSON Response
                  ↓
         Response to user
```

### System Components

#### 1. BedrockFunctionConfig
**Role:** Lambda function definition as Spring Bean

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

**Key aspects:**
- `@Configuration` - Spring configuration class
- `@Bean` - registers function in Spring context
- `Function<String, String>` - function type (input → output)
- Name `askAi` - used in environment variable

#### 2. BedrockService
**Role:** Communication with AWS Bedrock

**Responsibilities:**
1. Bedrock client initialization
2. Request formatting
3. API invocation
4. Response parsing
5. Error handling

**Optimizations:**
```java
// Using UrlConnectionHttpClient instead of Netty
.httpClient(UrlConnectionHttpClient.builder().build())

// Why?
// - Smaller JAR size (~50MB less)
// - Faster cold start
// - Better for SnapStart (fewer classes to load)
```

#### 3. Terraform Configuration
**Role:** Infrastructure definition

**Resources:**
1. **IAM Role** - Lambda permissions
2. **IAM Policies** - Bedrock access
3. **Lambda Function** - the function itself
4. **SnapStart Config** - SnapStart configuration

---

## Code Analysis

### pom.xml - Maven Dependencies

#### Spring Cloud Function
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-function-context</artifactId>
</dependency>
```
**Purpose:** Spring Cloud Function core - function management

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-function-adapter-aws</artifactId>
</dependency>
```
**Purpose:** AWS Lambda adapter - translates Lambda invocations to Spring function calls

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
**Purpose:** Bedrock Runtime client
**Exclusions:** Remove Netty (heavy HTTP client)

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>url-connection-client</artifactId>
</dependency>
```
**Purpose:** Lightweight HTTP client based on standard `HttpURLConnection`

#### CRaC (Coordinated Restore at Checkpoint)
```xml
<dependency>
    <groupId>org.crac</groupId>
    <artifactId>crac</artifactId>
    <version>1.4.0</version>
</dependency>
```
**Purpose:** API for SnapStart - allows hooks during snapshot/restore

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

**Purpose:** Creating "uber-jar" - single JAR with all dependencies

**Transformers:**
- Merge Spring configuration files from different JARs
- Prevent conflicts
- Ensure proper Spring operation in Lambda

### BedrockService - Detailed Analysis

```java
@Service
public class BedrockService {
    private static final Logger LOG = LoggerFactory.getLogger(BedrockService.class);
    private final BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper;

    public BedrockService() {
        // Constructor initialization - executed during snapshot
        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.US_EAST_1)
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
        this.objectMapper = new ObjectMapper();
        
        LOG.info("BedrockService initialized with UrlConnectionHttpClient");
    }
```

**Key design decisions:**

1. **Constructor initialization**
   - Executed during snapshot creation
   - Client is ready immediately after restore
   - Time savings on each invocation

2. **UrlConnectionHttpClient**
   - Simple, lightweight HTTP client
   - No additional dependencies
   - Ideal for SnapStart

3. **ObjectMapper as field**
   - Reusable between invocations
   - No need to create each time

```java
public String askBedrock(String question) {
    try {
        String modelId = "anthropic.claude-3-5-sonnet-20241022-v2:0";
        
        // Request preparation in Claude format
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

**Claude request format:**
- `anthropic_version` - required API version
- `max_tokens` - response length limit (1000 tokens ≈ 750 words)
- `messages` - message array (enables conversation)
- `role: user` - message from user

**Escaping:**
```java
question.replace("\"", "\\\"")
```
- Protection against breaking JSON
- Example: `What is "AI"?` → `What is \"AI\"?`

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

**Synchronous invocation:**
- Blocks until response received
- Simple to use
- Sufficient for most cases

**Alternative (asynchronous):**
```java
// For very long operations
CompletableFuture<InvokeModelResponse> future = 
    bedrockClient.invokeModelAsync(request);
```

```java
        String responseBody = response.body().asUtf8String();
        JsonNode jsonResponse = objectMapper.readTree(responseBody);
        
        // Claude response structure:
        // {
        //   "content": [
        //     {
        //       "type": "text",
        //       "text": "Response..."
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

**JSON parsing:**
- `path()` - safe navigation (doesn't throw exception if missing)
- `get(0)` - first element of `content` array
- `asText()` - conversion to String

```java
        LOG.info("Received response from Bedrock (length: {} chars)", answer.length());
        return answer;
        
    } catch (Exception e) {
        LOG.error("Error calling Bedrock: {}", e.getMessage(), e);
        return "Error: " + e.getMessage();
    }
}
```

**Error handling:**
- Logging error details
- Returning friendly message
- Doesn't interrupt Lambda execution

---

## Configuration and Deployment

### Terraform - Infrastructure as Code

#### IAM Role for Lambda

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

**Explanation:**
- `assume_role_policy` - who can assume this role
- `Service: lambda.amazonaws.com` - only Lambda can use this role
- `sts:AssumeRole` - role assumption action

#### Basic Permissions

```hcl
resource "aws_iam_role_policy_attachment" "lambda_basic" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
  role       = aws_iam_role.lambda_role.name
}
```

**AWSLambdaBasicExecutionRole contains:**
- `logs:CreateLogGroup`
- `logs:CreateLogStream`
- `logs:PutLogEvents`

**Enables:** Writing logs to CloudWatch

#### Bedrock Permissions

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

**Actions:**
- `bedrock:InvokeModel` - model invocation

**Resource: "*"**
- Access to all models
- In production: restrict to specific model ARNs

**Restriction example:**
```hcl
Resource = [
  "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-3-5-sonnet-20241022-v2:0"
]
```

#### Lambda Function Definition

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

**Parameters:**

1. **filename**
   - Path to JAR
   - Relative to terraform/ directory

2. **handler**
   - `FunctionInvoker::handleRequest` - Spring Cloud Function adapter
   - Automatically finds `askAi` function

3. **runtime**
   - `java21` - latest Java LTS
   - Required for SnapStart

4. **timeout**
   - 30 seconds
   - Bedrock may need time for long responses

5. **memory_size**
   - 2048 MB (2 GB)
   - More memory = faster CPU
   - Important for SnapStart (faster restore)

6. **publish**
   - `true` - REQUIRED for SnapStart
   - Creates function version

```hcl
  snap_start {
    apply_on = "PublishedVersions"
  }
```

**SnapStart configuration:**
- `PublishedVersions` - snapshot for published versions
- Alternative: `None` (disabled)

```hcl
  environment {
    variables = {
      JAVA_TOOL_OPTIONS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      SPRING_CLOUD_FUNCTION_DEFINITION = "askAi"
    }
  }
}
```

**Environment variables:**

1. **JAVA_TOOL_OPTIONS**
   - JVM options
   - `-XX:+TieredCompilation` - tiered compilation
   - `-XX:TieredStopAtLevel=1` - stop at C1 (faster start)

2. **SPRING_CLOUD_FUNCTION_DEFINITION**
   - Function name to invoke
   - Must match @Bean name

### Deployment Process

#### 1. Build Project
```bash
mvn clean package -DskipTests
```

**Result:**
- `target/poc-java-snapstart-1.0.0-SNAPSHOT.jar` - Spring Boot JAR
- `target/poc-java-snapstart-1.0.0-SNAPSHOT-aws.jar` - Shaded JAR for Lambda

#### 2. Terraform Init (one-time)
```bash
cd terraform
terraform init
```

**What happens:**
- Downloading AWS provider
- Backend initialization
- Workspace preparation

#### 3. Terraform Plan
```bash
terraform plan
```

**What happens:**
- Change analysis
- Comparison with current state
- Display action plan

#### 4. Terraform Apply
```bash
terraform apply -auto-approve
```

**What happens:**
1. Creating IAM role and policies
2. Uploading JAR to Lambda
3. Creating Lambda function
4. Configuring SnapStart
5. **AWS automatically:**
   - Runs function
   - Initializes environment
   - Creates snapshot
   - Saves snapshot

**Time:** ~2-3 minutes

#### 5. Verification
```bash
aws lambda get-function \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --query "Configuration.SnapStart"
```

**Expected output:**
```json
{
    "ApplyOn": "PublishedVersions",
    "OptimizationStatus": "On"
}
```

---

## Testing

### Local Test (optional)

```bash
# Run as Spring Boot app
mvn spring-boot:run
```

**Endpoint:**
```bash
curl -X POST http://localhost:8080/askAi \
  -H "Content-Type: text/plain" \
  -d "What is AWS Lambda?"
```

### Test in AWS Lambda

#### Using the Invocation Script

The project includes a convenient shell script to invoke the Lambda function: [`scripts/invoke-lambda.sh`](scripts/invoke-lambda.sh)

**Usage:**
```bash
./scripts/invoke-lambda.sh "Your question here"
```

**Example:**
```bash
./scripts/invoke-lambda.sh "What is the price of bitcoin"
```

**What the script does:**

1. **Prerequisites validation**
   - Checks that AWS CLI is installed
   - Checks that a question was provided

2. **Payload preparation**
   ```bash
   PAYLOAD=$(cat <<EOF
   {
     "question": "$MESSAGE"
   }
   EOF
   )
   ```
   - Creates a JSON object with the question
   - Format expected by the Lambda function

3. **Function invocation**
   ```bash
   aws lambda invoke \
       --function-name "$FUNCTION_NAME" \
       --region "$REGION" \
       --payload "$PAYLOAD" \
       --cli-binary-format raw-in-base64-out \
       "$OUTPUT_FILE"
   ```
   
   **Important parameters:**
   - `--payload "$PAYLOAD"`: JSON is passed directly (no manual base64 encoding)
   - `--cli-binary-format raw-in-base64-out`: Tells AWS CLI that INPUT is in raw format (raw JSON)
     - AWS CLI automatically handles base64 encoding internally
     - OUTPUT will be in base64 if needed
   - `$OUTPUT_FILE`: File to save the response (`lambda-response.json`)

4. **Response display**
   - Uses `jq` to format JSON (if available)
   - Otherwise displays raw JSON
   - Saves to `lambda-response.json`

**Important note about `--cli-binary-format`:**
- This option defines how AWS CLI handles binary data
- `raw-in-base64-out` means:
  - **INPUT (--payload)**: raw format - JSON directly
  - **OUTPUT**: base64 if necessary
- AWS CLI automatically encodes the payload to base64 internally before sending
- ⚠️ Do not manually encode the payload to base64, or you'll get double encoding!

**Script configuration:**
```bash
FUNCTION_NAME="java-snapstart-bedrock-poc"
REGION="us-east-1"
OUTPUT_FILE="lambda-response.json"
```

#### Basic Invocation (AWS CLI direct)

```bash
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '{"question":"What is AWS Lambda SnapStart?"}' \
  --cli-binary-format raw-in-base64-out \
  response.json

cat response.json
```

**Parameter explanation:**
- `--payload` - input data (JSON object with "question" field)
- `--cli-binary-format raw-in-base64-out` - data format (raw input, base64 output if needed)
- `response.json` - response file

#### Test with Different Questions

```bash
# Technical question
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '"Explain Java virtual threads"' \
  --cli-binary-format raw-in-base64-out \
  response1.json

# Code question
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '"Write a Java function to reverse a string"' \
  --cli-binary-format raw-in-base64-out \
  response2.json

# Analytical question
aws lambda invoke \
  --function-name java-snapstart-bedrock-poc \
  --region us-east-1 \
  --payload '"Compare AWS Lambda with traditional servers"' \
  --cli-binary-format raw-in-base64-out \
  response3.json
```

### Performance Analysis

#### Check Execution Time

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

**Output contains:**
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
Init Duration: 234.56 ms  ← Initialization time (with SnapStart: ~200-500ms)
Restore Duration: 123.45 ms  ← Snapshot restore time
```

#### Comparison with/without SnapStart

**Without SnapStart:**
```
Init Duration: 8000-15000 ms  (8-15 seconds!)
Duration: 1500 ms
Total: 9500-16500 ms
```

**With SnapStart:**
```
Restore Duration: 200-500 ms
Duration: 1500 ms
Total: 1700-2000 ms
```

**Savings: ~85-90%**

### Monitoring in CloudWatch

#### Display Logs

```bash
aws logs tail /aws/lambda/java-snapstart-bedrock-poc \
  --region us-east-1 \
  --follow
```

#### Metrics

```bash
# Number of invocations
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=java-snapstart-bedrock-poc \
  --start-time 2024-01-01T00:00:00Z \
  --end-time 2024-01-02T00:00:00Z \
  --period 3600 \
  --statistics Sum \
  --region us-east-1

# Duration
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

### Unit Tests

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

**Execution:**
```bash
mvn test
```

---

## Observability with AWS Lambda Powertools (Java)

This project uses **AWS Lambda Powertools for Java** to add production-grade observability utilities without changing the Spring Cloud Function programming model.

### What’s enabled

- **Logging (JSON)**: emits structured logs to stdout with Lambda context fields (request id, cold start, function name, etc.) using Logback.
- **Metrics (CloudWatch EMF)**: emits custom metrics asynchronously via CloudWatch Embedded Metric Format.
- **Tracing (AWS X-Ray)**: creates trace subsegments via annotations and automatically instruments AWS SDK v2 client calls when tracing is enabled.

### How it’s wired in this repo

- **Handler**: [src/main/java/org/example/function/PowertoolsFunctionInvoker.java](src/main/java/org/example/function/PowertoolsFunctionInvoker.java) extends Spring Cloud Function’s AWS adapter (`FunctionInvoker`) and applies:
  - `@Logging(clearState = true)`
  - `@FlushMetrics(captureColdStart = true)`
  - `@Tracing`
- **Structured logging config**: [src/main/resources/logback.xml](src/main/resources/logback.xml) configures `LambdaJsonEncoder` for JSON output.
- **Business-level metrics**: [src/main/java/org/example/service/BedrockService.java](src/main/java/org/example/service/BedrockService.java) emits `BedrockInvoke` and `BedrockInvokeError` metrics.

### Configuration

Powertools is configured primarily via environment variables (see [terraform/main.tf](terraform/main.tf)):

- `POWERTOOLS_SERVICE_NAME` – service name included in logs/metrics/traces
- `POWERTOOLS_METRICS_NAMESPACE` – CloudWatch namespace for EMF metrics
- `POWERTOOLS_LOG_LEVEL` – log verbosity (can be overridden by `AWS_LAMBDA_LOG_LEVEL`)
- `POWERTOOLS_TRACER_CAPTURE_RESPONSE` / `POWERTOOLS_TRACER_CAPTURE_ERROR` – control whether responses/exceptions are captured as X-Ray metadata

This repo avoids logging the full prompt by default; the function logs `questionLength` instead.

### Notes for tests

The Maven test configuration disables metrics output (`POWERTOOLS_METRICS_DISABLED=true`) and sets `LAMBDA_TASK_ROOT=handler` to keep X-Ray SDK initialization happy during unit tests.

---

## Best Practices

### 1. SnapStart Optimization

#### ✅ DO:
- Initialize heavy objects in constructors
- Use lightweight HTTP clients (UrlConnectionHttpClient)
- Minimize number of dependencies
- Use Java 21 (best optimization)

#### ❌ DON'T:
- Don't initialize database connections in snapshot
- Don't use Netty/Apache HTTP Client (heavy)
- Don't load large files during initialization

### 2. Security

#### Secrets in Lambda:
```java
// ❌ BAD - hardcoded
String apiKey = "sk-1234567890";

// ✅ GOOD - AWS Secrets Manager
String apiKey = getSecretFromSecretsManager("my-api-key");

// ✅ GOOD - Environment variables (for less sensitive)
String region = System.getenv("AWS_REGION");
```

#### IAM Policies:
```hcl
# ❌ BAD - too broad permissions
Resource = "*"

# ✅ GOOD - specific resources
Resource = [
  "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-*"
]
```

### 3. Error Handling

```java
public String askBedrock(String question) {
    try {
        // Input validation
        if (question == null || question.trim().isEmpty()) {
            return "Error: Question cannot be empty";
        }
        
        // Length limit
        if (question.length() > 10000) {
            return "Error: Question too long (max 10000 characters)";
        }
        
        // Bedrock invocation
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

### 4. Costs

#### Lambda cost optimization:
- Use appropriate memory amount (2048 MB is good balance)
- Timeout: not too long (you pay for time)
- SnapStart: reduces time = lower costs

#### Bedrock cost optimization:
```java
// Limit max_tokens
"max_tokens": 500  // Instead of 1000

// Use cheaper models for simple tasks
// Claude 3 Haiku: $0.25/$1.25 per 1M tokens (5x cheaper!)
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

## Troubleshooting

### Problem 1: Long initialization time despite SnapStart

**Symptoms:**
```
Init Duration: 5000 ms
Restore Duration: 4500 ms
```

**Causes:**
1. Too many dependencies
2. Heavy HTTP client (Netty)
3. Initialization in wrong place

**Solution:**
```xml
<!-- Remove heavy dependencies -->
<exclusions>
    <exclusion>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>netty-nio-client</artifactId>
    </exclusion>
</exclusions>

<!-- Add lightweight client -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>url-connection-client</artifactId>
</dependency>
```

### Problem 2: "No qualifying bean" error

**Symptoms:**
```
NoSuchBeanDefinitionException: No qualifying bean of type 
'org.springframework.cloud.function.context.FunctionCatalog'
```

**Cause:**
Missing `spring-cloud-function-context` dependency

**Solution:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-function-context</artifactId>
</dependency>
```

### Problem 3: Timeout when calling Bedrock

**Symptoms:**
```
Task timed out after 30.00 seconds
```

**Causes:**
1. Lambda timeout too short
2. Long Bedrock response
3. Network issue

**Solution:**
```hcl
resource "aws_lambda_function" "java_snapstart_function" {
  timeout = 60  # Increase to 60 seconds
  
  # Or limit response length
  environment {
    variables = {
      MAX_TOKENS = "500"  # Shorter response
    }
  }
}
```

### Problem 4: High costs

**Symptoms:**
Unexpectedly high bills

**Analysis:**
```bash
# Check number of invocations
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Invocations \
  --dimensions Name=FunctionName,Value=java-snapstart-bedrock-poc \
  --start-time $(date -u -d '1 day ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 86400 \
  --statistics Sum
```

**Solutions:**
1. Add response caching
2. Limit max_tokens
3. Use cheaper model
4. Add rate limiting

---

## Summary

### Key Concepts

1. **SnapStart = Snapshot + Restore**
   - Snapshot during deployment
   - Restore on invocation
   - 90% cold start time reduction

2. **Java in Lambda = possible thanks to SnapStart**
   - Previously: too slow
   - Now: competitive with Python/Node.js

3. **Bedrock = Managed AI access**
   - No infrastructure
   - Multiple models
   - Secure

### Architecture

```
User → Lambda (SnapStart) → Bedrock → Claude → Response
         ↑
    Spring Cloud Function
```

### Benefits of this Approach

1. **Performance**
   - Fast start (~300ms)
   - Fast response (~1-2s)

2. **Scalability**
   - Automatic Lambda scaling
   - Automatic Bedrock scaling

3. **Costs**
   - Pay-per-use
   - No servers
   - Optimization through SnapStart

4. **Simplicity**
   - Code like regular Spring application
   - Deployment through Terraform
   - Testing through AWS CLI

### Next Steps

1. **Add caching**
   ```java
   @Cacheable("bedrock-responses")
   public String askBedrock(String question) {
       // ...
   }
   ```

2. **Add streaming**
   ```java
   // For long responses
   bedrockClient.invokeModelWithResponseStream(request)
   ```

3. **Add conversation**
   ```java
   // Store history in DynamoDB
   List<Message> history = getHistory(sessionId);
   ```

4. **Add more models**
   ```java
   // Model selection based on task
   String modelId = selectModel(taskType);
   ```

---

## Resources

### AWS Documentation
- [Lambda SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html)
- [Bedrock User Guide](https://docs.aws.amazon.com/bedrock/)
- [Claude API Reference](https://docs.anthropic.com/claude/reference)

### Spring
- [Spring Cloud Function](https://spring.io/projects/spring-cloud-function)
- [Spring Cloud Function AWS](https://docs.spring.io/spring-cloud-function/docs/current/reference/html/aws.html)

### Java
- [Java 21 Features](https://openjdk.org/projects/jdk/21/)
- [CRaC Project](https://wiki.openjdk.org/display/crac)

### Tools
- [AWS CLI](https://aws.amazon.com/cli/)
- [Terraform](https://www.terraform.io/)
- [Maven](https://maven.apache.org/)

---

**Author:** Auto-generated  
**Date:** 2024  
**Version:** 1.0
