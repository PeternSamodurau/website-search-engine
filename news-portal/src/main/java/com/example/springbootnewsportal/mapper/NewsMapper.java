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

    @Mapping(target = "content", source = "content")
    News toNews(NewsRequest request);

    // ИСПРАВЛЕНО: контент теперь будет обновляться из запроса
    @Mapping(target = "content", source = "content")
    void updateNewsFromRequest(NewsRequest request, @MappingTarget News news);

    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "category.name", target = "name")
    @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    @Mapping(target = "text", source = "content")
    @Mapping(target = "createAt", source = "createAt")
    @Mapping(target = "updateAt", source = "updateAt")
    NewsResponse toNewsResponseForList(News news);

    @Mapping(source = "author.username", target = "authorUsername")
    @Mapping(source = "category.name", target = "name")
    @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    @Mapping(target = "comments", expression = "java(news.getComments().stream().map(commentMapper::toResponse).collect(Collectors.toList()))")
    @Mapping(target = "text", source = "content")
    @Mapping(target = "createAt", source = "createAt")
    @Mapping(target = "updateAt", source = "updateAt")
    NewsResponse toNewsResponseWithComments(News news);
}
