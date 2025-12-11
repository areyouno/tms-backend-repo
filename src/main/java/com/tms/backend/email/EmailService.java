package com.tms.backend.email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.tms.backend.job.JobWorkflowStatus;

import jakarta.mail.internet.MimeMessage;


@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:TransTree}")
    private String appName;

     public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        String subject = "Verify Your Email - " + appName;
        String htmlContent = buildVerificationEmailHTML(verificationLink);
        sendEmail(toEmail, subject, htmlContent);
    }

    private String buildVerificationEmailHTML(String verificationLink) {
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "  <head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <title>Verify Your Email</title>\n"
                + "  </head>\n"
                + "  <body style=\"margin: 0; padding: 0; background-color: #f9f9f9; font-family: sans-serif;\">\n"
                + "    <div style=\"width: 100%; text-align: center; padding: 40px 0;\">\n"
                + "      <div style=\"display: inline-block; max-width: 600px; padding: 30px; border-radius: 8px; \">\n" //box-shadow: 0 0 10px rgba(0,0,0,0.1); background: #ffffff;
                + "        <h2 style=\"text-align: center; color: #333333;\">Verify Your Email</h2>\n"
                + "        <p style=\"text-align: left; color: #555555; font-size: 16px; line-height: 1.6;\">\n"
                + "          Hello,<br>\n"
                + "          We are happy to have you on board!\n"
                + "          To get started, please click the button below to complete verification.\n"
                + "        </p>\n"
                + "        <a href=\"" + verificationLink + "\"\n"
                + "           style=\"display: inline-block; margin-top: 25px; padding: 13px 16px; font-size: 14px; font-weight: 600;\n"
                + "                  color: black; background-color:rgb(236, 126, 53); text-decoration: none;\n"
                + "                  border-radius: 26px; min-width: 50px; text-align: center;\">\n"
                + "          Verify email\n"
                + "        </a>\n"
                + "      </div>\n"
                + "  </body>\n"
                + "</html>".formatted(appName, verificationLink, verificationLink, appName);
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            System.out.println("Using email: " + fromEmail);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true for HTML

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendJobStatusChangeEmail(String toEmail, String projectName, String stepName, JobWorkflowStatus previousStatus, JobWorkflowStatus newStatus) {
        String subject = appName + " - Job Status Changed";
        String htmlContent = buildJobStatusChangeEmailHTML(projectName, stepName, previousStatus, newStatus);
        sendEmail(toEmail, subject, htmlContent);
    }

    private String buildJobStatusChangeEmailHTML(String projectName, String stepName, 
                                             JobWorkflowStatus previousStatus, JobWorkflowStatus newStatus) {
    return "<!DOCTYPE html>\n"
        + "<html>\n"
        + "  <head>\n"
        + "    <meta charset=\"UTF-8\">\n"
        + "    <title>Job Status Update</title>\n"
        + "  </head>\n"
        + "  <body style=\"margin: 0; padding: 0; background-color: #f9f9f9; font-family: sans-serif;\">\n"
        + "    <div style=\"width: 100%; text-align: center; padding: 40px 0;\">\n"
        + "      <div style=\"display: inline-block; max-width: 600px; padding: 30px; border-radius: 8px;\">\n"
        + "        <h2 style=\"text-align: center; color: #333333;\">Job Status Update</h2>\n"
        + "        <p style=\"text-align: left; color: #555555; font-size: 16px; line-height: 1.6;\">\n"
        + "          Hello,<br><br>\n"
        + "          The status of your job has been updated.<br><br>\n"
        + "          <strong>Project:</strong> " + projectName + "<br>\n"
        + "          <strong>For Workflow Step:</strong> " + stepName + "<br><br>\n"
        + "          <strong>Status Change:</strong> from <span style=\"color: #ec7e35;\">" + previousStatus 
        + "</span> to <span style=\"color: #ec7e35;\">" + newStatus + "</span>\n"
        + "        </p>\n"
        + "      </div>\n"
        + "    </div>\n"
        + "  </body>\n"
        + "</html>";
}
}