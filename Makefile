up:
	@docker compose -f src/test/resources/docker-compose.yml up

down:
	@docker compose -f src/test/resources/docker-compose.yml down

down_delete_volumes:
	@docker compose -f src/test/resources/docker-compose.yml down --volumes --remove-orphans
