**ROADMAP de développement du Microservice Job Offer (PIXID)**

**Etape 1 : initialisation et infrastructure locale**
- **1.1 :** Initialisation du projet Java 25, Spring Boot 4 (Spring Web, Spring Data JPA, PostgresSQL, SpringDoc OpenAPI, Flyway,...)
- **1.2 :** Créer le fichier `compose.yml` avec un conteneur PostegresSQL et un volume persistant
- **1.3 :** Configurer `application.yml`
- **1.4 :** Créer les packages de base (`domain`, `application`, `intrastructure`, `prensentation`)
- **1.5 :** Figer les règles d'architecture avec `ArchUnit`
- **1.5 :** Vérifier le démarrage à vide de l'application