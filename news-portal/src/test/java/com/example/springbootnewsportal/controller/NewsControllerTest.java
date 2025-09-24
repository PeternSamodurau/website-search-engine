package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.request.NewsUpdateRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.service.NewsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = NewsController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void findAll_shouldReturn200_whenNewsExist() throws Exception {
        // given
        NewsResponse response = createNewsResponse(1L, "Title", "Text", "user", "category");
        Page<NewsResponse> responsePage = new PageImpl<>(Collections.singletonList(response));
        when(newsService.findAll(any(), any(), any(Pageable.class))).thenReturn(responsePage);

        // when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        String expectedJson = readResourceFile("response/findAll_resp.json");

        assertThatJson(actualJsonResponse).isEqualTo(expectedJson);
    }

    @Test
    void findById_shouldReturn200_whenNewsExists() throws Exception {
        // given
        long newsId = 1L;
        NewsResponse response = createNewsResponse(newsId, "Title", "Text", "user", "category");
        when(newsService.findById(newsId)).thenReturn(response);

        // when
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/news/{id}", newsId))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        String expectedJson = readResourceFile("response/findById_resp.json");

        assertThatJson(actualJsonResponse).isEqualTo(expectedJson);
    }

    @Test
    void findById_shouldReturn404_whenNewsDoesNotExist() throws Exception {
        // given
        long newsId = 1L;
        when(newsService.findById(newsId)).thenThrow(new EntityNotFoundException("News not found"));

        // when & then
        mockMvc.perform(get("/api/v1/news/{id}", newsId))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createNews_shouldReturn201_whenRequestIsValid() throws Exception {
        // given
        NewsRequest request = new NewsRequest();
        request.setTitle("New Title");
        request.setText("New Text");
        request.setAuthorId(1L);
        request.setCategoryId(1L);

        NewsResponse response = createNewsResponse(1L, "New Title", "New Text", "user", "category");
        when(newsService.create(any(NewsRequest.class))).thenReturn(response);

        // when
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // then
        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        String expectedJson = readResourceFile("response/createNews_resp.json");

        assertThatJson(actualJsonResponse).isEqualTo(expectedJson);
    }

    @Test
    void createNews_shouldReturn400_whenRequestIsInvalid() throws Exception {
        // given
        NewsRequest request = new NewsRequest(); // Invalid request
        request.setTitle(""); // Blank title

        // when & then
        mockMvc.perform(post("/api/v1/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateNews_shouldReturn200_whenRequestIsValid() throws Exception {
        // given
        long newsId = 1L;
        NewsUpdateRequest request = new NewsUpdateRequest();
        request.setTitle("Updated Title");
        request.setText("Updated Text");

        NewsResponse response = createNewsResponse(newsId, "Updated Title", "Updated Text", "user", "category");
        when(newsService.update(anyLong(), any(NewsUpdateRequest.class))).thenReturn(response);

        // when
        MvcResult mvcResult = mockMvc.perform(put("/api/v1/news/{id}", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // then
        String actualJsonResponse = mvcResult.getResponse().getContentAsString();
        String expectedJson = readResourceFile("response/updateNews_resp.json");

        assertThatJson(actualJsonResponse).isEqualTo(expectedJson);
    }

    @Test
    void deleteNews_shouldReturn204_whenNewsExists() throws Exception {
        // given
        long newsId = 1L;
        doNothing().when(newsService).deleteById(newsId);

        // when & then
        mockMvc.perform(delete("/api/v1/news/{id}", newsId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteNews_shouldReturn404_whenNewsDoesNotExist() throws Exception {
        // given
        long newsId = 1L;
        doThrow(new EntityNotFoundException("News not found")).when(newsService).deleteById(newsId);

        // when & then
        mockMvc.perform(delete("/api/v1/news/{id}", newsId))
                .andExpect(status().isInternalServerError());
    }

    private NewsResponse createNewsResponse(Long id, String title, String text, String author, String category) {
        NewsResponse response = new NewsResponse();
        response.setId(id);
        response.setTitle(title);
        response.setText(text);
        response.setAuthorUsername(author);
        response.setCategoryName(category);
        response.setCreateAt(Instant.now());
        response.setUpdateAt(Instant.now());

        return response;
    }

    private String readResourceFile(String filePath) {
        try (Reader reader = new InputStreamReader(getClass().getClassLoader().getResourceAsStream(filePath), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}