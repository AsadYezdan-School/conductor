package com.github.asadyezdanschool.conductor.submitter.integration;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class SubmitterIntegrationTest extends IntegrationTestBase {

    // ── POST /jobs ────────────────────────────────────────────────────────────

    @Test
    void createJob_minimalValidRequest_returns201() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "smoke-job",
                  "cron": "* * * * *",
                  "url":  "https://example.com/ping",
                  "method": "GET"
                }
                """)
        .when()
            .post("/jobs")
        .then()
            .statusCode(201)
            .body("version", equalTo(1))
            .body("jobFamilyId", notNullValue())
            .body("jobDefinitionId", notNullValue());
    }

    @Test
    void createJob_withPayloadAndHeaders_returns201() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "post-job",
                  "cron": "0 * * * *",
                  "url":  "https://api.example.com/webhook",
                  "method": "POST",
                  "payload": {"key": "value"},
                  "headers": {"Authorization": "Bearer token"},
                  "timeoutSeconds": 60
                }
                """)
        .when()
            .post("/jobs")
        .then()
            .statusCode(201)
            .body("version", equalTo(1));
    }

    @Test
    void createJob_missingName_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "cron": "* * * * *",
                  "url":  "https://example.com",
                  "method": "GET"
                }
                """)
        .when()
            .post("/jobs")
        .then()
            .statusCode(422)
            .body("errors", hasItem(containsString("name")));
    }

    @Test
    void createJob_invalidUrl_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "job",
                  "cron": "* * * * *",
                  "url":  "ftp://not-http.com",
                  "method": "GET"
                }
                """)
        .when()
            .post("/jobs")
        .then()
            .statusCode(422)
            .body("errors", hasItem(containsString("url")));
    }

    @Test
    void createJob_invalidMethod_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "job",
                  "cron": "* * * * *",
                  "url":  "https://example.com",
                  "method": "LAUNCH"
                }
                """)
        .when()
            .post("/jobs")
        .then()
            .statusCode(422)
            .body("errors", hasItem(containsString("method")));
    }

    // ── PUT /jobs/{jobFamilyId} ───────────────────────────────────────────────

    @Test
    void editJob_validRequest_incrementsVersion() {
        // Create first
        String familyId = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "name": "editable-job",
                  "cron": "* * * * *",
                  "url":  "https://example.com",
                  "method": "GET"
                }
                """)
        .when()
            .post("/jobs")
        .then()
            .statusCode(201)
            .extract().path("jobFamilyId");

        // Edit it
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "cron": "0 * * * *"
                }
                """)
        .when()
            .put("/jobs/" + familyId)
        .then()
            .statusCode(200)
            .body("version", equalTo(2))
            .body("jobFamilyId", equalTo(familyId));
    }

    @Test
    void editJob_unknownFamily_returns404() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"cron\": \"0 * * * *\"}")
        .when()
            .put("/jobs/00000000-0000-0000-0000-000000000000")
        .then()
            .statusCode(404);
    }

    @Test
    void editJob_invalidUuid_returns422() {
        given()
            .contentType(ContentType.JSON)
            .body("{\"cron\": \"0 * * * *\"}")
        .when()
            .put("/jobs/not-a-uuid")
        .then()
            .statusCode(422);
    }

    // ── POST /jobs/{jobFamilyId}/park ─────────────────────────────────────────

    @Test
    void parkJob_activeJob_returns200() {
        String familyId = createJob("park-me");

        given()
        .when()
            .post("/jobs/" + familyId + "/park")
        .then()
            .statusCode(200)
            .body("isParked", equalTo(true))
            .body("jobFamilyId", equalTo(familyId));
    }

    @Test
    void parkJob_alreadyParked_returns409() {
        String familyId = createJob("double-park");

        // First park
        given().when().post("/jobs/" + familyId + "/park").then().statusCode(200);

        // Park again → conflict
        given()
        .when()
            .post("/jobs/" + familyId + "/park")
        .then()
            .statusCode(409);
    }

    @Test
    void parkJob_unknownFamily_returns404() {
        given()
        .when()
            .post("/jobs/00000000-0000-0000-0000-000000000001/park")
        .then()
            .statusCode(404);
    }

    // ── POST /jobs/{jobFamilyId}/unpark ───────────────────────────────────────

    @Test
    void unparkJob_parkedJob_returns200() {
        String familyId = createJob("unpark-me");

        // Park first
        given().when().post("/jobs/" + familyId + "/park").then().statusCode(200);

        // Unpark
        given()
        .when()
            .post("/jobs/" + familyId + "/unpark")
        .then()
            .statusCode(200)
            .body("isParked", equalTo(false))
            .body("jobFamilyId", equalTo(familyId));
    }

    @Test
    void unparkJob_alreadyUnparked_returns409() {
        String familyId = createJob("already-unparked");

        // Not parked → conflict
        given()
        .when()
            .post("/jobs/" + familyId + "/unpark")
        .then()
            .statusCode(409);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String createJob(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                  "name": "%s",
                  "cron": "* * * * *",
                  "url":  "https://example.com",
                  "method": "GET"
                }
                """, name))
        .when()
            .post("/jobs")
        .then()
            .statusCode(201)
            .extract().path("jobFamilyId");
    }

}