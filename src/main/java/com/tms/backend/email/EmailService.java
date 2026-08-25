package com.tms.backend.email;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @Value("${app.frontend-url}")
    private String frontendUrl;

     public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        String subject = "Verify Your Email - " + appName;
        String htmlContent = buildVerificationEmailHTML(verificationLink);
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendInvitationEmail(String toEmail, String invitationLink, String username) {
        String subject = "You have been invited to join " + appName;
        String htmlContent = buildInvitationEmailHTML(invitationLink, username);
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "Reset Your Password - " + appName;
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        String htmlContent = buildPasswordResetEmailHTML(resetLink);
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
                + "          We are happy to have you on board! <br>\n"
                + "          To get started, please click the button below to complete verification.\n"
                + "        </p>\n"
                + "        <a href=\"" + verificationLink + "\"\n"
                + "           style=\"display: inline-block; padding: 14px 28px; font-size: 15px; font-weight: 600; color: #ffffff; background-color: #3d8ace; text-decoration: none; border-radius: 6px; text-align: center; mso-padding-alt: 0;\">\n"
                + "          Verify email\n"
                + "        </a>\n"
                + "        <p style=\"margin-top: 25px; color: #999999; font-size: 12px;\">This is an automated message, please do not reply.</p>\n"
                + "      </div>\n"
                + "  </body>\n"
                + "</html>".formatted(appName, verificationLink, verificationLink, appName);
    }

    private String buildPasswordResetEmailHTML(String resetLink) {
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "  <head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <title>Reset Your Password</title>\n"
                + "  </head>\n"
                + "  <body style=\"margin: 0; padding: 0; background-color: #f9f9f9; font-family: sans-serif;\">\n"
                + "    <div style=\"width: 100%; text-align: center; padding: 40px 0;\">\n"
                + "      <div style=\"display: inline-block; max-width: 600px; padding: 30px; border-radius: 8px; \">\n"
                + "        <h2 style=\"text-align: center; color: #333333;\">Reset Your Password</h2>\n"
                + "        <p style=\"text-align: left; color: #555555; font-size: 16px; line-height: 1.6;\">\n"
                + "          Hello,<br>\n"
                + "          We received a request to reset your password. <br>\n"
                + "          Click the button below to choose a new password. This link will expire in 1 hour.\n"
                + "        </p>\n"
                + "        <a href=\"" + resetLink + "\"\n"
                + "           style=\"display: inline-block; padding: 14px 28px; font-size: 15px; font-weight: 600; color: #ffffff; background-color: #3d8ace; text-decoration: none; border-radius: 6px; text-align: center; mso-padding-alt: 0;\">\n"
                + "          Reset password\n"
                + "        </a>\n"
                + "        <p style=\"margin-top: 25px; color: #999999; font-size: 12px;\">If you did not request a password reset, you can safely ignore this email.</p>\n"
                + "      </div>\n"
                + "  </body>\n"
                + "</html>";
    }

    private String buildInvitationEmailHTML(String invitationLink, String username) {
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "  <head>\n"
                + "    <meta charset=\"UTF-8\">\n"
                + "    <title></title>\n"
                + "  </head>\n"
                + "  <body style=\"margin: 0; padding: 0; background-color: #f9f9f9; font-family: sans-serif;\">\n"
                + "    <div style=\"width: 100%; padding: 40px 20px; box-sizing: border-box;\">\n"
                + "      <div style=\"max-width: 600px; padding: 30px; border-radius: 8px;\">\n" //box-shadow: 0 0 10px rgba(0,0,0,0.1); background: #ffffff;
                + "        <h2 style=\"text-align: left; color: rgb(61, 138, 206);\">TransTree</h2>\n"
                + "        <p style=\"text-align: left; color: #555555; font-size: 16px; line-height: 1.6;\">\n"
                + "          Greetings! You have been invited to join TransTree. <br> \n"
                + "          To verify your account, you'll need to assign a password, then set up and confirm your profile. <br> \n"
                + "          Your username is <b>" + username + "</b>. Click the button below to get started.\n"
                + "        </p>\n"
                + "        <a href=\"" + invitationLink + "\"\n"
                + "           style=\"display: inline-block; padding: 14px 28px; font-size: 15px; font-weight: 600; color: #ffffff; background-color: #3d8ace; text-decoration: none; border-radius: 6px; text-align: center; mso-padding-alt: 0;\">\n"
                + "          Setup my profile\n"
                + "        </a>\n"
                + "        <p style=\"margin-top: 25px; color: #999999; font-size: 12px;\">This is an automated message, please do not reply.</p>\n"
                + "      </div>\n"
                + "    </div>\n"
                + "  </body>\n"
                + "</html>".formatted(appName, invitationLink, invitationLink, appName);
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
        + "          <strong>Status Change:</strong> from <span style=\"color: rgb(61, 138, 206);\">" + previousStatus 
        + "</span> to <span style=\"color: rgb(236, 126, 53);\">" + newStatus + "</span>\n"
        + "        </p>\n"
        + "        <p style=\"margin-top: 25px; color: #999999; font-size: 12px;\">This is an automated message, please do not reply.</p>\n"
        + "      </div>\n"
        + "    </div>\n"
        + "  </body>\n"
        + "</html>";
}

    public void sendTaskUnassignmentEmail(String toEmail, String assigneeFirstName, String taskName) {
        String subject = appName + " - Task Unassigned: " + taskName;
        String htmlContent = buildTaskUnassignmentEmailHTML(assigneeFirstName, taskName);
        sendEmail(toEmail, subject, htmlContent);
    }

    private String buildTaskUnassignmentEmailHTML(String assigneeFirstName, String taskName) {
        return "<!DOCTYPE html>\n"
            + "<html>\n"
            + "  <head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <title>Task Unassigned</title>\n"
            + "  </head>\n"
            + "  <body style=\"margin: 0; padding: 0; background-color: #f9f9f9; font-family: sans-serif;\">\n"
            + "    <div style=\"width: 100%; padding: 40px 20px; box-sizing: border-box;\">\n"
            + "      <div style=\"max-width: 600px; padding: 30px; border-radius: 8px;\">\n"
            + "        <p style=\"text-align: left; color: #555555; font-size: 16px; line-height: 1.6;\">\n"
            + "          Hello " + assigneeFirstName + ",<br><br>\n"
            + "          The task <strong>" + taskName + "</strong> that was assigned to you has been deleted.<br>\n"
            + "          You have been unassigned from it. We apologize for any inconvenience.\n"
            + "        </p>\n"
            + "        <p style=\"margin-top: 25px; color: #999999; font-size: 12px;\">This is an automated message, please do not reply.</p>\n"
            + "      </div>\n"
            + "    </div>\n"
            + "  </body>\n"
            + "</html>";
    }

    public void sendTaskAssignmentEmail(String toEmail, String assigneeFirstName, String taskName, String workflowStepName,
                                         String sourceLang, String targetLang,
                                         LocalDateTime startDate, LocalDateTime dueDate, String description, Long taskListId) {
        String subject = appName + " - New Task Assigned: " + taskName;
        String taskListLink = frontendUrl + "/tms/tasklist/" + taskListId;
        String htmlContent = buildTaskAssignmentEmailHTML(assigneeFirstName, taskName, workflowStepName, sourceLang, targetLang, startDate, dueDate, description, taskListLink);
        sendEmail(toEmail, subject, htmlContent);
    }

    private String buildTaskAssignmentEmailHTML(String assigneeFirstName, String taskName, String workflowStepName,
                                                  String sourceLang, String targetLang,
                                                  LocalDateTime startDate, LocalDateTime dueDate, String description,
                                                  String taskListLink) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        String startDateText = startDate != null ? startDate.format(formatter) : "Not specified";
        String dueDateText = dueDate != null ? dueDate.format(formatter) : "Not specified";
        String sourceLangText = (sourceLang != null && !sourceLang.isBlank()) ? sourceLang : "Not specified";
        String targetLangText = (targetLang != null && !targetLang.isBlank()) ? targetLang : "Not specified";
        String descriptionRow = (description != null && !description.isBlank())
                ? "          <tr><td style=\"padding: 8px 12px; border: 1px solid #dddddd; vertical-align: top;\"><strong>Note:</strong></td><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\">" + description + "</td></tr>\n"
                : "";

        return "<!DOCTYPE html>\n"
            + "<html>\n"
            + "  <head>\n"
            + "    <meta charset=\"UTF-8\">\n"
            + "    <title>New Task Assigned</title>\n"
            + "  </head>\n"
            + "  <body style=\"margin: 0; padding: 0; background-color: #f9f9f9; font-family: sans-serif;\">\n"
            + "    <div style=\"width: 100%; padding: 40px 20px; box-sizing: border-box;\">\n"
            + "      <div style=\"max-width: 600px; padding: 30px; border-radius: 8px;\">\n"
            + "        <p style=\"text-align: left; color: #555555; font-size: 16px; line-height: 1.6;\">\n"
            + "          Hello " + assigneeFirstName + ",<br><br>\n"
            + "          You have been assigned to a task.\n"
            + "        </p>\n"
            + "        <table style=\"border-collapse: collapse; border: 1px solid #dddddd; color: #555555; font-size: 16px; line-height: 1.6;\">\n"
            + "          <tr><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\"><strong>Task:</strong></td><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\">" + taskName + "</td></tr>\n"
            + "          <tr><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\"><strong>Workflow Step:</strong></td><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\">" + workflowStepName + "</td></tr>\n"
            + "          <tr><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\"><strong>Source Language:</strong></td><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\">" + sourceLangText + "</td></tr>\n"
            + "          <tr><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\"><strong>Target Language:</strong></td><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\">" + targetLangText + "</td></tr>\n"
            + "          <tr><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\"><strong>Start Date:</strong></td><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\">" + startDateText + "</td></tr>\n"
            + "          <tr><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\"><strong>Due Date:</strong></td><td style=\"padding: 8px 12px; border: 1px solid #dddddd;\">" + dueDateText + "</td></tr>\n"
            + descriptionRow
            + "        </table>\n"
            + "        <a href=\"" + taskListLink + "\"\n"
            + "           style=\"display: inline-block; margin-top: 25px; padding: 14px 28px; font-size: 15px; font-weight: 600; color: #ffffff; background-color: #3d8ace; text-decoration: none; border-radius: 6px; text-align: center; mso-padding-alt: 0;\">\n"
            + "          View Task\n"
            + "        </a>\n"
            + "        <p style=\"margin-top: 25px; color: #999999; font-size: 12px;\">This is an automated message, please do not reply.</p>\n"
            + "      </div>\n"
            + "    </div>\n"
            + "  </body>\n"
            + "</html>";
    }
}