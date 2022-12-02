# First start elastic kibana etc

`docker-compose up -d`

# Import kibana index, dashboard, visualizations

`curl -X POST http://elastic:MagicWord@localhost:5601/api/saved_objects/_import -H "kbn-xsrf: true" --form file=@./kibana_imports.ndjson`

# Command to collect data from konto-number: 

`mkdir db`

`docker run -it --volume db:/home/db --network=elk fasibio/hbci-elastic --blz=[blz] --userid=[userid] -p load --funk_url ws://funk-server:3000/data/subscribe -k [konto-number] `
