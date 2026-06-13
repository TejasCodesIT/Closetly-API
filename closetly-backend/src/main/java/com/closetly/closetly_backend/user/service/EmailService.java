package com.closetly.closetly_backend.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

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

    public void sendOrderCancelledByCustomerEmail(
            String sellerEmail,
            String productTitle,
            String customerName,
            String orderId,
            String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(sellerEmail);
        message.setSubject("Order cancelled by customer: " + productTitle);
        message.setText("""
                An order has been cancelled by the customer.

                Order ID: %s
                Product: %s
                Customer: %s
                Reason: %s
                """.formatted(
                safe(orderId),
                safe(productTitle),
                safe(customerName),
                safe(reason)));
        mailSender.send(message);
    }

    public void sendOrderCancelledBySellerEmail(
            String customerEmail,
            String productTitle,
            String sellerName,
            String orderId,
            String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customerEmail);
        message.setSubject("Order cancelled by seller: " + productTitle);
        message.setText("""
                Your order has been cancelled by the seller.

                Order ID: %s
                Product: %s
                Seller: %s
                Reason: %s
                """.formatted(
                safe(orderId),
                safe(productTitle),
                safe(sellerName),
                safe(reason)));
        mailSender.send(message);
    }

    public void sendBookingCancelledByCustomerEmail(
            String ownerEmail,
            String productTitle,
            String customerName,
            LocalDate startDate,
            LocalDate endDate,
            String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(ownerEmail);
        message.setSubject("Booking cancelled by customer: " + productTitle);
        message.setText("""
                A booking has been cancelled by the customer.

                Product: %s
                Customer: %s
                Dates: %s to %s
                Reason: %s
                """.formatted(
                safe(productTitle),
                safe(customerName),
                safe(startDate),
                safe(endDate),
                safe(reason)));
        mailSender.send(message);
    }

    public void sendBookingCancelledBySellerEmail(
            String customerEmail,
            String productTitle,
            String sellerName,
            LocalDate startDate,
            LocalDate endDate,
            String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(customerEmail);
        message.setSubject("Booking cancelled by seller: " + productTitle);
        message.setText("""
                Your booking has been cancelled by the seller.

                Product: %s
                Seller: %s
                Dates: %s to %s
                Reason: %s
                """.formatted(
                safe(productTitle),
                safe(sellerName),
                safe(startDate),
                safe(endDate),
                safe(reason)));
        mailSender.send(message);
    }

    // public void sendEmailVerificationEmail(String toEmail, String
    // verificationToken) {
    // SimpleMailMessage message = new SimpleMailMessage();
    // message.setTo(toEmail);
    // message.setSubject("Verify your Closetly email");
    // message.setText("Verify your email by clicking this link:\n\n" +
    // "http://localhost:8080/auth/verify?token=" + verificationToken + "\n\n" +
    // "If you didn't create this account, you can ignore this email.");
    // mailSender.send(message);
    // }

    public void sendEmailVerificationEmail(String toEmail, String verificationToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verify your Closetly email");

        String verificationUrl = baseUrl + "/api/auth/verify?token=" + verificationToken;

        message.setText(
                "Verify your email by clicking this link:\n\n" +
                        verificationUrl +
                        "\n\nIf you didn't create this account, you can ignore this email.");

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