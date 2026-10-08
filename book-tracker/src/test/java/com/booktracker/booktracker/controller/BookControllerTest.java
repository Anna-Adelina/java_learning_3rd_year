package com.booktracker.booktracker.controller;

import com.booktracker.booktracker.annotation.PostCreated;
import com.booktracker.booktracker.config.WebConfig;
import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC4/AC5 (@PostCreated) та AC8 (@CurrentUsername у реальному запиті).
 * Security-фільтри вимкнені, користувач передається через .principal(...).
 */
@WebMvcTest(
        controllers = BookController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class,
                OAuth2ResourceServerWebSecurityAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    private static final String VALID_BODY =
            "{\"title\":\"Dune\",\"authorId\":1,\"readingStatus\":\"READING\",\"totalPages\":300,\"pagesRead\":10}";

    // ---- AC4: composed == originals ----
    @Test
    void postCreated_hasSameMergedAnnotationsAsOriginals() throws Exception {
        Method create = BookController.class.getMethod("create", BookDto.class, String.class);

        assertNotNull(create.getAnnotation(PostCreated.class));

        ResponseStatus rs = AnnotatedElementUtils.findMergedAnnotation(create, ResponseStatus.class);
        assertNotNull(rs);
        assertEquals(HttpStatus.CREATED, rs.code());

        RequestMapping rm = AnnotatedElementUtils.findMergedAnnotation(create, RequestMapping.class);
        assertNotNull(rm);
        assertArrayEquals(new RequestMethod[]{RequestMethod.POST}, rm.method());
    }

    // ---- AC5: така ж поведінка в реальному запиті ----
    @Test
    void postCreated_returns201AndRoutesToCreate() throws Exception {
        when(bookService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/books")
                        .principal(new TestingAuthenticationToken("anna", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Dune"));
    }

    @Test
    void postCreated_wrongMethodIsNotRouted() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---- AC1/AC3 через реальний запит: constraint спрацьовує, повідомлення зрозуміле ----
    @Test
    void invalidReadingProgress_returns400WithClearMessage() throws Exception {
        String body = "{\"title\":\"Dune\",\"authorId\":1,\"readingStatus\":\"READING\","
                + "\"totalPages\":100,\"pagesRead\":150}";

        mockMvc.perform(post("/api/books")
                        .principal(new TestingAuthenticationToken("anna", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "pagesRead (150) cannot exceed totalPages (100)")));
        verifyNoInteractions(bookService);
    }

    // ---- AC8: резолвлене значення реально використовується в тілі методу ----
    @Test
    void currentUsername_isUsedInsideControllerMethod() throws Exception {
        when(bookService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        // клієнт намагається підмінити createdBy, але береться користувач із контексту запиту
        String body = "{\"title\":\"Dune\",\"authorId\":1,\"readingStatus\":\"PLANNED\",\"createdBy\":\"hacker\"}";

        mockMvc.perform(post("/api/books")
                        .principal(new TestingAuthenticationToken("anna", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("anna"));

        var captor = org.mockito.ArgumentCaptor.forClass(BookDto.class);
        verify(bookService).create(captor.capture());
        assertEquals("anna", captor.getValue().getCreatedBy());
    }
}
