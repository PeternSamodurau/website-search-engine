package com.example.springbootnewsportal.mapper;

import com.example.springbootnewsportal.dto.request.NewsRequest;
import com.example.springbootnewsportal.dto.request.NewsUpdateRequest;
import com.example.springbootnewsportal.dto.response.CommentResponse;
import com.example.springbootnewsportal.dto.response.NewsResponse;
import com.example.springbootnewsportal.model.Comment;
import com.example.springbootnewsportal.model.News;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import java.util.List;

@Mapper(componentModel = "spring")
public interface NewsMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createAt", ignore = true),
            @Mapping(target = "updateAt", ignore = true),
            @Mapping(target = "author", ignore = true),
            @Mapping(target = "category", ignore = true),
            @Mapping(target = "comments", ignore = true)
    })
    News toNews(NewsRequest request);

    @Mappings({
            @Mapping(target = "authorUsername", source = "author.username"),
            @Mapping(target = "categoryName", source = "category.categoryName"),
            @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    })
    NewsResponse toNewsResponse(News news);

    @Mappings({
            @Mapping(target = "authorUsername", source = "author.username"),
            @Mapping(target = "categoryName", source = "category.categoryName"),
            @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    })
    NewsResponse toNewsResponseForList(News news);

    @Mappings({
            @Mapping(target = "authorUsername", source = "author.username"),
            @Mapping(target = "categoryName", source = "category.categoryName"),
            @Mapping(target = "comments", source = "comments"),
            @Mapping(target = "commentsCount", expression = "java(news.getComments() != null ? (long) news.getComments().size() : 0L)")
    })
    NewsResponse toNewsResponseWithComments(News news);

    @Mapping(target = "authorUsername", source = "author.username")
    CommentResponse toCommentResponse(Comment comment);

    List<CommentResponse> toCommentResponseList(List<Comment> comments);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createAt", ignore = true),
            @Mapping(target = "updateAt", ignore = true),
            @Mapping(target = "author", ignore = true),
            @Mapping(target = "category", ignore = true),
            @Mapping(target = "comments", ignore = true)
    })
    void updateNewsFromRequest(NewsUpdateRequest request, @MappingTarget News news);
}

