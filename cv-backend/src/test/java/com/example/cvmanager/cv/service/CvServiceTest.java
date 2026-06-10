package com.example.cvmanager.cv.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.common.exception.NotFoundException;
import com.example.cvmanager.cv.dto.CvCreateRequest;
import com.example.cvmanager.cv.dto.CvUpdateRequest;
import com.example.cvmanager.cv.mapper.CvMapper;
import com.example.cvmanager.cv.model.Cv;
import com.example.cvmanager.cv.repository.CvRepository;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class CvServiceTest {

    @TempDir
    Path tempDir;

    private final CvRepository cvRepository = mock(CvRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private CvService service;

    @BeforeEach
    void setUp() {
        service = new CvService(
                cvRepository,
                userRepository,
                new CvMapper(),
                new CvStorageProperties(tempDir.toString()));
    }

    @Test
    void listGetCreateAndUpdateCvsMapRepositoryEntities() {
        UserAccount owner = user(2L, "alice@example.com");
        Cv cv = cv(10L, owner, "Alice CV", "uploads/alice.html");
        when(cvRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))).thenReturn(List.of(cv));
        when(cvRepository.findById(10L)).thenReturn(Optional.of(cv));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(cvRepository.save(any(Cv.class))).thenAnswer(invocation -> {
            Cv saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            return saved;
        });

        assertThat(service.listCvs()).extracting("title").containsExactly("Alice CV");
        assertThat(service.getCv(10L).ownerEmail()).isEqualTo("alice@example.com");
        assertThat(service.createCv(new CvCreateRequest(2L, "Created CV", "uploads/created.html")).id())
                .isEqualTo(11L);

        var updated = service.updateCv(10L, new CvUpdateRequest("Updated CV", "uploads/updated.html"));

        assertThat(updated.title()).isEqualTo("Updated CV");
        assertThat(cv.getUploadedHtmlFilePath()).isEqualTo("uploads/updated.html");
    }

    @Test
    void missingCvOrOwnerRaiseNotFoundCodes() {
        when(cvRepository.findById(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCv(99L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("CV_NOT_FOUND"));

        assertThatThrownBy(() -> service.createCv(new CvCreateRequest(99L, "Missing Owner", "uploads/missing.html")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("USER_NOT_FOUND"));
    }

    @Test
    void searchUsesListForBlankQueryAndRepositorySearchForNonBlankQuery() {
        UserAccount owner = user(2L, "alice@example.com");
        Cv cv = cv(10L, owner, "Alice CV", "uploads/alice.html");
        when(cvRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))).thenReturn(List.of(cv));
        when(cvRepository.search("A_%'")).thenReturn(List.of(cv));

        assertThat(service.searchCvs(" ")).extracting("title").containsExactly("Alice CV");
        assertThat(service.searchCvs("A_%'")).extracting("title").containsExactly("Alice CV");

        verify(cvRepository).search("A_%'");
    }

    @Test
    void uploadHtmlCvValidatesRequiredInputsAndFileType() {
        assertThatThrownBy(() -> service.uploadHtmlCv(null, "Title", htmlFile("cv.html", "<html></html>")))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("OWNER_REQUIRED"));

        assertThatThrownBy(() -> service.uploadHtmlCv(2L, "Title", null))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("CV_FILE_REQUIRED"));

        assertThatThrownBy(() -> service.uploadHtmlCv(2L, "Title",
                        new MockMultipartFile("file", "cv.txt", "text/plain", "text".getBytes(StandardCharsets.UTF_8))))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("CV_FILE_TYPE"));
    }

    @Test
    void uploadHtmlCvStoresFileWithDerivedAndFallbackTitles() throws Exception {
        UserAccount owner = user(2L, "alice@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(cvRepository.save(any(Cv.class))).thenAnswer(invocation -> {
            Cv saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 20L);
            return saved;
        });

        var derived = service.uploadHtmlCv(2L, null,
                htmlFile("../Alice CV.html", "<html><head><title>Derived Title</title></head><body></body></html>"));
        var fromOriginal = service.uploadHtmlCv(2L, "", htmlFile("fallback.htm", "<html>No title</html>"));
        var defaultTitle = service.uploadHtmlCv(2L, " ", new MockMultipartFile(
                "file",
                null,
                "text/html",
                "<html>No filename</html>".getBytes(StandardCharsets.UTF_8)));

        assertThat(derived.title()).isEqualTo("Derived Title");
        assertThat(derived.uploadedHtmlFilePath()).contains("Alice_CV.html");
        assertThat(Files.readString(Path.of(derived.uploadedHtmlFilePath()))).contains("Derived Title");
        assertThat(fromOriginal.title()).isEqualTo("fallback.htm");
        assertThat(defaultTitle.title()).isEqualTo("Uploaded CV");
        assertThat(defaultTitle.uploadedHtmlFilePath()).endsWith("cv.html");
    }

    @Test
    void uploadHtmlCvReportsMissingOwnerAndStorageFailure() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.uploadHtmlCv(99L, "Title", htmlFile("cv.html", "<html></html>")))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("USER_NOT_FOUND"));

        Path fileInsteadOfDirectory = tempDir.resolve("not-a-directory");
        Files.writeString(fileInsteadOfDirectory, "blocking file");
        CvService failingService = new CvService(
                cvRepository,
                userRepository,
                new CvMapper(),
                new CvStorageProperties(fileInsteadOfDirectory.toString()));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "alice@example.com")));

        assertThatThrownBy(() -> failingService.uploadHtmlCv(2L, "Title", htmlFile("cv.html", "<html></html>")))
                .isInstanceOfSatisfying(BadRequestException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("CV_UPLOAD_FAILED"));
    }

    @Test
    void getUploadedHtmlReadsFileAndReportsMissingFile() throws Exception {
        UserAccount owner = user(2L, "alice@example.com");
        Path file = tempDir.resolve("alice.html");
        Files.writeString(file, "<html>Alice</html>");
        when(cvRepository.findById(10L)).thenReturn(Optional.of(cv(10L, owner, "Alice", file.toString())));
        when(cvRepository.findById(11L)).thenReturn(Optional.of(cv(11L, owner, "Missing", tempDir.resolve("missing.html").toString())));

        assertThat(service.getUploadedHtml(10L)).isEqualTo("<html>Alice</html>");

        assertThatThrownBy(() -> service.getUploadedHtml(11L))
                .isInstanceOfSatisfying(NotFoundException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("CV_FILE_NOT_FOUND"));
    }

    @Test
    void legacyPreviewNormalizesHeadersAndBlankContent() {
        assertThat(service.buildLegacyPreview("CV:\r\nAda Lovelace\nMath"))
                .isEqualTo("<html><body><h1>Ada Lovelace</h1><div>Ada Lovelace<br>Math</div></body></html>");

        assertThat(service.buildLegacyPreview(null))
                .isEqualTo("<html><body><h1>Legacy CV</h1><div></div></body></html>");
    }

    private MockMultipartFile htmlFile(String originalFilename, String html) {
        return new MockMultipartFile(
                "file",
                originalFilename,
                "text/html",
                html.getBytes(StandardCharsets.UTF_8));
    }

    private UserAccount user(Long id, String email) {
        UserAccount user = new UserAccount(email, "User", "password", false);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Cv cv(Long id, UserAccount owner, String title, String path) {
        Cv cv = new Cv(owner, title, path);
        ReflectionTestUtils.setField(cv, "id", id);
        return cv;
    }
}
