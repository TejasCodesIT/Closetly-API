package com.closetly.closetly_backend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Closetly Password Reset");
            message.setText("Click the link below to reset your password:\n\n" +
                    "http://localhost:4200/reset-password?token=" + resetToken + "\n\n" +
                    "This link will expire in 15 minutes.\n\n" +
                    "If you didn't request this password reset, please ignore this email.");

            mailSender.send(message);
            System.out.println("Password reset email sent successfully to: " + toEmail);
        } catch (MailAuthenticationException e) {
            System.err.println("Email authentication failed: " + e.getMessage());
            throw new RuntimeException("Email authentication failed. Please check SMTP credentials.");
        } catch (MailSendException e) {
            System.err.println("Failed to send email: " + e.getMessage());
            throw new RuntimeException("Failed to send email. Please try again later.");
        } catch (Exception e) {
            System.err.println("Unexpected error sending email: " + e.getMessage());
            throw new RuntimeException("Unable to send email. Please try again later.");
        }
    }

    public void sendBookingCreatedEmail(
            String ownerEmail,
            String productTitle,
            String customerName,
            LocalDate startDate,
            LocalDate endDate,
            String customerMessage) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(ownerEmail);
        message.setSubject("New booking request: " + productTitle);
        message.setText("""
                You have received a new booking request.

                Product: %s
                Customer: %s
                Dates: %s to %s

                Message:
                %s
                """.formatted(
                safe(productTitle),
                safe(customerName),
                safe(startDate),
                safe(endDate),
                safeMultiline(customerMessage)));
        mailSender.send(message);
    }

    public void sendBookingApprovedEmail(
            String customerEmail,
            String productTitle,
            LocalDate startDate,
            LocalDate endDate) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customerEmail);
        message.setSubject("Booking approved: " + productTitle);
        message.setText("""
                Your booking request has been approved.

                Product: %s
                Dates: %s to %s
                """.formatted(safe(productTitle), safe(startDate), safe(endDate)));
        mailSender.send(message);
    }

    public void sendBookingRejectedEmail(
            String customerEmail,
            String productTitle,
            LocalDate startDate,
            LocalDate endDate) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customerEmail);
        message.setSubject("Booking rejected: " + productTitle);
        message.setText("""
                Your booking request has been rejected.

                Product: %s
                Dates: %s to %s
                """.formatted(safe(productTitle), safe(startDate), safe(endDate)));
        mailSender.send(message);
    }

    public void sendEmailVerificationEmail(String toEmail, String verificationToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify your Closetly email");
        message.setText("Verify your email by clicking this link:\n\n" +
                "http://localhost:8080/api/auth/verify?token=" + verificationToken + "\n\n" +
                "If you didn't create this account, you can ignore this email.");
        mailSender.send(message);
    }

    private static String safe(Object val) {
        return val == null ? "-" : val.toString();
    }

    private static String safeMultiline(String val) {
        if (val == null || val.trim().isEmpty()) {
            return "-";
        }
        return val;
    }
}