package com.closetly.closetly_backend.support.service;

import com.closetly.closetly_backend.support.dto.SupportRequestDTO;
import com.closetly.closetly_backend.support.entity.SupportRequest;
import com.closetly.closetly_backend.support.repository.SupportRequestRepository;
import com.closetly.closetly_backend.support.status.SupportRequestStatus;
import com.closetly.closetly_backend.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final EmailService emailService;

    @Value("${support.admin.email:support@closetly.dev}")
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
            String subject = "New Support Request: " + supportRequest.getSubject();
            String htmlBody = "<p>A new support request has been submitted.</p>" +
                    "<p><strong>Name:</strong> " + escapeHtml(safe(supportRequest.getName())) + "</p>" +
                    "<p><strong>Email:</strong> " + escapeHtml(safe(supportRequest.getEmail())) + "</p>" +
                    "<p><strong>Issue Type:</strong> " + escapeHtml(safe(supportRequest.getIssueType())) + "</p>" +
                    "<p><strong>Subject:</strong> " + escapeHtml(safe(supportRequest.getSubject())) + "</p>" +
                    "<p><strong>Message:</strong></p>" +
                    "<pre style=\"font-family:inherit;white-space:pre-wrap;\">"
                    + escapeHtml(safe(supportRequest.getMessage())) + "</pre>" +
                    "<p><strong>Submitted at:</strong> " + escapeHtml(String.valueOf(supportRequest.getCreatedAt()))
                    + "</p>";
            emailService.sendEmail(adminEmail, subject, htmlBody);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to send support request email. Please try again later.", ex);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not specified" : value;
    }

    private static String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
