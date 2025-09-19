package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.model.News;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.stream.Collectors;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {CommentMapper.class},
    imports = {Collectors.class}
)
public interface NewsMapper {

    News toNews(NewsRequest request);

    void updateNewsFromRequest(NewsRequest request, @MappingTarget News news);

    // === БЛОК ИЗМЕНЕНИЙ НАЧАЛО ===
    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "category.categoryName", target = "categoryName") // ИСПРАВЛЕНО
    @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    NewsResponse toNewsResponseForList(News news);

    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "category.categoryName", target = "categoryName") // ИСПРАВЛЕНО
    @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    @Mapping(target = "comments", expression = "java(news.getComments().stream().map(commentMapper::toResponse).collect(Collectors.toList()))")
    NewsResponse toNewsResponseWithComments(News news);
    // === БЛОК ИЗМЕНЕНИЙ КОНЕЦ ===
}
