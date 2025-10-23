package com.study.synopsi.service;

import com.study.synopsi.dto.ArticleRequestDto;
import com.study.synopsi.dto.ArticleResponseDto;
import com.study.synopsi.mapper.ArticleMapper;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Feed;
import com.study.synopsi.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    private ArticleMapper articleMapper;

    @InjectMocks
    private ArticleService articleService;

    private Article article;
    private Feed feed;
    private ArticleResponseDto responseDto;
    private ArticleRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // Setup Feed
        feed = new Feed();
        feed.setId(1L);
        feed.setTitle("Tech Feed");

        // Setup Article Entity
        article = new Article();
        article.setId(1L);
        article.setTitle("Test Article");
        article.setOriginalUrl("http://example.com");
        article.setContent("Full content of the article.");
        article.setFeed(feed);
        article.setStatus(Article.ArticleStatus.PENDING);
        article.setPublicationDate(LocalDateTime.now());
        article.setLanguage("en");
        article.setReadTimeMinutes(5);

        // Setup Response DTO
        responseDto = new ArticleResponseDto();
        responseDto.setId(1L);
        responseDto.setTitle("Test Article");
        responseDto.setOriginalUrl("http://example.com");
        responseDto.setContent("Full content of the article.");
        responseDto.setStatus("PENDING");
        responseDto.setFeedTitle("Tech Feed");
        responseDto.setLanguage("en");
        responseDto.setReadTimeMinutes(5);

        // Setup Request DTO
        requestDto = new ArticleRequestDto();
        requestDto.setTitle("New Article");
        requestDto.setOriginalUrl("http://new.com");
        requestDto.setContent("New content.");
        requestDto.setFeedId(1L);
        requestDto.setSummary("A summary.");
    }

    @Nested
    @DisplayName("Get Article Tests")
    class GetArticleTests {

        @Test
        @DisplayName("getAllArticles should return a list of articles")
        void getAllArticles_ShouldReturnArticleList() {
            // Arrange
            List<Article> articles = List.of(article);
            List<ArticleResponseDto> responseDtos = List.of(responseDto);

            when(articleRepository.findAll()).thenReturn(articles);
            when(articleMapper.toDtoList(articles)).thenReturn(responseDtos);

            // Act
            List<ArticleResponseDto> result = articleService.getAllArticles();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Test Article");

            verify(articleRepository, times(1)).findAll();
            verify(articleMapper, times(1)).toDtoList(articles);
        }

        @Test
        @DisplayName("getArticleById should return an article when found")
        void getArticleById_WhenFound_ShouldReturnArticle() {
            // Arrange
            when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
            when(articleMapper.toDto(article)).thenReturn(responseDto);

            // Act
            ArticleResponseDto result = articleService.getArticleById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Test Article");

            verify(articleRepository, times(1)).findById(1L);
            verify(articleMapper, times(1)).toDto(article);
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
            verify(articleMapper, never()).toDto(any());
        }
    }

    @Nested
    @DisplayName("Create Article Tests")
    class CreateArticleTests {

        @Test
        @DisplayName("createArticle should create and return a new article")
        void createArticle_ShouldSucceed() {
            // Arrange
            Article newArticle = new Article();
            newArticle.setTitle("New Article");
            newArticle.setOriginalUrl("http://new.com");
            newArticle.setContent("New content.");
            newArticle.setFeed(feed);
            newArticle.setStatus(Article.ArticleStatus.PENDING);

            Article savedArticle = new Article();
            savedArticle.setId(2L);
            savedArticle.setTitle("New Article");
            savedArticle.setOriginalUrl("http://new.com");
            savedArticle.setContent("New content.");
            savedArticle.setFeed(feed);
            savedArticle.setStatus(Article.ArticleStatus.PENDING);

            ArticleResponseDto savedResponseDto = new ArticleResponseDto();
            savedResponseDto.setId(2L);
            savedResponseDto.setTitle("New Article");
            savedResponseDto.setStatus("PENDING");

            when(articleMapper.toEntity(requestDto)).thenReturn(newArticle);
            when(articleRepository.save(newArticle)).thenReturn(savedArticle);
            when(articleMapper.toDto(savedArticle)).thenReturn(savedResponseDto);

            // Act
            ArticleResponseDto result = articleService.createArticle(requestDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(2L);
            assertThat(result.getTitle()).isEqualTo("New Article");
            assertThat(result.getStatus()).isEqualTo("PENDING");

            verify(articleMapper, times(1)).toEntity(requestDto);
            verify(articleRepository, times(1)).save(newArticle);
            verify(articleMapper, times(1)).toDto(savedArticle);
        }

        @Test
        @DisplayName("createArticle should throw IllegalArgumentException if feed not found")
        void createArticle_WhenFeedNotFound_ShouldThrowException() {
            // Arrange
            ArticleRequestDto badRequestDto = new ArticleRequestDto();
            badRequestDto.setFeedId(99L);

            // Mapper throws exception when feedId not found
            when(articleMapper.toEntity(badRequestDto))
                    .thenThrow(new IllegalArgumentException("Feed not found with id: 99"));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> articleService.createArticle(badRequestDto)
            );

            assertThat(exception.getMessage()).isEqualTo("Feed not found with id: 99");
            verify(articleMapper, times(1)).toEntity(badRequestDto);
            verify(articleRepository, never()).save(any(Article.class));
        }
    }

    @Nested
    @DisplayName("Update Article Tests")
    class UpdateArticleTests {

        @Test
        @DisplayName("updateArticle should update and return the article")
        void updateArticle_ShouldSucceed() {
            // Arrange
            ArticleRequestDto updateDto = new ArticleRequestDto();
            updateDto.setSummary("Updated summary");
            updateDto.setAuthor("Updated Author");

            Article updatedArticle = new Article();
            updatedArticle.setId(1L);
            updatedArticle.setTitle("Test Article");
            updatedArticle.setSummary("Updated summary");
            updatedArticle.setAuthor("Updated Author");

            ArticleResponseDto updatedResponseDto = new ArticleResponseDto();
            updatedResponseDto.setId(1L);
            updatedResponseDto.setTitle("Test Article");
            updatedResponseDto.setSummary("Updated summary");
            updatedResponseDto.setAuthor("Updated Author");

            when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
            doNothing().when(articleMapper).updateEntityFromDto(updateDto, article);
            when(articleRepository.save(article)).thenReturn(updatedArticle);
            when(articleMapper.toDto(updatedArticle)).thenReturn(updatedResponseDto);

            // Act
            ArticleResponseDto result = articleService.updateArticle(1L, updateDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getSummary()).isEqualTo("Updated summary");
            assertThat(result.getAuthor()).isEqualTo("Updated Author");

            verify(articleRepository, times(1)).findById(1L);
            verify(articleMapper, times(1)).updateEntityFromDto(updateDto, article);
            verify(articleRepository, times(1)).save(article);
            verify(articleMapper, times(1)).toDto(updatedArticle);
        }

        @Test
        @DisplayName("updateArticle should throw ArticleNotFoundException when article not found")
        void updateArticle_WhenNotFound_ShouldThrowException() {
            // Arrange
            ArticleRequestDto updateDto = new ArticleRequestDto();
            when(articleRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            ArticleService.ArticleNotFoundException exception = assertThrows(
                    ArticleService.ArticleNotFoundException.class,
                    () -> articleService.updateArticle(99L, updateDto)
            );

            assertThat(exception.getMessage()).isEqualTo("Article not found with id: 99");
            verify(articleRepository, times(1)).findById(99L);
            verify(articleMapper, never()).updateEntityFromDto(any(), any());
        }
    }

    @Nested
    @DisplayName("Delete Article Tests")
    class DeleteArticleTests {

        @Test
        @DisplayName("deleteArticle should delete the article when found")
        void deleteArticle_ShouldSucceed() {
            // Arrange
            when(articleRepository.existsById(1L)).thenReturn(true);
            doNothing().when(articleRepository).deleteById(1L);

            // Act
            articleService.deleteArticle(1L);

            // Assert
            verify(articleRepository, times(1)).existsById(1L);
            verify(articleRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("deleteArticle should throw ArticleNotFoundException when not found")
        void deleteArticle_WhenNotFound_ShouldThrowException() {
            // Arrange
            when(articleRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            ArticleService.ArticleNotFoundException exception = assertThrows(
                    ArticleService.ArticleNotFoundException.class,
                    () -> articleService.deleteArticle(99L)
            );

            assertThat(exception.getMessage()).isEqualTo("Article not found with id: 99");
            verify(articleRepository, times(1)).existsById(99L);
            verify(articleRepository, never()).deleteById(any());
        }
    }
}