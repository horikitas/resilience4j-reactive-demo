#!/bin/bash

URL="http://localhost:8080/api/place-order"
CB_STATE_URL="http://localhost:8080/actuator/circuitbreakers"
TOTAL_CALLS=30
SLEEP_SECONDS=2   # Increased sleep between requests

echo "Starting Resilience4j simulation with longer OPEN state and 40% failure rate..."

for ((i=1; i<=TOTAL_CALLS; i++)); do
  ORDER_ID=$i
  PRODUCT_ID=$i

  # Inject 4 failures into the last 10 calls (calls 21–24) to trip CB
  if (( i >= 15 && i <= 20 )); then
    FULL_URL="$URL?simulateFail=true"
    echo "[$i] ❌ Simulating FAILURE: orderId=$ORDER_ID, productId=$PRODUCT_ID"
    if(( i == 20 )); then
      sleep $SLEEP_SECONDS
    fi
  else
    FULL_URL="$URL"
    echo "[$i] ✅ Simulating SUCCESS: orderId=$ORDER_ID, productId=$PRODUCT_ID"
  fi

  # Send API request and log response
  curl -s -o - -w "\nStatus: %{http_code}\n" -H "Content-Type: application/json" -X POST "$FULL_URL" \
    -d '{
      "orderId": '"$ORDER_ID"',
      "productId": '"$PRODUCT_ID"'
    }'

  # Print circuit breaker state (raw JSON)
  echo "Circuit Breaker State:"
  curl -s "$CB_STATE_URL"
  echo -e "\n========== SLEEPING ($SLEEP_SECONDS s) ==========\n"

  sleep "$SLEEP_SECONDS"
done

echo "Simulation complete."
