package com.closetly.closetly_backend.user.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;

@Service
public class EmailService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient httpClient = new OkHttpClient();

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @PostConstruct
    public void test() {
        System.out.println("BASE URL = " + baseUrl);
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Recipient email address is required");
        }
        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException("Resend API key is not configured");
        }

        String payload = "{" +
                "\"from\":\"onboarding@resend.dev\"," +
                "\"to\":[\"" + escapeJson(to) + "\"]," +
                "\"subject\":\"" + escapeJson(subject) + "\"," +
                "\"html\":\"" + escapeJson(htmlContent) + "\"" +
                "}";

        RequestBody body = RequestBody.create(payload, JSON);
        Request request = new Request.Builder()
                .url("https://api.resend.com/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                throw new RuntimeException(
                        "Resend API request failed with status " + response.code() + ": " + responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to send email through Resend API", e);
        }
    }

    public void sendVerificationEmail(String email, String verificationUrl) {
        String subject = "Verify your Closetly email";
        String htmlBody = "<p>Verify your email by clicking the link below:</p>" +
                "<p><a href=\"" + escapeHtml(verificationUrl) + "\">" + escapeHtml(verificationUrl) + "</a></p>";
        sendEmail(email, subject, htmlBody);
    }

    public void sendPasswordResetEmail(String email, String resetUrl) {
        String subject = "Closetly Password Reset";
        String htmlBody = "<p>Click the link below to reset your password:</p>" +
                "<p><a href=\"" + escapeHtml(resetUrl) + "\">" + escapeHtml(resetUrl) + "</a></p>" +
                "<p>This link will expire in 15 minutes.</p>" +
                "<p>If you didn't request this password reset, please ignore this email.</p>";
        sendEmail(email, subject, htmlBody);
    }

    public void sendBookingCreatedEmail(
            String ownerEmail,
            String productTitle,
            String customerName,
            LocalDate startDate,
            LocalDate endDate,
            String customerMessage) {
        String subject = "New booking request: " + safe(productTitle);
        String text = "You have received a new booking request." +
                "\n\nProduct: " + safe(productTitle) +
                "\nCustomer: " + safe(customerName) +
                "\nDates: " + safe(startDate) + " to " + safe(endDate) +
                "\n\nMessage:\n" + safeMultiline(customerMessage);
        sendEmail(ownerEmail, subject, wrapHtml(text));
    }

    public void sendBookingApprovedEmail(
            String customerEmail,
            String productTitle,
            LocalDate startDate,
            LocalDate endDate) {
        String subject = "Booking approved: " + safe(productTitle);
        String text = "Your booking request has been approved." +
                "\n\nProduct: " + safe(productTitle) +
                "\nDates: " + safe(startDate) + " to " + safe(endDate);
        sendEmail(customerEmail, subject, wrapHtml(text));
    }

    public void sendBookingRejectedEmail(
            String customerEmail,
            String productTitle,
            LocalDate startDate,
            LocalDate endDate) {
        String subject = "Booking rejected: " + safe(productTitle);
        String text = "Your booking request has been rejected." +
                "\n\nProduct: " + safe(productTitle) +
                "\nDates: " + safe(startDate) + " to " + safe(endDate);
        sendEmail(customerEmail, subject, wrapHtml(text));
    }

    public void sendOrderCancelledByCustomerEmail(
            String sellerEmail,
            String productTitle,
            String customerName,
            String orderId,
            String reason) {
        String subject = "Order cancelled by customer: " + safe(productTitle);
        String text = "An order has been cancelled by the customer." +
                "\n\nOrder ID: " + safe(orderId) +
                "\nProduct: " + safe(productTitle) +
                "\nCustomer: " + safe(customerName) +
                "\nReason: " + safe(reason);
        sendEmail(sellerEmail, subject, wrapHtml(text));
    }

    public void sendOrderCancelledBySellerEmail(
            String customerEmail,
            String productTitle,
            String sellerName,
            String orderId,
            String reason) {
        String subject = "Order cancelled by seller: " + safe(productTitle);
        String text = "Your order has been cancelled by the seller." +
                "\n\nOrder ID: " + safe(orderId) +
                "\nProduct: " + safe(productTitle) +
                "\nSeller: " + safe(sellerName) +
                "\nReason: " + safe(reason);
        sendEmail(customerEmail, subject, wrapHtml(text));
    }

    public void sendBookingCancelledByCustomerEmail(
            String ownerEmail,
            String productTitle,
            String customerName,
            LocalDate startDate,
            LocalDate endDate,
            String reason) {
        String subject = "Booking cancelled by customer: " + safe(productTitle);
        String text = "A booking has been cancelled by the customer." +
                "\n\nProduct: " + safe(productTitle) +
                "\nCustomer: " + safe(customerName) +
                "\nDates: " + safe(startDate) + " to " + safe(endDate) +
                "\nReason: " + safe(reason);
        sendEmail(ownerEmail, subject, wrapHtml(text));
    }

    public void sendBookingCancelledBySellerEmail(
            String customerEmail,
            String productTitle,
            String sellerName,
            LocalDate startDate,
            LocalDate endDate,
            String reason) {
        String subject = "Booking cancelled by seller: " + safe(productTitle);
        String text = "Your booking has been cancelled by the seller." +
                "\n\nProduct: " + safe(productTitle) +
                "\nSeller: " + safe(sellerName) +
                "\nDates: " + safe(startDate) + " to " + safe(endDate) +
                "\nReason: " + safe(reason);
        sendEmail(customerEmail, subject, wrapHtml(text));
    }

    public void sendEmailVerificationEmail(String toEmail, String verificationToken) {
        try {
            String verificationUrl = baseUrl + "/api/auth/verify?token=" + verificationToken;
            sendVerificationEmail(toEmail, verificationUrl);
        } catch (Exception e) {
            System.err.println("EMAIL ERROR: " + e.getMessage());
            throw new RuntimeException("Unable to send email. Please try again later.", e);
        }
    }

    private static String wrapHtml(String text) {
        return "<html><body><pre style=\"font-family:inherit;white-space:pre-wrap;\">" + escapeHtml(text)
                + "</pre></body></html>";
    }

    private static String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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
