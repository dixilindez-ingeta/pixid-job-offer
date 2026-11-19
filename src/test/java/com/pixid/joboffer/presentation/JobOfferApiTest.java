package com.pixid.joboffer.presentation;

import com.jayway.jsonpath.JsonPath;
import com.pixid.joboffer.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JobOfferApiTest {

    private static final String JOB_OFFERS = "/api/v1/job-offers";
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
    private static final String LOCATION = """
            {"type":"CUSTOM","address":{"lines":["10 avenue de France"],"postalCode":"75013",
             "city":"Paris","countryCode":"FR"}}""";
    private static final String COMPENSATION = """
            {"salaryEntries":[{"type":"HOURLY","amount":13.50,"currency":"EUR"}],
             "bonusEntries":[{"type":"SIGNING","amount":300,"currency":"EUR"}]}""";

    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;
    private UUID companyId;

    private static String completeOffer(UUID companyId) {
        return """
                {"companyId":"%s","title":"Cariste H/F","location":%s,"compensation":%s}"""
                .formatted(companyId, LOCATION, COMPENSATION);
    }

    private static String action(String action) {
        return "{\"action\":\"" + action + "\"}";
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        companyId = UUID.randomUUID();
    }

    private ResultActions configure(boolean approval, boolean partialSave, boolean manualPosting) throws Exception {
        return mvc.perform(put("/api/v1/companies/{companyId}/config", companyId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"approvalRequired":%s,"partialSaveEnabled":%s,"manualPostingRequired":%s}"""
                        .formatted(approval, partialSave, manualPosting)));
    }

    private ResultActions create(String body) throws Exception {
        return mvc.perform(post(JOB_OFFERS).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String createAndGetId(String body) throws Exception {
        String response = create(body).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions transition(String id, String action) throws Exception {
        return mvc.perform(post(JOB_OFFERS + "/{id}/transitions", id)
                .contentType(MediaType.APPLICATION_JSON).content(action(action)));
    }

    private ResultActions update(String id, String body) throws Exception {
        return mvc.perform(patch(JOB_OFFERS + "/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Nested
    @DisplayName("Parcours complets")
    class Workflows {

        @Test
        @DisplayName("config par défaut (tout désactivé) : SUBMIT publie directement, puis CLOSE")
        void defaultConfigPublishesDirectly() throws Exception {
            String body = completeOffer(companyId);
            String response = create(body)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", matchesPattern(".*/api/v1/job-offers/[0-9a-f-]{36}")))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                    .andExpect(jsonPath("$.location.address.city").value("Paris"))
                    .andExpect(jsonPath("$.compensation.salaryEntries[0].amount").value(13.5))
                    .andExpect(jsonPath("$.publishedAt").value(nullValue()))
                    .andExpect(jsonPath("$.availableTransitions", contains("SUBMIT")))
                    .andReturn().getResponse().getContentAsString();
            String id = JsonPath.read(response, "$.id");

            transition(id, "SUBMIT")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PUBLISHED"))
                    .andExpect(jsonPath("$.publishedAt").value(notNullValue()))
                    .andExpect(jsonPath("$.availableTransitions", containsInAnyOrder("CLOSE", "EXPIRE")));

            transition(id, "CLOSE")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"))
                    .andExpect(jsonPath("$.availableTransitions", empty()));

            mvc.perform(get(JOB_OFFERS + "/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"));
        }

        @Test
        @DisplayName("tout activé : brouillon partiel, toutes les étapes, REJECT renvoie en DRAFT, puis EXPIRE")
        void allEnabledGoesThroughEveryStep() throws Exception {
            configure(true, true, true).andExpect(status().isOk());

            String id = createAndGetId("""
                    {"companyId":"%s","title":"Cariste H/F"}""".formatted(companyId));
            mvc.perform(get(JOB_OFFERS + "/{id}", id))
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.location").value(nullValue()))
                    .andExpect(jsonPath("$.availableTransitions", contains("SUBMIT")));

            transition(id, "SUBMIT")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("TO_FINALIZE"))
                    .andExpect(jsonPath("$.availableTransitions", empty()));

            transition(id, "FINALIZE")
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.missingFields", containsInAnyOrder("location", "compensation")));

            update(id, """
                    {"location":%s,"compensation":%s}""".formatted(LOCATION, COMPENSATION))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Cariste H/F"))
                    .andExpect(jsonPath("$.availableTransitions", contains("FINALIZE")));

            transition(id, "FINALIZE")
                    .andExpect(jsonPath("$.status").value("TO_APPROVE"))
                    .andExpect(jsonPath("$.availableTransitions", containsInAnyOrder("APPROVE", "REJECT")));

            transition(id, "REJECT")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DRAFT"));

            transition(id, "SUBMIT").andExpect(jsonPath("$.status").value("TO_FINALIZE"));
            transition(id, "FINALIZE").andExpect(jsonPath("$.status").value("TO_APPROVE"));
            transition(id, "APPROVE")
                    .andExpect(jsonPath("$.status").value("TO_POST"))
                    .andExpect(jsonPath("$.publishedAt").value(nullValue()))
                    .andExpect(jsonPath("$.availableTransitions", contains("POST")));
            transition(id, "POST")
                    .andExpect(jsonPath("$.status").value("PUBLISHED"))
                    .andExpect(jsonPath("$.publishedAt").value(notNullValue()));
            transition(id, "EXPIRE")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CLOSED"));
        }
    }

    @Nested
    @DisplayName("Recherche")
    class Search {

        @Test
        @DisplayName("filtre par entreprise et statut, pagination et tri, sans availableTransitions")
        void filtersPaginatesAndSorts() throws Exception {
            String first = createAndGetId(completeOffer(companyId));
            String second = createAndGetId(completeOffer(companyId));
            String third = createAndGetId(completeOffer(companyId));
            transition(third, "SUBMIT").andExpect(status().isOk());
            createAndGetId(completeOffer(UUID.randomUUID()));

            mvc.perform(get(JOB_OFFERS)
                            .param("companyId", companyId.toString())
                            .param("status", "DRAFT")
                            .param("size", "1")
                            .param("sort", "createdAt,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id").value(first))
                    .andExpect(jsonPath("$.content[0].availableTransitions").doesNotExist())
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(1))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(2));

            mvc.perform(get(JOB_OFFERS)
                            .param("companyId", companyId.toString())
                            .param("status", "DRAFT")
                            .param("page", "1")
                            .param("size", "1")
                            .param("sort", "createdAt,asc"))
                    .andExpect(jsonPath("$.content[0].id").value(second));

            mvc.perform(get(JOB_OFFERS).param("companyId", companyId.toString()))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.content[*].id", contains(third, second, first)));
        }

        @Test
        @DisplayName("400 sur sort, size, page ou status invalides")
        void rejectsInvalidParameters() throws Exception {
            mvc.perform(get(JOB_OFFERS).param("sort", "title,asc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.errors[0].field").value("sort"));
            mvc.perform(get(JOB_OFFERS).param("size", "101"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("size"));
            mvc.perform(get(JOB_OFFERS).param("page", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("page"));
            mvc.perform(get(JOB_OFFERS).param("status", "UNKNOWN"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Erreurs 400")
    class BadRequests {

        @Test
        @DisplayName("validation des champs avec détail par champ")
        void fieldValidation() throws Exception {
            create("""
                    {"title":"%s","compensation":{"salaryEntries":[{"type":"HOURLY","amount":-1,"currency":"EUR"}]}}"""
                    .formatted("x".repeat(201)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Invalid request"))
                    .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                            "companyId", "title", "compensation.salaryEntries[0].amount")));
        }

        @Test
        @DisplayName("création incomplète refusée sans partialSaveEnabled")
        void incompleteCreationWithoutPartialSave() throws Exception {
            create("""
                    {"companyId":"%s","title":"Cariste H/F"}""".formatted(companyId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail", containsString("location")));
        }

        @Test
        @DisplayName("JSON mal formé, action inconnue, UUID invalide")
        void unreadableRequests() throws Exception {
            create("{\"companyId\":")
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));

            String id = createAndGetId(completeOffer(companyId));
            transition(id, "PUBLISH")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("action"))
                    .andExpect(jsonPath("$.errors[0].message", containsString("SUBMIT")));

            mvc.perform(get(JOB_OFFERS + "/not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
        }

        @Test
        @DisplayName("configuration entreprise : champs obligatoires")
        void companyConfigValidation() throws Exception {
            mvc.perform(put("/api/v1/companies/{companyId}/config", companyId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"approvalRequired\":true}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field",
                            containsInAnyOrder("partialSaveEnabled", "manualPostingRequired")));
        }
    }

    @Nested
    @DisplayName("Erreurs 404 et 409")
    class NotFoundAndConflicts {

        @Test
        @DisplayName("404 sur une offre inexistante (GET, PATCH, transition)")
        void notFound() throws Exception {
            String unknown = UUID.randomUUID().toString();
            mvc.perform(get(JOB_OFFERS + "/{id}", unknown))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.instance").value(JOB_OFFERS + "/" + unknown));
            update(unknown, "{\"title\":\"Nouveau titre\"}").andExpect(status().isNotFound());
            transition(unknown, "SUBMIT").andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("409 sur une action interdite dans l'état courant")
        void forbiddenTransition() throws Exception {
            String id = createAndGetId(completeOffer(companyId));
            transition(id, "CLOSE")
                    .andExpect(status().isConflict())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Transition not allowed"));
        }

        @Test
        @DisplayName("409 sur une modification d'offre publiée")
        void modificationOfPublishedOffer() throws Exception {
            String id = createAndGetId(completeOffer(companyId));
            transition(id, "SUBMIT").andExpect(jsonPath("$.status").value("PUBLISHED"));

            update(id, "{\"title\":\"Nouveau titre\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Modification not allowed"));
        }
    }

    @Nested
    @DisplayName("Configuration entreprise")
    class CompanyConfiguration {

        @Test
        @DisplayName("valeurs par défaut puis remplacement")
        void defaultsThenReplace() throws Exception {
            mvc.perform(get("/api/v1/companies/{companyId}/config", companyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.companyId").value(companyId.toString()))
                    .andExpect(jsonPath("$.approvalRequired").value(false))
                    .andExpect(jsonPath("$.partialSaveEnabled").value(false))
                    .andExpect(jsonPath("$.manualPostingRequired").value(false));

            configure(true, false, true).andExpect(status().isOk());

            mvc.perform(get("/api/v1/companies/{companyId}/config", companyId))
                    .andExpect(jsonPath("$.approvalRequired").value(true))
                    .andExpect(jsonPath("$.partialSaveEnabled").value(false))
                    .andExpect(jsonPath("$.manualPostingRequired").value(true));
        }
    }
}
