package com.booktracker.reporting.client;

import com.booktracker.booktracker.dto.BookDto;
import com.booktracker.booktracker.entity.ReadingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestBookClientTest {

    @Test
    void findAll_callsBooksEndpoint_andMapsJsonToCommonDto() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://book-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://book-service/api/books"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"id\":1,\"title\":\"Dune\",\"authorId\":2,\"readingStatus\":\"READING\",\"createdBy\":\"anna\"}]",
                        MediaType.APPLICATION_JSON));

        List<BookDto> books = new RestBookClient(builder.build()).findAll();

        assertEquals(1, books.size());
        assertEquals("Dune", books.get(0).getTitle());
        assertEquals(ReadingStatus.READING, books.get(0).getReadingStatus());
        assertEquals("anna", books.get(0).getCreatedBy());
        server.verify();
    }

    @Test
    void findAll_returnsEmptyList_whenBodyIsEmptyArray() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://book-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://book-service/api/books"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(new RestBookClient(builder.build()).findAll().isEmpty());
    }
}
