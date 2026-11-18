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

`présentation (API REST: Controllers, DTOs REST...)`

↓

`infrastructure (Adapters : PostgreSQL, Spring Data, Flyway, JPA Entities,...)`

↓

`application (Use case : UseCase Handlers, Command/Queries, Ports,...)`

↓

`domain (Core business : Aggregate, State Machine, Value Objects,...)`


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
    - Le schéma de BDD peut être optimisé indépendemment de la structure objet métier. Le modèle de domaine garantit ses invariants à la construction et à chaque invariant.
  - #### Négatives :
    - Classes de mapping nécessaires
