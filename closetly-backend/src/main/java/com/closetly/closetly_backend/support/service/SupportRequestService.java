package com.closetly.closetly_backend.support.service;

import com.closetly.closetly_backend.support.dto.SupportRequestDTO;
import com.closetly.closetly_backend.support.entity.SupportRequest;
import com.closetly.closetly_backend.support.repository.SupportRequestRepository;
import com.closetly.closetly_backend.support.status.SupportRequestStatus;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final JavaMailSender javaMailSender;

    @Value("${support.admin.email:${spring.mail.username}}")
    private String adminEmail;

    public SupportRequest saveSupportRequest(SupportRequestDTO request) {
        SupportRequest supportRequest = SupportRequest.builder()
                .name(request.getName())
                .email(request.getEmail())
                .subject(request.getSubject())
                .message(request.getMessage())
                .issueType(request.getIssueType())
                .status(SupportRequestStatus.OPEN)
                .build();

        SupportRequest savedRequest = supportRequestRepository.save(supportRequest);
        sendEmailToAdmin(savedRequest);
        return savedRequest;
    }

    public Page<SupportRequest> getAllSupportRequests(Pageable pageable) {
        return supportRequestRepository.findAll(pageable);
    }

    public void updateSupportRequestStatus(Long requestId, SupportRequestStatus status) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Support request not found"));
        request.setStatus(status);
        supportRequestRepository.save(request);
    }

    public void sendEmailToAdmin(SupportRequest supportRequest) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(adminEmail);
            message.setSubject("New Support Request: " + supportRequest.getSubject());
            message.setText("A new support request has been submitted.\n\n" +
                    "Name: " + supportRequest.getName() + "\n" +
                    "Email: " + supportRequest.getEmail() + "\n" +
                    "Issue Type: " + safe(supportRequest.getIssueType()) + "\n" +
                    "Subject: " + supportRequest.getSubject() + "\n\n" +
                    "Message:\n" + supportRequest.getMessage() + "\n\n" +
                    "Submitted at: " + supportRequest.getCreatedAt());
            javaMailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to send support request email. Please try again later.", ex);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not specified" : value;
    }
}
