Ce fichier documente les décisions architecturales structurales prises lors du développement du service PIXID Job Offer.

# ADR-01 : Adoption de l'architecture en Oignon (Onion Architecture)

- ### Statut : Accepté
- ### Contexte :
  Le microservice (MS) gère un cycle de vie métier complexe (machine à états configurable par entreprise, règles de validation conditionnelles) tout en devant s'intégrer dans un écosystème plus vaste (back-office, job board public, futurs évènements asynchrones).
- ### Décision :
  L'architecture en oignon est pleinement adapté aux projets en DDD. On structure le code avec quatre couches concentriques, de l'intérieur vers l'extérieur (les couches internes ne dépendent pas des couches externes) :

  - `domain`
  - `application`
  - `infrastructure`
  - `présentation`

<div align="center">

`présentation (API REST: Controllers, DTOs REST...)`

↓

`infrastructure (Adapters : PostgreSQL, Spring Data, Flyway, JPA Entities,...)`

↓

`application (Use case : UseCase Handlers, Command/Queries, Ports,...)`

↓

`domain (Core business : Aggregate, State Machine, Value Objects,...)`
</div>

- ### Conséquences :
  - #### Positives :
    - Le métier (application+domain) ne dépend pas de la technologie (spring)
    - Testabilité unitaire instantanée et exhaustive du moteur d'états sans démarrage du moteur Spring
    - Evolution facilitée (ex : Ajout d'un adaptateur Kafka pour publier des évènements sans toucher au domaine,...)
  - #### Négatives :
    - Verbosité élevée avec par exemple des Mappers entre JPA/DTOs REST.

# ADR-02 : Séparation stricte entre les entités du domaine et les entités JPA

- ### Statut :** Accepté
- ### Contexte :
Spring Data JPA impose des contraintes techniques aux entités(mutabilité, annotations de mapping relationnel, constructeur sans arguments,...) qui rentrent en conflit avec les principes d'immuabilité des aggrégats DDD.
- ### Décision :
Créer des classes distinctes pour la persistance(dans infrastructure/persistence) et pour le métier (dans domain/model)

- ### Conséquences :
  - #### Positives :
  Le schéma de BDD peut être optimisé indépendemment de la structure objet métier. Le modèle de domaine garantit ses invariants à la construction et à chaque invariant.
  - #### Négatives :
  Classes de mapping nécessaires



# ADR-03 : Résolution Dynamique des Sauts d'États dans un Service de Domaine

- ### Statut : Accepté

- ### Contexte : 
Une offre d'emploi traverse jusqu'à 6 états, dont 3 optionnels(`TO_FINALIZE`, `TO_APPROVE`, `TO_POST`) selon la configuration de l'entreprise. De plus, l'API doit exposer dynamiquement les transitions disponibles (`availableTransitions`).

- ### Décision : 
Centraliser l'algorithme de calcul des transitions et de résolution de l'état cible dans une classe dédiée du domaine (`JobOfferStateMachine`) prenant en entrée l'état courant de l'offre et la `CompanyConfig`.

- ### Conséquences :
  - #### Positives : 
  Élimination complète de la duplication de conditions(`if/else`) entre les contrôleurs REST, les cas d'utilisation et les validations; ajout aisé d'un nouvel état optionnel dans le futur.
  - #### Négatives : 
  Nécessite de toujours charger la CompanyConfig (ou ses valeurs par défaut) lors d'une lecture unitaire ou d'une transition d'offre.




# ADR-04 : Modélisation des Transitions via une Ressource dédiée (POST /transitions)

- ### Statut : Accepté

- ### Contexte : 
Les changements d'état d'une offre(`submit`, `approve`, `reject`, `post`, `close`, `expire`) déclenchent des règles de validation spécifiques et des effets de bord (ex: valorisation de `publishedAt`), contrairement à une simple mise à jour de champs (`PUT`).

- ### Décision : 
Exposer un endpoint dédié orienté commande `POST /api/v1/job-offers/{id}/transitions` recevant l'action demandée (ex: `{"action": "APPROVE"}`), plutôt que d'autoriser la modification directe du champ status via `PUT` ou `PATCH`.

- ### Conséquences :
  - #### Positives : 
  Sémantique REST claire et explicite ; séparation nette entre la modification du contenu de l'offre (autorisée uniquement en `DRAFT/TO_FINALIZE`) et l'avancement dans le workflow; gestion fine des erreurs de transition(`400 Bad Request` / `409 Conflict`).
  - #### Négatives : 
  Un endpoint supplémentaire à documenter dans OpenAPI.