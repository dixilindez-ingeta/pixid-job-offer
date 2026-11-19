# Job Offer Service (PIXID)

Microservice de gestion des **offres d'emploi** d'une plateforme d'intérim. Il est consommé par le back-office
(recruteurs), le job board public (lecture seule) et d'autres services internes.

Stack : Java 25, Spring Boot 4.1, PostgreSQL 17, Spring Data JPA, Flyway, springdoc-openapi 3, Testcontainers.

## Prérequis

- **Java 25** (JDK)
- **Docker** (Docker Desktop, OrbStack…), utilisé pour la base PostgreSQL et pour les tests d'intégration
- Maven n'est pas nécessaire : le wrapper `./mvnw` est fourni

## Lancer l'application

```bash
./mvnw spring-boot:run
```

- Grâce à `spring-boot-docker-compose`, le conteneur PostgreSQL défini dans `compose.yml` est **démarré
  automatiquement**. Il est exposé sur `localhost:5434`, base `job_offer`, utilisateur `super_jobber`.
- Les migrations Flyway (`src/main/resources/db/migration`) sont appliquées au démarrage, puis Hibernate valide le
  schéma (`ddl-auto: validate`).
- L'API écoute sur **http://localhost:8486**.

> Le lancement complet en conteneurs (`docker compose up --build`, application + base) sera ajouté à l'étape F
> (Dockerfile).

### Configuration

Toutes les propriétés de `src/main/resources/application.yaml` peuvent être surchargées par variables d'environnement,
par exemple :

```bash
SPRING_DOCKER_COMPOSE_ENABLED=false \
SPRING_DATASOURCE_URL=jdbc:postgresql://mon-hote:5432/job_offer \
SPRING_DATASOURCE_USERNAME=... SPRING_DATASOURCE_PASSWORD=... \
./mvnw spring-boot:run
```

### Réinitialiser la base locale

Il n'y a pas encore de mise en production : la migration `V1__init_schema.sql` peut donc encore évoluer. Si elle a
changé depuis le dernier lancement, Flyway refuse de démarrer (`Migration checksum mismatch`). Dans ce cas, supprime le
volume local (**toutes les données locales sont perdues**) :

```bash
docker compose down -v
```

## Tests

```bash
./mvnw verify
```

Docker doit être démarré : les tests d'intégration lancent un PostgreSQL éphémère via Testcontainers. La suite
comprend :

- les règles d'architecture (ArchUnit) ;
- les tests unitaires du domaine et des use cases ;
- les tests d'intégration de la persistance.

## Documentation de l'API

- Swagger UI : http://localhost:8486/swagger-ui.html
- Spécification OpenAPI (JSON) : http://localhost:8486/v3/api-docs

### Endpoints

| Méthode | Chemin                                 | Description                                                                                                                                                                  | Succès |
|---------|----------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------|
| `POST`  | `/api/v1/job-offers`                   | Créer un brouillon (header `Location`)                                                                                                                                       | 201    |
| `PATCH` | `/api/v1/job-offers/{id}`              | Mise à jour partielle (DRAFT / TO_FINALIZE uniquement)                                                                                                                       | 200    |
| `POST`  | `/api/v1/job-offers/{id}/transitions`  | Appliquer une action du cycle de vie                                                                                                                                         | 200    |
| `GET`   | `/api/v1/job-offers/{id}`              | Lire une offre et ses `availableTransitions`                                                                                                                                 | 200    |
| `GET`   | `/api/v1/job-offers`                   | Rechercher : `companyId`, `status`, `page` (0…), `size` (1–100, défaut 20), `sort` (`createdAt` \| `updatedAt` \| `publishedAt`, `,asc` \| `,desc`, défaut `createdAt,desc`) | 200    |
| `GET`   | `/api/v1/companies/{companyId}/config` | Lire la configuration (tout à `false` si jamais définie)                                                                                                                     | 200    |
| `PUT`   | `/api/v1/companies/{companyId}/config` | Remplacer la configuration                                                                                                                                                   | 200    |

### Cycle de vie

```
DRAFT --SUBMIT--> TO_FINALIZE --FINALIZE--> TO_APPROVE --APPROVE--> TO_POST --POST--> PUBLISHED --CLOSE/EXPIRE--> CLOSED
                  (partialSaveEnabled)      (approvalRequired)  │   (manualPostingRequired)
                                                                └--REJECT--> DRAFT
```

Les états optionnels sont **sautés** quand le paramètre correspondant de l'entreprise est désactivé. Avec la
configuration par défaut (tout à `false`), `SUBMIT` publie donc directement l'offre.

| Paramètre entreprise    | Effet si activé                                                            |
|-------------------------|----------------------------------------------------------------------------|
| `partialSaveEnabled`    | création possible avec des données incomplètes ; passage par `TO_FINALIZE` |
| `approvalRequired`      | passage par `TO_APPROVE` (`APPROVE` ou `REJECT`)                           |
| `manualPostingRequired` | attente en `TO_POST` au lieu d'une publication automatique                 |

