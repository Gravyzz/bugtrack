package ru.hits.bugtrack;

import com.fasterxml.jackson.databind.*;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import ru.hits.bugtrack.config.DemoData;
import ru.hits.bugtrack.dto.ApiDtos.CreateBugRequest;
import ru.hits.bugtrack.model.*;
import ru.hits.bugtrack.repository.*;
import ru.hits.bugtrack.service.BugService;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"bugtrack.demo.enabled=true", "bugtrack.demo.password=BugtrackDemo2026!"})
@AutoConfigureMockMvc
class BugtrackIntegrationTest {
    static final EmbeddedPostgres POSTGRES;
    static { try { POSTGRES = EmbeddedPostgres.builder().setPort(0).setServerConfig("unix_socket_directories", "").start(); } catch (Exception e) { throw new ExceptionInInitializerError(e); } }

    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BugRepository bugs;
    @Autowired HistoryEntryRepository history;
    @Autowired UserRepository users;
    @Autowired UserSessionRepository sessions;
    @Autowired ProjectRepository projects;
    @Autowired BugService bugService;

    record Login(Cookie cookie, String csrf) {}

    Login login(String email) throws Exception {
        var response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("email",email,"password","BugtrackDemo2026!"))))
            .andExpect(status().isOk()).andReturn().getResponse();
        String raw = response.getHeader("Set-Cookie").split(";",2)[0].split("=",2)[1];
        return new Login(new Cookie("BUGTRACK_SESSION", raw), json.readTree(response.getContentAsString()).get("csrfToken").asText());
    }

    String draft(String title) throws Exception {
        return json.writeValueAsString(Map.of("title",title,"description","Описание ошибки", "steps","1. Открыть страницу", "expected","Страница работает", "actual","Ошибка 500", "severity","CRITICAL", "priority","HIGH"));
    }

    JsonNode create(Login login, String title) throws Exception {
        return json.readTree(mvc.perform(post("/api/projects/"+DemoData.SHOP_ID+"/bugs")
            .cookie(login.cookie()).header("X-CSRF-Token",login.csrf()).contentType(MediaType.APPLICATION_JSON).content(draft(title)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    @Test void createsBugWithServerAuthorAndHistory() throws Exception {
        Login login=login("zakhar@example.org");
        var bug=create(login,"Не создаётся заказ");
        assertThat(bug.get("status").asText()).isEqualTo("NEW");
        assertThat(bug.get("code").asText()).startsWith("SHOP-");
        assertThat(bug.get("author").asText()).isEqualTo("Захар");
        assertThat(bug.get("reviewer").asText()).isEqualTo("Захар");
        mvc.perform(get("/api/bugs/"+bug.get("id").asText()).cookie(login.cookie())).andExpect(status().isOk());
        mvc.perform(get("/api/bugs/"+bug.get("id").asText()+"/history").cookie(login.cookie()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].action").value("CREATED"));
    }

    @Test void observerCannotCreateEvenWithDirectRequest() throws Exception {
        Login login=login("observer@example.org");long count=bugs.count();
        mvc.perform(post("/api/projects/"+DemoData.SHOP_ID+"/bugs").cookie(login.cookie())
            .header("X-CSRF-Token",login.csrf()).contentType(MediaType.APPLICATION_JSON).content(draft("Попытка")))
            .andExpect(status().isForbidden());
        assertThat(bugs.count()).isEqualTo(count);
    }

    @Test void validatesWhitespaceAndDoesNotWriteHistory() throws Exception {
        Login login=login("zakhar@example.org");long bugCount=bugs.count(),historyCount=history.count();
        mvc.perform(post("/api/projects/"+DemoData.SHOP_ID+"/bugs").cookie(login.cookie())
            .header("X-CSRF-Token",login.csrf()).contentType(MediaType.APPLICATION_JSON).content(draft("   ")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.title").exists());
        assertThat(bugs.count()).isEqualTo(bugCount);assertThat(history.count()).isEqualTo(historyCount);
    }

    @Test void unknownPropertiesCannotSetAuthorOrStatus() throws Exception {
        Login login=login("zakhar@example.org");
        var data=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(draft("Подмена"));data.put("status","CLOSED");
        mvc.perform(post("/api/projects/"+DemoData.SHOP_ID+"/bugs").cookie(login.cookie()).header("X-CSRF-Token",login.csrf())
            .contentType(MediaType.APPLICATION_JSON).content(data.toString())).andExpect(status().isBadRequest());
    }

    @Test void developerCreatesWithoutAssigningThemselfAsReviewer() throws Exception {
        var bug=create(login("ivan@example.org"),"Ошибка разработчика");
        assertThat(bug.get("reviewer").isNull()).isTrue();
    }

    @Test void requiresSessionAndRejectsExpiredSession() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        Login login=login("zakhar@example.org");
        var u=users.findByEmail("zakhar@example.org").orElseThrow();
        var helper=new ru.hits.bugtrack.service.SessionService(sessions);
        String expiredToken=helper.newToken();
        sessions.save(new UserSession(UUID.randomUUID(),u,helper.hash(expiredToken),"x",Instant.now().minusSeconds(1)));
        mvc.perform(get("/api/projects").cookie(new Cookie("BUGTRACK_SESSION",expiredToken))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/projects").cookie(new Cookie("BUGTRACK_SESSION","invalid"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").cookie(login.cookie())).andExpect(status().isOk());
    }

    @Test void logoutRevokesSession() throws Exception {
        Login login=login("zakhar@example.org");
        mvc.perform(post("/api/auth/logout").cookie(login.cookie()).header("X-CSRF-Token",login.csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").cookie(login.cookie())).andExpect(status().isUnauthorized());
    }

    @Test void protectsMutationsFromCsrfAndCrossOrigin() throws Exception {
        Login login=login("zakhar@example.org");
        mvc.perform(post("/api/projects/"+DemoData.SHOP_ID+"/bugs").cookie(login.cookie())
            .contentType(MediaType.APPLICATION_JSON).content(draft("Без токена"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").header("Origin","https://other.example")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test void rejectsWrongPasswordWithoutCreatingSession() throws Exception {
        long count=sessions.count();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"zakhar@example.org\",\"password\":\"incorrect\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        assertThat(sessions.count()).isEqualTo(count);
    }

    @Test void hidesAnotherProjectsBugsAndHistory() throws Exception {
        Login login=login("zakhar@example.org");
        Project privateProject=projects.save(new Project(UUID.randomUUID(),"PRIVATE","Закрытый проект",null,null,1));
        mvc.perform(get("/api/projects/"+privateProject.getId()+"/bugs").cookie(login.cookie())).andExpect(status().isNotFound());
        var outsider=users.save(new User(UUID.randomUUID(),"Посторонний","outside-"+UUID.randomUUID()+"@example.org","unused"));
        var hash=new ru.hits.bugtrack.service.SessionService(sessions);
        String token=hash.newToken();
        sessions.save(new UserSession(UUID.randomUUID(),outsider,hash.hash(token),"csrf",Instant.now().plusSeconds(600)));
        var bug=create(login,"Недоступный баг");
        Cookie cookie=new Cookie("BUGTRACK_SESSION",token);
        mvc.perform(get("/api/bugs/"+bug.get("id").asText()).cookie(cookie)).andExpect(status().isNotFound());
        mvc.perform(get("/api/bugs/"+bug.get("id").asText()+"/history").cookie(cookie)).andExpect(status().isNotFound());
        mvc.perform(get("/api/projects").cookie(cookie)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test void searchesByCodeAndTitleAndCombinesFilters() throws Exception {
        Login login=login("zakhar@example.org");String unique="Проверка-"+UUID.randomUUID();var bug=create(login,unique);
        mvc.perform(get("/api/projects/"+DemoData.SHOP_ID+"/bugs").param("query",bug.get("code").asText()).cookie(login.cookie()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/projects/"+DemoData.SHOP_ID+"/bugs").param("query",unique).param("priority","LOW").cookie(login.cookie()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/projects/"+DemoData.SHOP_ID+"/bugs").param("query",unique).param("status","NEW").param("priority","HIGH").cookie(login.cookie()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    @Test void concurrentCreatesAllocateUniqueNumbersAndHistory() throws Exception {
        UUID user=users.findByEmail("zakhar@example.org").orElseThrow().getId();
        long before=history.count();
        var draft=new CreateBugRequest("Параллельный баг","Описание","Шаги","Ожидание","Ошибка",null,Severity.MAJOR,Priority.NORMAL);
        try(var pool=Executors.newFixedThreadPool(4)) {
            List<Callable<String>> calls=new ArrayList<>();
            for(int i=0;i<8;i++)calls.add(()->bugService.create(user,DemoData.SHOP_ID,draft).code());
            var codes=new HashSet<String>();
            for(var result:pool.invokeAll(calls)) codes.add(result.get(15,TimeUnit.SECONDS));
            assertThat(codes).hasSize(8);
        }
        assertThat(history.count()).isEqualTo(before+8);
    }
}
