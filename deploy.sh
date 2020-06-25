sudo docker-compose -p ofbiz -f docker-compose-databases.yml up -d
sudo docker-compose -p ofbiz -f docker-compose-databases.yml scale worker=5
sudo docker-compose -f docker-compose-databases.yml restart master
sudo docker-compose -p ofbiz -f docker-compose-ofbiz.yml up -d
#curl -s -H "Content-Type: application/json" -X PUT "http://localhost:5984/_users" -u ofbiz:ofbiz
