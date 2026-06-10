package com.example.cvmanager.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.cvmanager.ai.model.AiActionType;
import com.example.cvmanager.ai.model.AiSuggestion;
import com.example.cvmanager.ai.repository.AiSuggestionRepository;
import com.example.cvmanager.cv.model.Cv;
import com.example.cvmanager.cv.service.CvService;
import com.example.cvmanager.user.model.UserAccount;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AiApplicationServiceTest {

    private final CvService cvService = mock(CvService.class);
    private final AiService aiService = mock(AiService.class);
    private final AiSuggestionRepository repository = mock(AiSuggestionRepository.class);
    private final AiApplicationService service = new AiApplicationService(cvService, aiService, repository);

    @Test
    void improveSummaryReadsCvHtmlAndPersistsSuggestion() {
        Cv cv = cv(10L);
        when(cvService.findCv(10L)).thenReturn(cv);
        when(cvService.getUploadedHtml(10L)).thenReturn("<html>summary</html>");
        when(aiService.suggest(AiActionType.IMPROVE_SUMMARY, "<html>summary</html>")).thenReturn("better summary");
        when(repository.save(any(AiSuggestion.class))).thenAnswer(invocation -> {
            AiSuggestion suggestion = invocation.getArgument(0);
            ReflectionTestUtils.setField(suggestion, "id", 5L);
            return suggestion;
        });

        var response = service.improveSummary(10L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.cvId()).isEqualTo(10L);
        assertThat(response.originalText()).isEqualTo("<html>summary</html>");
        assertThat(response.suggestedText()).isEqualTo("better summary");
        assertThat(response.status()).isEqualTo("PENDING");
        verify(repository).save(any(AiSuggestion.class));
    }

    @Test
    void listSuggestionsMapsSuggestionsForCv() {
        AiSuggestion suggestion = new AiSuggestion(cv(10L), AiActionType.IMPROVE_SUMMARY, "before", "after", "PENDING");
        ReflectionTestUtils.setField(suggestion, "id", 5L);
        when(repository.findByCvIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(suggestion));

        var suggestions = service.listSuggestions(10L);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).actionType()).isEqualTo(AiActionType.IMPROVE_SUMMARY);
    }

    @Test
    void mockAiServiceReturnsReadableSuggestion() {
        assertThat(new MockAiService().suggest(AiActionType.IMPROVE_WORK_EXPERIENCE, "input"))
                .isEqualTo("Mock improve work experience suggestion for content from: input");
    }

    private Cv cv(Long id) {
        UserAccount owner = new UserAccount("alice@example.com", "Alice", "password", false);
        ReflectionTestUtils.setField(owner, "id", 2L);
        Cv cv = new Cv(owner, "Alice CV", "uploads/alice.html");
        ReflectionTestUtils.setField(cv, "id", id);
        return cv;
    }
}
