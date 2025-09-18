package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.model.News;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {CommentMapper.class})
public interface NewsMapper {

    News toNews(NewsRequest request);

    void updateNewsFromRequest(NewsRequest request, @MappingTarget News news);

    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "category.name", target = "name")
    @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    NewsResponse toNewsResponseWithComments(News news);

    @Named("toNewsResponseForList")
    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "category.name", target = "name")
    @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    @Mapping(target = "comments", ignore = true)
    NewsResponse toNewsResponseForList(News news);

    List<NewsResponse> toNewsResponseList(List<News> news);
}
