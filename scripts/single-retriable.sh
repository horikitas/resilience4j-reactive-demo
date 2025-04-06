#!/bin/bash

URL="http://localhost:8080/api/place-order"
RETRIES_ACTUATOR_URL="http://localhost:8080/actuator/retries"
TOTAL_CALLS=30
SLEEP_SECONDS=1
ORDER_ID=1
PRODUCT_ID=1
FULL_URL="$URL?simulateFail=true"

curl -s -o - -w "\nStatus: %{http_code}\n" -H "Content-Type: application/json" -X POST "$FULL_URL" \
    -d '{
      "orderId": '"$ORDER_ID"',
      "productId": '"$PRODUCT_ID"'
    }'

# Print circuit breaker state (raw JSON)
echo "Retry State:"
curl -s "$RETRIES_ACTUATOR_URL"
echo -e "\n========== SLEEPING ($SLEEP_SECONDS s) ==========\n"

sleep "$SLEEP_SECONDS"