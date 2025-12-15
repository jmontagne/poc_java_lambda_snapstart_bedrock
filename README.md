# Java SnapStart & Bedrock POC

## Business Value
This Proof of Concept (POC) demonstrates a high-performance, serverless architecture for Generative AI applications using Java. By leveraging **AWS Lambda SnapStart** and **Amazon Bedrock**, we achieve:

*   **Enterprise-Grade Security**: Utilizing the robust Java ecosystem and AWS IAM for fine-grained access control.
*   **Ultra-Low Latency**: Reducing Java "Cold Starts" from typical 6+ seconds to **under 500ms** using SnapStart (CRaC), making Java viable for interactive GenAI apps.
*   **Cost Efficiency**: Serverless pay-per-use model combined with optimized runtime performance.
*   **Rapid Innovation**: Integrating with state-of-the-art models (Claude 3) via Amazon Bedrock without managing infrastructure.

## Architecture
*   **Runtime**: Java 21 on AWS Lambda
*   **Framework**: Spring Boot 3.2 with Spring Cloud Function
*   **Optimization**: AWS SnapStart enabled with CRaC (Coordinated Restore at Checkpoint) hooks for pre-warming connections.
*   **AI Service**: Amazon Bedrock (Claude 3 Haiku)

## Prerequisites
*   Java 21 JDK
*   Maven 3.9+
*   Terraform
*   AWS CLI configured with appropriate permissions

## Build & Deploy

1.  **Build the project:**
    ```bash
    mvn clean package
    ```

2.  **Deploy with Terraform:**
    ```bash
    cd terraform
    terraform init
    terraform apply
    ```

3.  **Test:**
    Invoke the Lambda function with a JSON string payload (e.g., `"Explain quantum computing in one sentence"`).

## Key Implementation Details
*   **`BedrockService.java`**: Implements `org.crac.Resource` to initialize the Bedrock client *before* the snapshot is taken (`beforeCheckpoint`). This moves the heavy initialization cost to the build phase, not the request phase.
*   **`pom.xml`**: Uses `url-connection-client` instead of the default Apache/Netty HTTP clients to reduce the artifact size and startup overhead.
*   **`main.tf`**: Configures `snap_start` and sets `JAVA_TOOL_OPTIONS` for tiered compilation, further optimizing startup speed.

