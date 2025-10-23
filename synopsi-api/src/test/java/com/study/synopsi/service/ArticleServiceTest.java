package com.study.synopsi.service;

import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Feed;
import com.study.synopsi.repository.ArticleRepository;
import com.study.synopsi.repository.FeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private FeedRepository feedRepository;

    @InjectMocks
    private ArticleService articleService;

    private Article article;
    private Feed feed;

    @BeforeEach
    void setUp() {
        feed = new Feed();
        feed.setId(1L);
        feed.setTitle("Tech Feed");

        article = new Article();
        article.setId(1L);
        article.setTitle("Test Article");
        article.setOriginalUrl("http://example.com");
        article.setContent("Full content of the article.");
        article.setFeed(feed);
        article.setStatus(Article.ArticleStatus.PENDING);
        article.setPublicationDate(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Get Article Tests")
    class GetArticleTests {

        @Test
        @DisplayName("getAllArticles should return a list of articles")
        void getAllArticles_ShouldReturnArticleList() {
            // Arrange
            when(articleRepository.findAll()).thenReturn(List.of(article));

            // Act
            List<ArticleResponseDto> result = articleService.getAllArticles();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Test Article");
            verify(articleRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("getArticleById should return an article when found")
        void getArticleById_WhenFound_ShouldReturnArticle() {
            // Arrange
            when(articleRepository.findById(1L)).thenReturn(Optional.of(article));

            // Act
            ArticleResponseDto result = articleService.getArticleById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Article");
            verify(articleRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("getArticleById should throw ArticleNotFoundException when not found")
        void getArticleById_WhenNotFound_ShouldThrowException() {
            // Arrange
            when(articleRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            ArticleService.ArticleNotFoundException exception = assertThrows(
                    ArticleService.ArticleNotFoundException.class,
                    () -> articleService.getArticleById(99L)
            );

            assertThat(exception.getMessage()).isEqualTo("Article not found with id: 99");
            verify(articleRepository, times(1)).findById(99L);
        }
    }


    @Nested
    @DisplayName("Create Article Tests")
    class CreateArticleTests {

        @Test
        @DisplayName("createArticle should create and return a new article")
        void createArticle_ShouldSucceed() {
            // Arrange
            ArticleRequestDto requestDto = new ArticleRequestDto();
            requestDto.setTitle("New Article");
            requestDto.setOriginalUrl("http://new.com");
            requestDto.setContent("New content.");
            requestDto.setFeedId(1L);
            requestDto.setSummary("A summary."); // This will set the status to SUMMARIZED

            when(feedRepository.findById(1L)).thenReturn(Optional.of(feed));
            when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
                Article articleToSave = invocation.getArgument(0);
                articleToSave.setId(2L); // Simulate saving and getting an ID back
                return articleToSave;
            });

            // Act
            ArticleResponseDto result = articleService.createArticle(requestDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getTitle()).isEqualTo("New Article");
            assertThat(result.getStatus()).isEqualTo(Article.ArticleStatus.SUMMARIZED.name());
            assertThat(result.getSummarizedAt()).isNotNull();

            verify(feedRepository, times(1)).findById(1L);
            verify(articleRepository, times(1)).save(any(Article.class));
        }

        @Test
        @DisplayName("createArticle should throw IllegalArgumentException if feed not found")
        void createArticle_WhenFeedNotFound_ShouldThrowException() {
            // Arrange
            ArticleRequestDto requestDto = new ArticleRequestDto();
            requestDto.setFeedId(99L);

            when(feedRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> articleService.createArticle(requestDto)
            );

            assertThat(exception.getMessage()).isEqualTo("Feed not found with id: 99");
            verify(feedRepository, times(1)).findById(99L);
            verify(articleRepository, never()).save(any(Article.class)); // Ensure save was not called
        }
    }
}