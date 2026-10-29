package com.infotech.lang.application.data.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service for sending emails using SendGrid.
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name}")
    private String fromName;

    @Value("${app.base.url}")
    private String appBaseUrl;

    /**
     * Send a password reset email with token link.
     */
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        String resetLink = appBaseUrl + "/reset-password?token=" + resetToken;
        
        String subject = "Password Reset Request - LanguageTool";
        String htmlContent = buildPasswordResetEmailHtml(userName, resetLink);
        
        sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Send an email verification email with token link.
     */
    public void sendEmailVerificationEmail(String toEmail, String userName, String verificationToken) {
        String verificationLink = appBaseUrl + "/verify-email?token=" + verificationToken;
        
        String subject = "Verify Your Email - LanguageTool";
        String htmlContent = buildEmailVerificationHtml(userName, verificationLink);
        
        sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Send a password change confirmation email.
     */
    public void sendPasswordChangeConfirmationEmail(String toEmail, String userName) {
        String subject = "Password Changed Successfully - LanguageTool";
        String htmlContent = buildPasswordChangeConfirmationHtml(userName);
        
        sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Core method to send email using SendGrid.
     */
    private void sendEmail(String toEmail, String subject, String htmlContent) {
        Email from = new Email(fromEmail, fromName);
        Email to = new Email(toEmail);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();
        
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("Email sent successfully to: {}", toEmail);
            } else {
                logger.error("Failed to send email. Status: {}, Body: {}", 
                    response.getStatusCode(), response.getBody());
            }
        } catch (IOException e) {
            logger.error("Error sending email to: " + toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Build HTML content for password reset email.
     */
    private String buildPasswordResetEmailHtml(String userName, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; 
                             color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { font-size: 12px; color: #666; margin-top: 20px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>LanguageTool</h1>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>We received a request to reset your password. Click the button below to reset it:</p>
                        <p><a href="%s" class="button">Reset Password</a></p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p><a href="%s">%s</a></p>
                        <p><strong>This link will expire in 24 hours.</strong></p>
                        <p>If you didn't request a password reset, please ignore this email or contact support if you have concerns.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 LanguageTool. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, resetLink, resetLink, resetLink);
    }

    /**
     * Build HTML content for email verification email.
     */
    private String buildEmailVerificationHtml(String userName, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #2196F3; 
                             color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { font-size: 12px; color: #666; margin-top: 20px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to LanguageTool!</h1>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>Thanks for signing up! Please verify your email address to activate your account:</p>
                        <p><a href="%s" class="button">Verify Email</a></p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p><a href="%s">%s</a></p>
                        <p><strong>This link will expire in 24 hours.</strong></p>
                        <p>If you didn't create this account, please ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 LanguageTool. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, verificationLink, verificationLink, verificationLink);
    }

    /**
     * Build HTML content for password change confirmation email.
     */
    private String buildPasswordChangeConfirmationHtml(String userName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; background-color: #f9f9f9; }
                    .footer { font-size: 12px; color: #666; margin-top: 20px; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Changed</h1>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>Your password has been successfully changed.</p>
                        <p>If you did not make this change, please contact our support team immediately.</p>
                    </div>
                    <div class="footer">
                        <p>&copy; 2026 LanguageTool. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName);
    }
}