Une offre doit être **complète** (titre, localisation, au moins une entrée de rémunération) dès qu'elle dépasse
`TO_FINALIZE`.

## Exemples (curl)

```bash
API=http://localhost:8486/api/v1
COMPANY=3f6c2a9e-1b7d-4c2e-9a1f-5d8e7b6a4c3d

# 1. Configurer l'entreprise (toutes les étapes activées)
curl -X PUT $API/companies/$COMPANY/config -H 'Content-Type: application/json' \
  -d '{"approvalRequired":true,"partialSaveEnabled":true,"manualPostingRequired":true}'

# 2. Créer un brouillon partiel (autorisé car partialSaveEnabled = true)
curl -i -X POST $API/job-offers -H 'Content-Type: application/json' \
  -d '{"companyId":"'$COMPANY'","title":"Cariste H/F"}'
#   -> 201, header Location: .../api/v1/job-offers/{id}
ID=<id renvoyé>

# 3. Compléter l'offre
curl -X PATCH $API/job-offers/$ID -H 'Content-Type: application/json' -d '{
  "location": {"type":"CUSTOM","address":{"lines":["10 avenue de France"],"postalCode":"75013","city":"Paris","countryCode":"FR"}},
  "compensation": {"salaryEntries":[{"type":"HOURLY","amount":13.50,"currency":"EUR"}],
                   "bonusEntries":[{"type":"SIGNING","amount":300,"currency":"EUR"}]}
}'

# 4. Faire avancer l'offre : SUBMIT, FINALIZE, APPROVE, POST
curl -X POST $API/job-offers/$ID/transitions -H 'Content-Type: application/json' -d '{"action":"SUBMIT"}'

# 5. Consulter l'offre et les actions disponibles
curl $API/job-offers/$ID
#   -> { ..., "status":"TO_FINALIZE", "availableTransitions":["FINALIZE"] }

# 6. Rechercher
curl "$API/job-offers?companyId=$COMPANY&status=PUBLISHED&page=0&size=20&sort=publishedAt,desc"
```

## Erreurs

Les erreurs suivent la **RFC 9457** (`application/problem+json`) :

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/job-offers",
  "errors": [{ "field": "compensation.salaryEntries[0].amount", "message": "must be greater than or equal to 0" }]
}
```

| Statut | Cas                                                                                                                                                                                         |
|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 400    | Corps ou paramètre invalide (détail par champ dans `errors`), JSON mal formé, valeur d'enum inconnue, offre incomplète à la création sans `partialSaveEnabled`                              |
| 404    | Offre introuvable                                                                                                                                                                           |
| 409    | Action interdite dans l'état courant, modification hors DRAFT / TO_FINALIZE, offre incomplète pour `SUBMIT`/`FINALIZE` (champ `missingFields`), modification concurrente (verrou optimiste) |

## Architecture en bref

Architecture en **oignon** (`domain` ← `application` ← `infrastructure` / `presentation`). Les règles de dépendance sont
vérifiées par ArchUnit.

- `domain` : agrégat `JobOffer`, `JobOfferStateMachine`, value objects (records), exceptions métier, ports
  (`JobOfferRepository`, `CompanyConfigProvider`). Aucune dépendance à Spring ni à JPA.
- `application` : use cases transactionnels (`CreateJobOffer`, `TransitionJobOffer`, `SearchJobOffers`…).
- `infrastructure` : adaptateurs PostgreSQL (Spring Data JPA, entités JPA séparées du domaine, mapper), migrations
  Flyway.
- `presentation` : contrôleurs REST, DTOs (records + Bean Validation), gestion des erreurs RFC 9457, OpenAPI.

Décisions détaillées : [docs/architecture-decision-records.md](docs/architecture-decision-records.md) — feuille de
route : [docs/roadmap.md](docs/roadmap.md).

### Choix assumés

- **`REJECT` → `DRAFT`** : l'offre redevient modifiable puis peut être resoumise (ADR-06).
- **`CLOSE` / `EXPIRE` uniquement depuis `PUBLISHED`** (ADR-06).
- **Rémunération composée de primes uniquement acceptée** : la spécification exige au moins une entrée, salaire ou
  prime.
- **Pas de clé étrangère vers l'entreprise** : `companyId` référence un service externe. Une entreprise sans
  configuration utilise les valeurs par défaut (ADR-05).
- **Offre incomplète à la transition → 409** : la requête est valide, c'est l'état de la ressource qui bloque (ADR-07).
- Les listes n'incluent pas `availableTransitions` (calculé sur la lecture unitaire et les réponses de commandes).

### Avec plus de temps

- Outbox + publication d'événements (Kafka) à chaque transition.
- `EXPIRE` déclenché automatiquement (job planifié sur une date d'expiration).
- Concurrence optimiste exposée au client (`ETag` / `If-Match`).
- Cache de la configuration entreprise.
- Recherche plein texte (titre, ville) et filtres supplémentaires.
- Authentification / autorisation par entreprise.
