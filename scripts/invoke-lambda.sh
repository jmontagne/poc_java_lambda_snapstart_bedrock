#!/bin/bash

# Script to invoke the Lambda function with AWS CLI
# Usage: ./invoke-lambda.sh "Your question here"

# Configuration
FUNCTION_NAME="java-snapstart-bedrock-poc"
REGION="us-east-1"
OUTPUT_FILE="lambda-response.json"

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "❌ Error: AWS CLI is not installed"
    echo "Please install AWS CLI: https://aws.amazon.com/cli/"
    exit 1
fi

# Check if a message was provided
if [ -z "$1" ]; then
    echo "❌ Error: No message provided"
    echo "Usage: $0 \"Your question here\""
    echo "Example: $0 \"What is AWS Lambda SnapStart?\""
    exit 1
fi

MESSAGE="$1"

# Create the payload
PAYLOAD=$(cat <<EOF
{
  "question": "$MESSAGE"
}
EOF
)

echo "🚀 Invoking Lambda function: $FUNCTION_NAME"
echo "📝 Message: $MESSAGE"
echo "📍 Region: $REGION"
echo ""

# Invoke the Lambda function
aws lambda invoke \
    --function-name "$FUNCTION_NAME" \
    --region "$REGION" \
    --payload "$PAYLOAD" \
    --cli-binary-format raw-in-base64-out \
    "$OUTPUT_FILE"

# Check if the invocation was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Lambda invoked successfully!"
    echo "📄 Response saved to: $OUTPUT_FILE"
    echo ""
    echo "📖 Response content:"
    echo "===================="
    cat "$OUTPUT_FILE" | jq '.' 2>/dev/null || cat "$OUTPUT_FILE"
    echo ""
else
    echo ""
    echo "❌ Lambda invocation failed!"
    exit 1
fi
