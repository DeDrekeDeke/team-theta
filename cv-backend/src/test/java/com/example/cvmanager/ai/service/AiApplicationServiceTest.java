package com.example.cvmanager.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.cvmanager.ai.model.AiActionType;
import com.example.cvmanager.ai.model.AiSuggestion;
import com.example.cvmanager.ai.repository.AiSuggestionRepository;
import com.example.cvmanager.cv.model.Cv;
import com.example.cvmanager.cv.service.CvService;
import com.example.cvmanager.user.model.UserAccount;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AiApplicationServiceTest {

    private CvService cvService;
    private AiService aiService;
    private AiSuggestionRepository aiSuggestionRepository;
    private AiApplicationService aiApplicationService;

    @BeforeEach
    void setUp() {
        cvService = mock(CvService.class);
        aiService = mock(AiService.class);
        aiSuggestionRepository = mock(AiSuggestionRepository.class);
        aiApplicationService = new AiApplicationService(cvService, aiService, aiSuggestionRepository);
    }

    @Test
    void improveSummaryPersistsPendingSuggestionForCvHtml() {
        Cv cv = cv(42L);
        String sourceHtml = "<html><body>Alice summary</body></html>";
        when(cvService.findCv(42L)).thenReturn(cv);
        when(cvService.getUploadedHtml(42L)).thenReturn(sourceHtml);
        when(aiService.suggest(AiActionType.IMPROVE_SUMMARY, sourceHtml))
                .thenReturn("Improved summary");
        ArgumentCaptor<AiSuggestion> suggestionCaptor = ArgumentCaptor.forClass(AiSuggestion.class);
        when(aiSuggestionRepository.save(suggestionCaptor.capture())).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
            ReflectionTestUtils.setField(suggestion, "id", 100L);
            ReflectionTestUtils.setField(suggestion, "createdAt", LocalDateTime.of(2026, 6, 10, 13, 0));
            return suggestion;
        });

        var response = aiApplicationService.improveSummary(42L);

        assertEquals(100L, response.id());
        assertEquals(42L, response.cvId());
        assertEquals(AiActionType.IMPROVE_SUMMARY, response.actionType());
        assertEquals(sourceHtml, response.originalText());
        assertEquals("Improved summary", response.suggestedText());
        assertEquals("PENDING", response.status());

        AiSuggestion saved = suggestionCaptor.getValue();
        assertSame(cv, saved.getCv());
        assertEquals(sourceHtml, saved.getOriginalText());
        assertEquals("Improved summary", saved.getSuggestedText());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    void listSuggestionsReturnsRepositoryResultsInRepositoryOrder() {
        AiSuggestion newest = suggestion(2L, cv(42L), "newest", LocalDateTime.of(2026, 6, 10, 13, 0));
        AiSuggestion oldest = suggestion(1L, cv(42L), "oldest", LocalDateTime.of(2026, 6, 10, 12, 0));
        when(aiSuggestionRepository.findByCvIdOrderByCreatedAtDesc(42L)).thenReturn(List.of(newest, oldest));

        var response = aiApplicationService.listSuggestions(42L);

        assertEquals(2, response.size());
        assertEquals(2L, response.get(0).id());
        assertEquals("newest", response.get(0).suggestedText());
        assertEquals(1L, response.get(1).id());
        verify(aiSuggestionRepository).findByCvIdOrderByCreatedAtDesc(42L);
    }

    private AiSuggestion suggestion(Long id, Cv cv, String suggestedText, LocalDateTime createdAt) {
        AiSuggestion suggestion = new AiSuggestion(
                cv,
                AiActionType.IMPROVE_SUMMARY,
                "original",
                suggestedText,
                "PENDING");
        ReflectionTestUtils.setField(suggestion, "id", id);
        ReflectionTestUtils.setField(suggestion, "createdAt", createdAt);
        return suggestion;
    }

    private Cv cv(Long id) {
        UserAccount owner = new UserAccount("alice@example.com", "Alice Student", "hash", false);
        ReflectionTestUtils.setField(owner, "id", 2L);
        Cv cv = new Cv(owner, "Alice CV", "uploads/alice.html");
        ReflectionTestUtils.setField(cv, "id", id);
        return cv;
    }
}
