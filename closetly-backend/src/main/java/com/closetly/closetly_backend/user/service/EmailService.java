package com.closetly.closetly_backend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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
}