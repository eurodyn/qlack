package com.eurodyn.qlack.fuse.mailing.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for Email data.
 */
@Getter
@Setter
public class EmailDTO implements Serializable {

  private String id;

  private String messageId;
  private @NotBlank String subject;
  private @NotBlank String body;
  private @NotBlank String from;
  private @NotEmpty List<String> toContact;
  private List<String> ccContact;
  private List<String> bccContact;
  private List<String> replyToContact;
  private @NotNull EMAIL_TYPE emailType;
  private String status;
  private List<AttachmentDTO> attachments;
  private Date dateSent;
  private String serverResponse;

  public EmailDTO() {
    this.emailType = EMAIL_TYPE.TEXT;
  }

  public void setToContact(List<String> toContact) {
    this.toContact = toContact;
  }

  public void setToContact(String toContact) {
    List<String> l = new ArrayList<String>();
    l.add(toContact);
    setToContact(l);
  }

  public void addAttachment(AttachmentDTO attachmentDTO) {
    if (attachments == null) {
      attachments = new ArrayList<>();
    }
    attachments.add(attachmentDTO);
  }

  public void setDateSent(Long dateSent) {
    if (dateSent != null) {
      this.dateSent = new Date(dateSent);
    }
  }

  public void resetAllRecipients() {
    this.toContact = null;
    this.ccContact = null;
    this.bccContact = null;
  }

  @Override
  public String toString() {
    StringBuffer strBuf = new StringBuffer();
    strBuf.append("DTO id is: " + getId())
        .append("Subject is: " + getSubject())
        .append("To contact List: ")
        .append(getToContact() != null ? getToContact().toString() : null)
        .append("CC contact List: ")
        .append(getCcContact() != null ? getCcContact().toString() : null)
        .append("BCC contact List: ")
        .append(getBccContact() != null ? getBccContact().toString() : null)
        .append("body: ").append(body)
        .append("status: ").append(status)
        .append("Date sent: ").append(dateSent)
        .append("Server Response: ").append(serverResponse)
        .append("attachment: ").append(attachments)
        .append("email type: ").append(emailType)
        .append("message Id: ").append(messageId);
    return strBuf.toString();
  }

  public static enum EMAIL_TYPE {
    TEXT, HTML
  }

}
