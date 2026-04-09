package org.tikcoin.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendQrCode(String toEmail, String buyerDisplayName, long coinAmount, String qrCodeBase64) {
        if (mailSender == null || fromAddress == null || fromAddress.isBlank()) {
            logger.warn("Email not configured — skipping QR code email to {}", toEmail);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            logger.warn("Buyer has no email address — skipping QR code email");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Your TikCoin QR Code — Scan to Receive " + coinAmount + " Coins");

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto;">
                      <h2 style="color: #1a1a2e;">Your TikCoin Order is Being Processed</h2>
                      <p>Hi <strong>%s</strong>,</p>
                      <p>Your payment was successful! Scan the QR code below with your <strong>TikTok app</strong>
                         to receive your <strong>%d TikCoins</strong>.</p>
                      <div style="text-align: center; margin: 24px 0;">
                        <img src="data:image/png;base64,%s"
                             alt="TikTok QR Code"
                             style="width: 240px; height: 240px; border: 2px solid #eee; border-radius: 8px;" />
                      </div>
                      <p style="color: #666; font-size: 13px;">
                        Once scanned, your coins will be delivered to your TikTok account.
                        You will receive a confirmation notification shortly after.
                      </p>
                      <p style="color: #999; font-size: 12px;">— The TikCoin Team</p>
                    </div>
                    """.formatted(buyerDisplayName != null ? buyerDisplayName : "there", coinAmount, qrCodeBase64);

            helper.setText(html, true);
            mailSender.send(message);
            logger.info("QR code email sent to {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send QR code email to {}: {}", toEmail, e.getMessage());
        }
    }

    public void sendOrderCompletion(String toEmail, String buyerDisplayName, long coinAmount) {
        if (mailSender == null || fromAddress == null || fromAddress.isBlank() || toEmail == null || toEmail.isBlank()) {
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("TikCoin Order Completed — " + coinAmount + " Coins Delivered!");

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto;">
                      <h2 style="color: #1a1a2e;">Order Completed ✓</h2>
                      <p>Hi <strong>%s</strong>,</p>
                      <p>Your order of <strong>%d TikCoins</strong> has been completed successfully.
                         Check your TikTok account — your coins should be available now.</p>
                      <p>Thank you for using TikCoin!</p>
                      <p style="color: #999; font-size: 12px;">— The TikCoin Team</p>
                    </div>
                    """.formatted(buyerDisplayName != null ? buyerDisplayName : "there", coinAmount);

            helper.setText(html, true);
            mailSender.send(message);
            logger.info("Order completion email sent to {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send completion email to {}: {}", toEmail, e.getMessage());
        }
    }
}