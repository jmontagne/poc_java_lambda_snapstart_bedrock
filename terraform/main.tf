terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

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

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
  role       = aws_iam_role.lambda_role.name
}

resource "aws_iam_role_policy_attachment" "lambda_xray" {
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
  role       = aws_iam_role.lambda_role.name
}

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
        Resource = "*" # In production, restrict to specific model ARNs
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "bedrock_attach" {
  policy_arn = aws_iam_policy.bedrock_policy.arn
  role       = aws_iam_role.lambda_role.name
}

resource "aws_lambda_function" "java_snapstart_function" {
  filename      = "../target/poc-java-snapstart-1.0.0-SNAPSHOT-aws.jar"
  function_name = "java-snapstart-bedrock-poc"
  role          = aws_iam_role.lambda_role.arn
  handler       = "org.example.function.PowertoolsFunctionInvoker::handleRequest"
  runtime       = "java21"
  timeout       = 30
  memory_size   = 2048 # SnapStart benefits from higher memory during restore
  publish       = true
  source_code_hash = filebase64sha256("../target/poc-java-snapstart-1.0.0-SNAPSHOT-aws.jar")

  tracing_config {
    mode = "Active"
  }

  # CRITICAL: Enable SnapStart
  snap_start {
    apply_on = "PublishedVersions"
  }

  environment {
    variables = {
      # Optimization for SnapStart
      JAVA_TOOL_OPTIONS = "-XX:+TieredCompilation -XX:TieredStopAtLevel=1"
      # Spring Cloud Function definition
      SPRING_CLOUD_FUNCTION_DEFINITION = "askAi"

      # AWS Lambda Powertools (Java)
      POWERTOOLS_SERVICE_NAME       = "java-snapstart-bedrock-poc"
      POWERTOOLS_METRICS_NAMESPACE  = "POCJavaSnapStartBedrock"
      POWERTOOLS_LOG_LEVEL          = "INFO"
      POWERTOOLS_TRACER_CAPTURE_RESPONSE = "false"
      POWERTOOLS_TRACER_CAPTURE_ERROR    = "false"
    }
  }
}

# Output the function name
output "lambda_function_name" {
  value = aws_lambda_function.java_snapstart_function.function_name
}

