package com.example.cvmanager.cv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.cvmanager.common.exception.BadRequestException;
import com.example.cvmanager.cv.mapper.CvMapper;
import com.example.cvmanager.cv.repository.CvRepository;
import com.example.cvmanager.user.model.UserAccount;
import com.example.cvmanager.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CvServiceTest {

    private CvRepository cvRepository;
    private UserRepository userRepository;
    private CvService cvService;

    @BeforeEach
    void setUp() {
        cvRepository = mock(CvRepository.class);
        userRepository = mock(UserRepository.class);
        cvService = new CvService(
                cvRepository,
                userRepository,
                mock(CvMapper.class),
                new CvStorageProperties("build/test-uploads"));
    }

    @Test
    void uploadHtmlCvRejectsNonPositiveOwnerId() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> cvService.uploadHtmlCv(0L, "CV", null));

        assertEquals("OWNER_INVALID", exception.getCode());
        verifyNoInteractions(userRepository, cvRepository);
    }

    @Test
    void uploadHtmlCvRejectsUnsafeHtmlAttributes() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(new UserAccount("alice@example.com", "Alice Student", "hash", false)));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "alice-cv.html",
                "text/html",
                "<html><body><img src=\"x\" onerror=\"alert(1)\"></body></html>".getBytes(StandardCharsets.UTF_8));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> cvService.uploadHtmlCv(1L, "Alice CV", file));

        assertEquals("CV_FILE_UNSAFE", exception.getCode());
        verifyNoInteractions(cvRepository);
    }
}
