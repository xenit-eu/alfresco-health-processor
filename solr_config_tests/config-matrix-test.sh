#!/bin/bash

# Function to wait for Alfresco to respond
wait_for_alfresco() {
  echo "Waiting for Alfresco to be available..."
  until curl --output /dev/null --silent --fail http://localhost:8080/alfresco/s/api/server; do
    printf '.'
    sleep 5
  done
  echo "Alfresco is up!"
}

# Function to run the test sequence
run_test() {
  local compose_files="$1"

  echo "Removing solr"
  docker rm -fv alfresco-health-processor-solr-1

  echo "Restarting Alfresco container..."
  docker restart alfresco-health-processor-alfresco-1

  wait_for_alfresco

  echo "Starting Docker Compose with: $compose_files"
  docker compose -f $compose_files up -d

  echo "Running Python script..."
  python3 solr.py

  echo "Python script finished."
}

# Run test 1 (only docker-compose.yml)
#run_test "docker-compose.yml"

# Run tests 2-7 (docker-compose.yml + scenario files)
#for i in {1..6}; do
#  run_test "docker-compose.yml -f docker-compose_scenario-$i.yml"
#done

#run_test "docker-compose.yml -f docker-compose_scenario-7.yml"
#run_test "docker-compose.yml -f docker-compose_scenario-8.yml"
#run_test "docker-compose.yml -f docker-compose_scenario-9.yml"
#run_test "docker-compose.yml -f docker-compose_scenario-10.yml"
run_test "docker-compose.yml -f docker-compose_scenario-11.yml"

echo "All tests completed."

