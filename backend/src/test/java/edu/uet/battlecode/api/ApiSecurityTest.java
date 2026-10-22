package edu.uet.battlecode.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.uet.battlecode.BattlecodeApplication;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = BattlecodeApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:api;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "battlecode.jwt-issuer=https://auth.example",
        "battlecode.jwks-uri=https://auth.example/.well-known/jwks.json",
        "battlecode.worker-key=test-only-secret-32-characters-0000"
        ,"battlecode.github-dispatch-repo=owner/platform",
        "battlecode.github-dispatch-token=test-token",
        "battlecode.r2-endpoint=https://r2.example",
        "battlecode.r2-access-key=test",
        "battlecode.r2-secret-key=test",
        "battlecode.r2-bucket=test",
        "battlecode.r2-public-base=https://cdn.example"
})
class ApiSecurityTest {
    @Autowired MockMvc mvc;

    @Test void healthIsPublicButTeamMutationRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(post("/api/tournaments/" + UUID.randomUUID() + "/teams")
                .contentType("application/json").content("{\"name\":\"Alpha\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void workerClaimRejectsMissingSharedSecret() throws Exception {
        mvc.perform(post("/api/worker/claim").contentType("application/json")
                .content("{\"workerId\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
