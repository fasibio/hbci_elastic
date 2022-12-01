# First start elastic kibana etc

`docker-compose up -d`

# Import kibana index, dashboard, visualizations

`curl -X POST http://elastic:MagicWord@localhost:5601/api/saved_objects/_import -H "kbn-xsrf: true" --form file=@./kibana_imports.ndjson`

