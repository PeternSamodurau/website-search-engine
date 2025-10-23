package com.example.springbootnewsportal.controller;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.request.NewsUpdateRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.mapper.CommentMapper;
import com.example.springbootnewsportal.mapper.NewsMapper;
import com.example.springbootnewsportal.service.NewsService;
import com.example.springbootnewsportal.service.impl.CommentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NewsController.class)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NewsService newsService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CommentServiceImpl commentServiceImpl;

    @MockBean
    private NewsMapper newsMapper;

    @MockBean
    private CommentMapper commentMapper;

    @Test
    @WithMockUser
    void getAllNews_shouldReturnOk() throws Exception {
        Page<NewsResponse> responsePage = new PageImpl<>(Collections.singletonList(new NewsResponse()));
        when(newsService.findAll(any(), any(Pageable.class))).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getNewsById_shouldReturnOk() throws Exception {
        when(newsService.findById(1L)).thenReturn(new NewsResponse());

        mockMvc.perform(get("/api/v1/news/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void createNews_shouldReturnCreated() throws Exception {
        NewsRequest request = new NewsRequest();
        request.setTitle("Test Title");
        request.setText("Test Text");
        request.setCategoryId(1L);

        NewsResponse response = new NewsResponse();
        response.setId(1L);

        when(newsService.create(any(NewsRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/news")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void updateNews_shouldReturnOk() throws Exception {
        NewsUpdateRequest request = new NewsUpdateRequest();
        request.setTitle("Updated Title");

        when(newsService.isNewsAuthor(any(), any())).thenReturn(true);
        when(newsService.update(any(), any())).thenReturn(new NewsResponse());

        mockMvc.perform(put("/api/v1/news/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void deleteNews_shouldReturnNoContent() throws Exception {
        when(newsService.isNewsAuthor(any(), any())).thenReturn(true);

        mockMvc.perform(delete("/api/v1/news/1"))
                .andExpect(status().isNoContent());
    }
}
