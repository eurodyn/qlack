package com.eurodyn.qlack.fuse.crypto.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Locale;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SignDTO {
  @NotNull
  private PrivateKey privateKey;
  @NotNull
  private PublicKey publicKey;
  @NotNull
  private String issuerCN;
  @NotNull
  private Instant validForm;
  @NotNull
  private Instant validto;
  @NotNull
  private Locale locale;
  @NotNull
  private String subjectCN;
  @NotNull
  private String signatureAlgorithm;
  @NotNull
  private String signatureProvider;
}