package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.model.News;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = CommentMapper.class)
public interface NewsMapper {

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "category", ignore = true)
    News toNews(NewsRequest request);

    // Маппинг для полного ответа (с комментариями)
    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "category.categoryName", target = "categoryName")
    @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    NewsResponse toNewsResponseWithComments(News news);

    // Маппинг для краткого ответа (для списков)
    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "category.categoryName", target = "categoryName")
    @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    @Mapping(target = "comments", ignore = true)
    NewsResponse toNewsResponseForList(News news);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateNewsFromRequest(NewsRequest request, @MappingTarget News news);
}
