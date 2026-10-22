package edu.uet.battlecode.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.uet.battlecode.BattlecodeApplication;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = BattlecodeApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:authz;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class ApiAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    private UUID student, admin, tournament;

    @BeforeEach void setUp() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS profiles(id UUID PRIMARY KEY, role VARCHAR(16))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS tournaments(id UUID PRIMARY KEY,status VARCHAR(24))");
        jdbc.execute("CREATE TABLE IF NOT EXISTS teams(id UUID PRIMARY KEY,tournament_id UUID,name VARCHAR(100),seed INT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS tournament_entries(tournament_id UUID,team_id UUID,submission_id UUID,seed INT)");
        jdbc.update("DELETE FROM tournament_entries"); jdbc.update("DELETE FROM teams");
        jdbc.update("DELETE FROM tournaments"); jdbc.update("DELETE FROM profiles");
        student = UUID.randomUUID(); admin = UUID.randomUUID(); tournament = UUID.randomUUID();
        jdbc.update("INSERT INTO profiles VALUES (?,'STUDENT')", student);
        jdbc.update("INSERT INTO profiles VALUES (?,'ADMIN')", admin);
        jdbc.update("INSERT INTO tournaments VALUES (?,'REGISTRATION_OPEN')", tournament);
    }

    @Test void onlyDatabaseAdminCanFreeze() throws Exception {
        String url = "/api/admin/tournaments/" + tournament + "/freeze";
        mvc.perform(post(url).with(jwt().jwt(j -> j.subject(student.toString()).audience(List.of("authenticated")))))
                .andExpect(status().isForbidden());
        mvc.perform(post(url).with(jwt().jwt(j -> j.subject(admin.toString()).audience(List.of("authenticated")))))
                .andExpect(status().isOk());
    }
}
