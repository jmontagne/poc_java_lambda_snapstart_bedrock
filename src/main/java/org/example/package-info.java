/**
 * Serverless Java 21 + AWS Lambda SnapStart + Amazon Bedrock — Reference Architecture.
 *
 * <p>A production-ready pattern for deploying LLM inference as a serverless function
 * with <b>sub-500ms cold starts</b> using AWS Lambda SnapStart (CRaC). Demonstrates
 * how to build cost-efficient, observable GenAI endpoints for regulated enterprises
 * (banking, insurance).</p>
 *
 * <h2>Architecture Highlights</h2>
 * <ul>
 *   <li><b>SnapStart + CRaC:</b> {@link org.example.service.BedrockService} implements
 *       {@code org.crac.Resource} — Bedrock client is pre-initialized at checkpoint time,
 *       reducing cold start from ~5s to ~200ms.</li>
 *   <li><b>Spring Cloud Function:</b> {@link org.example.function.BedrockFunctionConfig}
 *       exposes a {@code Function<String, String>} bean invoked via Lambda.</li>
 *   <li><b>Observability:</b> {@link org.example.function.PowertoolsFunctionInvoker}
 *       wraps invocations with structured logging, X-Ray tracing, and CloudWatch metrics.</li>
 *   <li><b>Cost optimization:</b> Uses Claude 3 Haiku ($0.00025/$0.00125 per 1K tokens)
 *       with lightweight UrlConnectionHttpClient (faster startup than Netty).</li>
 * </ul>
 *
 * <h2>Tech Stack</h2>
 * <ul>
 *   <li>Java 21, Spring Boot 3.2, Spring Cloud Function</li>
 *   <li>AWS SDK v2 (BedrockRuntimeClient, UrlConnectionHttpClient)</li>
 *   <li>AWS Lambda SnapStart + CRaC 1.4</li>
 *   <li>Lambda Powertools 2.8 (@Logging, @Tracing, @FlushMetrics)</li>
 *   <li>X-Ray TracingInterceptor on Bedrock client</li>
 *   <li>Terraform IaC (Lambda, IAM, SnapStart config)</li>
 * </ul>
 *
 * @see org.example.service.BedrockService CRaC-aware Bedrock client with SnapStart
 * @see org.example.function.PowertoolsFunctionInvoker Lambda handler with Powertools
 */
package org.example;
