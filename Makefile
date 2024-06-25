up:
	@docker compose -f assets/docker-compose.yml up

down:
	@docker compose -f assets/docker-compose.yml down

down_delete_volumes:
	@docker compose -f assets/docker-compose.yml down --volumes --remove-orphans
