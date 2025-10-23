package com.study.synopsi.service;

import com.study.synopsi.exception.ArticleNotFoundException;
import com.study.synopsi.exception.SummaryJobNotFoundException;
import com.study.synopsi.model.Article;
import com.study.synopsi.model.Summary;
import com.study.synopsi.model.SummaryJob;
import com.study.synopsi.model.User;
import com.study.synopsi.repository.ArticleRepository;
import com.study.synopsi.repository.SummaryJobRepository;
import com.study.synopsi.repository.SummaryRepository;
import com.study.synopsi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private SummaryRepository summaryRepository;
    @Mock
    private SummaryJobRepository summaryJobRepository;
    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SummaryService summaryService;

    private Article article;
    private User user;

    @BeforeEach
    void setUp() {
        article = new Article();
        article.setId(1L);
        article.setStatus(Article.ArticleStatus.PENDING);

        user = new User();
        user.setId(10L);
    }

    // ... (no changes to the passing tests)

    @Test
    void requestSummary_whenUserRequests_createsHighPriorityJob() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(summaryJobRepository.save(any(SummaryJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SummaryJob job = summaryService.requestSummary(1L, 10L, Summary.SummaryType.LIST, Summary.SummaryLength.MEDIUM);

        // Then
        assertNotNull(job);
        assertEquals(article, job.getArticle());
        assertEquals(user, job.getUser());
        assertEquals(7, job.getPriority()); // User jobs have higher priority
        assertEquals(SummaryJob.JobStatus.QUEUED, job.getStatus());

        verify(summaryJobRepository).save(any(SummaryJob.class));
        verify(articleRepository).save(article);
        assertEquals(Article.ArticleStatus.PROCESSING, article.getStatus());
    }

    @Test
    void requestSummary_whenNoUser_createsDefaultPriorityJob() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(summaryJobRepository.save(any(SummaryJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        SummaryJob job = summaryService.requestSummary(1L, null, Summary.SummaryType.BRIEF, Summary.SummaryLength.SHORT);

        // Then
        assertNotNull(job);
        assertEquals(article, job.getArticle());
        assertNull(job.getUser());
        assertEquals(5, job.getPriority()); // Default jobs have lower priority
        assertEquals(SummaryJob.JobStatus.QUEUED, job.getStatus());

        verify(summaryJobRepository).save(any(SummaryJob.class));
        verify(articleRepository).save(article);
    }

    @Test
    void requestSummary_whenArticleNotFound_throwsException() {
        // Given
        when(articleRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ArticleNotFoundException.class, () ->
                summaryService.requestSummary(99L, 10L, Summary.SummaryType.LIST, Summary.SummaryLength.MEDIUM)
        );
    }

    @Test
    void requestSummary_whenActiveJobExists_throwsException() {
        // Given
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(summaryJobRepository.existsByArticleIdAndUserIdAndStatusIn(anyLong(), anyLong(), anyList())).thenReturn(true);

        // When & Then
        Exception exception = assertThrows(RuntimeException.class, () ->
                summaryService.requestSummary(1L, 10L, Summary.SummaryType.LIST, Summary.SummaryLength.MEDIUM)
        );
        assertEquals("Summary generation already in progress", exception.getMessage());
    }

    @Test
    void getSummary_whenUserSummaryExists_returnsUserSummary() {
        // Given
        Summary userSummary = new Summary();
        userSummary.setId(100L);
        userSummary.setUser(user);

        when(summaryRepository.findByArticleIdAndUserIdAndSummaryTypeAndStatus(1L, 10L, Summary.SummaryType.LIST, Summary.SummaryStatus.COMPLETED))
                .thenReturn(Optional.of(userSummary));

        // When
        Optional<Summary> result = summaryService.getSummary(1L, 10L, Summary.SummaryType.LIST);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userSummary, result.get());
        verify(summaryRepository, never()).findByArticleIdAndUserIsNullAndSummaryTypeAndStatus(any(), any(), any());
    }

    @Test
    void getSummary_whenOnlyDefaultSummaryExists_returnsDefaultSummary() {
        // Given
        Summary defaultSummary = new Summary();
        defaultSummary.setId(101L);

        when(summaryRepository.findByArticleIdAndUserIdAndSummaryTypeAndStatus(1L, 10L, Summary.SummaryType.LIST, Summary.SummaryStatus.COMPLETED))
                .thenReturn(Optional.empty());
        when(summaryRepository.findByArticleIdAndUserIsNullAndSummaryTypeAndStatus(1L, Summary.SummaryType.LIST, Summary.SummaryStatus.COMPLETED))
                .thenReturn(Optional.of(defaultSummary));

        // When
        Optional<Summary> result = summaryService.getSummary(1L, 10L, Summary.SummaryType.LIST);

        // Then
        assertTrue(result.isPresent());
        assertEquals(defaultSummary, result.get());
    }

    @Test
    void handleWorkerCallback_forDefaultSummary_updatesArticleStatus() {
        // Given
        SummaryJob job = new SummaryJob();
        job.setId(5L);
        job.setArticle(article);
        job.setUser(null); // Default summary job
        job.setStatus(SummaryJob.JobStatus.PROCESSING);

        when(summaryJobRepository.findById(5L)).thenReturn(Optional.of(job));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        summaryService.handleWorkerCallback(5L, "This is the summary.", "v1.0", 150);

        // Then
        ArgumentCaptor<Summary> summaryCaptor = ArgumentCaptor.forClass(Summary.class);
        verify(summaryRepository).save(summaryCaptor.capture());
        Summary savedSummary = summaryCaptor.getValue();
        assertEquals("This is the summary.", savedSummary.getSummaryText());
        assertEquals(Summary.SummaryStatus.COMPLETED, savedSummary.getStatus());
        assertNull(savedSummary.getUser());

        assertEquals(SummaryJob.JobStatus.COMPLETED, job.getStatus());
        verify(summaryJobRepository).save(job);

        assertEquals(Article.ArticleStatus.SUMMARIZED, article.getStatus());
        verify(articleRepository).save(article);
    }

    @Test
    void handleWorkerCallback_forUserSummary_doesNotUpdateArticleStatus() {
        // Given
        SummaryJob job = new SummaryJob();
        job.setId(6L);
        job.setArticle(article);
        job.setUser(user); // User-specific job
        job.setStatus(SummaryJob.JobStatus.PROCESSING);

        when(summaryJobRepository.findById(6L)).thenReturn(Optional.of(job));

        // When
        summaryService.handleWorkerCallback(6L, "User summary.", "v1.1", 120);

        // Then
        assertEquals(Article.ArticleStatus.PENDING, article.getStatus()); // Original status
        verify(articleRepository, never()).save(article);
    }

    @Test
    void handleWorkerFailure_whenCanRetry_requeuesJob() {
        // Given
        SummaryJob job = new SummaryJob();
        job.setId(7L);
        job.setAttempts(1);
        job.setMaxAttempts(3);
        job.setStatus(SummaryJob.JobStatus.PROCESSING);
        job.setArticle(article); // <<< FIX: Added article to prevent NPE in submitToWorker

        when(summaryJobRepository.findById(7L)).thenReturn(Optional.of(job));

        // When
        summaryService.handleWorkerFailure(7L, "Worker timeout");

        // Then
        assertEquals(SummaryJob.JobStatus.QUEUED, job.getStatus());
        assertEquals(2, job.getAttempts());
        assertTrue(job.getErrorMessage().contains("will retry"));
        verify(summaryJobRepository).save(job);
    }

    @Test
    void handleWorkerFailure_whenMaxRetriesExceeded_failsJobAndArticle() {
        // Given
        SummaryJob job = new SummaryJob();
        job.setId(8L);
        job.setArticle(article);
        job.setUser(null); // Default job
        job.setAttempts(3);
        job.setMaxAttempts(3);
        job.setStatus(SummaryJob.JobStatus.PROCESSING);

        when(summaryJobRepository.findById(8L)).thenReturn(Optional.of(job));

        // When
        summaryService.handleWorkerFailure(8L, "Permanent error");

        // Then
        assertEquals(SummaryJob.JobStatus.FAILED, job.getStatus());
        assertEquals(4, job.getAttempts());
        assertFalse(job.getErrorMessage().contains("will retry"));
        verify(summaryJobRepository).save(job);

        assertEquals(Article.ArticleStatus.FAILED, article.getStatus());
        verify(articleRepository).save(article);
    }

    @Test
    void regenerateSummary_createsNewJobAndIncrementsCounter() {
        // Given
        Summary existingSummary = new Summary();
        existingSummary.setId(20L);
        existingSummary.setArticle(article);
        existingSummary.setUser(user);
        existingSummary.setSummaryType(Summary.SummaryType.LIST);
        existingSummary.setSummaryLength(Summary.SummaryLength.SHORT);
        existingSummary.setRegenerationCount(0);

        when(summaryRepository.findById(20L)).thenReturn(Optional.of(existingSummary));

        // Mock the call to requestSummary
        when(articleRepository.findById(1L)).thenReturn(Optional.of(article));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(summaryJobRepository.save(any(SummaryJob.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        summaryService.regenerateSummary(20L);

        // Then
        assertEquals(1, existingSummary.getRegenerationCount());
        verify(summaryRepository).save(existingSummary);

        // Verify a new job was created with the correct parameters
        ArgumentCaptor<SummaryJob> jobCaptor = ArgumentCaptor.forClass(SummaryJob.class);
        verify(summaryJobRepository).save(jobCaptor.capture());
        SummaryJob newJob = jobCaptor.getValue();
        assertEquals(Summary.SummaryType.LIST, newJob.getSummaryType());
        assertEquals(Summary.SummaryLength.SHORT, newJob.getSummaryLength());
        assertEquals(article, newJob.getArticle());
        assertEquals(user, newJob.getUser());
    }

    @Test
    void retryFailedJob_whenJobIsFailed_requeuesJob() {
        // Given
        SummaryJob job = new SummaryJob();
        job.setId(9L);
        job.setStatus(SummaryJob.JobStatus.FAILED);
        job.setAttempts(2);
        job.setMaxAttempts(3);
        job.setArticle(article); // <<< FIX: Added article to prevent NPE in submitToWorker

        when(summaryJobRepository.findById(9L)).thenReturn(Optional.of(job));
        when(summaryJobRepository.save(any(SummaryJob.class))).thenReturn(job);

        // When
        SummaryJob retriedJob = summaryService.retryFailedJob(9L);

        // Then
        assertEquals(SummaryJob.JobStatus.QUEUED, retriedJob.getStatus());
        assertNull(retriedJob.getErrorMessage());
        verify(summaryJobRepository).save(job);
    }

    @Test
    void retryFailedJob_whenJobNotFailed_throwsException() {
        // Given
        SummaryJob job = new SummaryJob();
        job.setId(9L);
        job.setStatus(SummaryJob.JobStatus.COMPLETED); // Not FAILED

        when(summaryJobRepository.findById(9L)).thenReturn(Optional.of(job));

        // When & Then
        Exception exception = assertThrows(IllegalStateException.class, () ->
                summaryService.retryFailedJob(9L)
        );
        assertEquals("Can only retry FAILED jobs", exception.getMessage());
    }

    @Test
    void cleanupOldJobs_callsRepositoryWithCorrectCutoffDate() {
        // When
        summaryService.cleanupOldJobs();

        // Then
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(summaryJobRepository).deleteOldCompletedJobs(captor.capture());

        // Verify the cutoff date is approximately 7 days ago
        assertTrue(captor.getValue().isBefore(LocalDateTime.now().minusDays(6)));
    }

    @Test
    void checkStaleJobs_handlesStaleJobsCorrectly() {
        // Given
        SummaryJob staleJob = new SummaryJob();
        staleJob.setId(50L);
        staleJob.setArticle(article);
        staleJob.setUser(null);
        staleJob.setAttempts(0);
        staleJob.setMaxAttempts(3);

        List<SummaryJob> staleJobs = Collections.singletonList(staleJob);
        when(summaryJobRepository.findStaleProcessingJobs(any(LocalDateTime.class))).thenReturn(staleJobs);
        // <<< FIX: Added mock for the findById call that happens inside handleWorkerFailure
        when(summaryJobRepository.findById(50L)).thenReturn(Optional.of(staleJob));

        // When
        summaryService.checkStaleJobs();

        // Then
        // Verify handleWorkerFailure logic is triggered for the stale job
        assertEquals(SummaryJob.JobStatus.QUEUED, staleJob.getStatus());
        assertEquals(1, staleJob.getAttempts());
        assertTrue(staleJob.getErrorMessage().contains("Job timeout"));
        verify(summaryJobRepository).save(staleJob);
    }
}