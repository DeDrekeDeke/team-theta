package com.example.cvmanager.cv.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.common.exception.NotFoundException;
import com.example.cvmanager.cv.mapper.CvMapper;
import com.example.cvmanager.cv.model.Cv;
import com.example.cvmanager.cv.repository.CvRepository;
import com.example.cvmanager.cv.dto.CvCreateRequest;
import com.example.cvmanager.cv.dto.CvUpdateRequest;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class CvServiceTest {

    @TempDir
    private Path tempDir;

    private CvRepository cvRepository;
    private UserRepository userRepository;
    private CvService cvService;

    @BeforeEach
    void setUp() {
        cvRepository = org.mockito.Mockito.mock(CvRepository.class);
        userRepository = org.mockito.Mockito.mock(UserRepository.class);
        cvService = new CvService(
                cvRepository,
                userRepository,
                new CvMapper(),
                new CvStorageProperties(tempDir.toString()));
    }

    @Test
    void createCvPersistsCvForExistingOwner() {
        UserAccount owner = user(2L, "alice@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(cvRepository.save(any(Cv.class))).thenAnswer(invocation -> {
            Cv saved = invocation.getArgument(0);
            setCvId(saved, 10L);
            return saved;
        });

        var response = cvService.createCv(new CvCreateRequest(
                2L,
                "Alice CV",
                "uploads/alice.html"));

        assertEquals(10L, response.id());
        assertEquals(2L, response.ownerUserId());
        assertEquals("alice@example.com", response.ownerEmail());
        assertEquals("Alice CV", response.title());
        assertEquals("uploads/alice.html", response.uploadedHtmlFilePath());
    }

    @Test
    void createCvRejectsMissingOwner() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> cvService.createCv(new CvCreateRequest(99L, "Missing Owner CV", "uploads/missing.html")));

        assertEquals("USER_NOT_FOUND", exception.getCode());
        verifyNoInteractions(cvRepository);
    }

    @Test
    void updateCvChangesEditableFields() {
        UserAccount owner = user(2L, "alice@example.com");
        Cv cv = cv(7L, owner, "Old CV", "uploads/old.html");
        when(cvRepository.findByIdAndArchivedAtIsNull(7L)).thenReturn(Optional.of(cv));
        when(cvRepository.save(cv)).thenReturn(cv);

        var response = cvService.updateCv(7L, new CvUpdateRequest("Updated CV", "uploads/updated.html"));

        assertEquals("Updated CV", response.title());
        assertEquals("uploads/updated.html", response.uploadedHtmlFilePath());
        assertEquals("Updated CV", cv.getTitle());
        assertEquals("uploads/updated.html", cv.getUploadedHtmlFilePath());
    }

    @Test
    void archiveCvMarksCvArchivedAndSavesIt() {
        Cv cv = cv(8L, user(2L, "alice@example.com"), "Alice CV", "uploads/alice.html");
        when(cvRepository.findByIdAndArchivedAtIsNull(8L)).thenReturn(Optional.of(cv));

        cvService.archiveCv(8L);

        assertTrue(cv.isArchived());
        verify(cvRepository).save(cv);
    }

    @Test
    void searchWithBlankQueryFallsBackToActiveCvList() {
        Cv cv = cv(1L, user(2L, "alice@example.com"), "Alice CV", "uploads/alice.html");
        when(cvRepository.findByArchivedAtIsNull(any(Sort.class))).thenReturn(List.of(cv));

        var response = cvService.searchCvs("  ");

        assertEquals(1, response.size());
        assertEquals("Alice CV", response.get(0).title());
        verify(cvRepository, never()).search(any());
    }

    @Test
    void searchTrimsSpecialCharacterQueryBeforeRepositoryCall() {
        Cv cv = cv(3L, user(2L, "alice@example.com"), "O'Connor %_;--", "uploads/special.html");
        when(cvRepository.search("O'Connor %_;--")).thenReturn(List.of(cv));

        var response = cvService.searchCvs("  O'Connor %_;--  ");

        assertEquals(1, response.size());
        assertEquals("O'Connor %_;--", response.get(0).title());
        verify(cvRepository).search("O'Connor %_;--");
    }

    @Test
    void uploadHtmlCvExtractsTitleSanitizesFilenameAndStoresContent() throws Exception {
        UserAccount owner = user(5L, "alice@example.com");
        when(userRepository.findById(5L)).thenReturn(Optional.of(owner));
        when(cvRepository.save(any(Cv.class))).thenAnswer(invocation -> {
            Cv saved = invocation.getArgument(0);
            setCvId(saved, 12L);
            return saved;
        });
        String html = "<html><head><title>Alice HTML CV</title></head><body>Skills</body></html>";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../bad name!.html",
                "text/html",
                html.getBytes(UTF_8));

        var response = cvService.uploadHtmlCv(5L, " ", file);

        assertEquals(12L, response.id());
        assertEquals("Alice HTML CV", response.title());
        assertTrue(response.uploadedHtmlFilePath().endsWith("-bad_name_.html"));
        assertEquals(html, Files.readString(Path.of(response.uploadedHtmlFilePath()), UTF_8));
    }

    @Test
    void uploadHtmlCvRejectsMissingOwnerUserId() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> cvService.uploadHtmlCv(null, "Title", htmlFile("cv.html", "<html></html>")));

        assertEquals("OWNER_REQUIRED", exception.getCode());
        verifyNoInteractions(userRepository, cvRepository);
    }

    @Test
    void uploadHtmlCvRejectsNonHtmlFileBeforeOwnerLookup() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cv.pdf",
                "application/pdf",
                "not html".getBytes(UTF_8));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> cvService.uploadHtmlCv(5L, "Title", file));

        assertEquals("CV_FILE_TYPE", exception.getCode());
        verifyNoInteractions(userRepository, cvRepository);
    }

    @Test
    void getUploadedHtmlReadsStoredFile() throws Exception {
        Path file = tempDir.resolve("alice.html");
        Files.writeString(file, "<html>Alice</html>", UTF_8);
        Cv cv = cv(4L, user(2L, "alice@example.com"), "Alice CV", file.toString());
        when(cvRepository.findByIdAndArchivedAtIsNull(4L)).thenReturn(Optional.of(cv));

        assertEquals("<html>Alice</html>", cvService.getUploadedHtml(4L));
    }

    @Test
    void getUploadedHtmlRejectsMissingFile() {
        Path file = tempDir.resolve("missing.html");
        Cv cv = cv(4L, user(2L, "alice@example.com"), "Alice CV", file.toString());
        when(cvRepository.findByIdAndArchivedAtIsNull(4L)).thenReturn(Optional.of(cv));

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> cvService.getUploadedHtml(4L));

        assertEquals("CV_FILE_NOT_FOUND", exception.getCode());
    }

    @Test
    void findCvRejectsArchivedOrMissingCv() {
        when(cvRepository.findByIdAndArchivedAtIsNull(404L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> cvService.findCv(404L));

        assertEquals("CV_NOT_FOUND", exception.getCode());
    }

    @Test
    void buildLegacyPreviewNormalizesHeaderAndLineBreaks() {
        String html = cvService.buildLegacyPreview("CV:\r\nDana Candidate\r\nJava\r\nSpring");

        assertTrue(html.contains("<h1>Dana Candidate</h1>"));
        assertTrue(html.contains("Java<br>Spring"));
    }

    private MockMultipartFile htmlFile(String filename, String content) {
        return new MockMultipartFile("file", filename, "text/html", content.getBytes(UTF_8));
    }

    private UserAccount user(Long id, String email) {
        UserAccount user = new UserAccount(email, "Test User", "hash", false);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Cv cv(Long id, UserAccount owner, String title, String path) {
        Cv cv = new Cv(owner, title, path);
        setCvId(cv, id);
        return cv;
    }

    private void setCvId(Cv cv, Long id) {
        ReflectionTestUtils.setField(cv, "id", id);
        ReflectionTestUtils.setField(cv, "createdAt", LocalDateTime.of(2026, 6, 10, 12, 0));
        ReflectionTestUtils.setField(cv, "updatedAt", LocalDateTime.of(2026, 6, 10, 12, 30));
    }
}
