# microservicios-futfem-squads-temp

`microservicios-futfem-squads-temp` is the temporary squad service used by the Tikitakas backend for transitional or staged squad records. It exists alongside the main squad service so that temporary workflows can evolve independently without polluting the canonical squad dataset.

The project uses Java 21, Spring Boot, Spring Data JPA, MySQL, Springdoc OpenAPI, and Maven Wrapper. It inherits generic CRUD behavior from `microservicios-common`, registers in Eureka for discovery, and is intended to be consumed externally through the central gateway. This gives the repository the same operational model as the rest of the futfem services.

Typical local execution:

```bash
./mvnw spring-boot:run
```

Gateway route:

- `/api/futfem/squadstemp/**`

In `v0.1.0`, the service includes CI support, Docker-compatible packaging, and OpenAPI configuration tailored for gateway access. That means its Swagger documentation is now usable from the shared gateway UI, with "Try it out" requests correctly pointed to the public route.

This repository is the right place for temporary squad information, import pipelines, or intermediate synchronization workflows that should remain isolated from the stable squad domain until they are validated.
