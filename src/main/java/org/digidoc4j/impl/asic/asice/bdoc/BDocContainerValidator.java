/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package org.digidoc4j.impl.asic.asice.bdoc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.digidoc4j.Configuration;
import org.digidoc4j.Signature;
import org.digidoc4j.SignatureValidationResult;
import org.digidoc4j.ValidationResult;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.digidoc4j.exceptions.TechnicalException;
import org.digidoc4j.exceptions.UnsupportedFormatException;
import org.digidoc4j.impl.asic.AsicParseResult;
import org.digidoc4j.impl.asic.AsicValidationReportBuilder;
import org.digidoc4j.impl.asic.AsicValidationResult;
import org.digidoc4j.impl.asic.manifest.ManifestErrorMessage;
import org.digidoc4j.impl.asic.manifest.ManifestParser;
import org.digidoc4j.impl.asic.manifest.ManifestValidator;
import org.digidoc4j.impl.asic.xades.validation.SignatureValidationData;
import org.digidoc4j.impl.asic.xades.validation.SignatureValidationTask;
import org.digidoc4j.impl.asic.xades.validation.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.DSSDocument;

/**
 * BDOC container validator
 */
public class BDocContainerValidator implements Serializable {

  private final static Logger logger = LoggerFactory.getLogger(BDocContainerValidator.class);
  private List<DigiDoc4JException> errors = new ArrayList<>();
  private List<DigiDoc4JException> warnings = new ArrayList<>();
  private AsicParseResult containerParseResult;
  private boolean validateManifest;
  private List<SignatureValidationData> signatureValidationData = new ArrayList<>();
  private List<DigiDoc4JException> manifestErrors;
  private ThreadPoolManager threadPoolManager;

  /**
   * @param configuration configuration
   */
  public BDocContainerValidator(Configuration configuration) {
    threadPoolManager = new ThreadPoolManager(configuration);
    validateManifest = false;
  }

  /**
   * @param containerParseResult ASIC container parse result
   * @param configuration configuration
   */
  public BDocContainerValidator(AsicParseResult containerParseResult, Configuration configuration) {
    this.containerParseResult = containerParseResult;
    threadPoolManager = new ThreadPoolManager(configuration);
    validateManifest = true;
  }

  /**
   * @param signatures list of signatures
   * @return validation result
   */
  public ValidationResult validate(List<Signature> signatures) {
    logger.debug("Validating BDOC container");
    validateSignatures(signatures);
    extractManifestErrors(signatures);
    AsicValidationResult result = createValidationResult();
    logger.info("Is container valid: " + result.isValid());
    return result;
  }

  private void validateSignatures(List<Signature> signatures) {
    List<Future<SignatureValidationData>> validationData = startSignatureValidationInParallel(signatures);
    extractValidatedSignatureErrors(validationData);
  }

  private List<Future<SignatureValidationData>> startSignatureValidationInParallel(List<Signature> signatures) {
    List<Future<SignatureValidationData>> futures = new ArrayList<>();
    for (Signature signature : signatures) {
      SignatureValidationTask validationExecutor = new SignatureValidationTask(signature);
      Future<SignatureValidationData> validationDataFuture = threadPoolManager.submit(validationExecutor);
      futures.add(validationDataFuture);
    }
    return futures;
  }

  private void extractValidatedSignatureErrors(List<Future<SignatureValidationData>> validationFutures) {
    logger.debug("Extracting errors from the signatures");
    for (Future<SignatureValidationData> validationFuture : validationFutures) {
      try {
        SignatureValidationData validationData = validationFuture.get();
        extractSignatureErrors(validationData);
      } catch (InterruptedException | ExecutionException e) {
        logger.error("Error validating signatures on multiple threads: " + e.getMessage());
        throw new TechnicalException("Error validating signatures on multiple threads: " + e.getMessage(), e);
      }
    }
  }

  public void setValidateManifest(boolean validateManifest) {
    this.validateManifest = validateManifest;
  }

  private void extractSignatureErrors(SignatureValidationData validationData) {
    logger.debug("Extracting signature errors for signature " + validationData.getSignatureId());
    signatureValidationData.add(validationData);
    SignatureValidationResult validationResult = validationData.getValidationResult();
    List<DigiDoc4JException> signatureErrors = validationResult.getErrors();
    errors.addAll(signatureErrors);
    warnings.addAll(validationResult.getWarnings());
  }

  private void extractManifestErrors(List<Signature> signatures) {
    logger.debug("Extracting manifest errors");
    manifestErrors = findManifestErrors(signatures);
    errors.addAll(manifestErrors);
  }

  private AsicValidationResult createValidationResult() {
    AsicValidationReportBuilder reportBuilder = new AsicValidationReportBuilder(signatureValidationData, manifestErrors);
    AsicValidationResult result = new AsicValidationResult();
    result.setErrors(errors);
    result.setWarnings(warnings);
    result.setContainerErrorsOnly(manifestErrors);
    result.setReportBuilder(reportBuilder);
    return result;
  }

  private List<DigiDoc4JException> findManifestErrors(List<Signature> signatures) {
    if (!validateManifest || containerParseResult == null) {
      return Collections.emptyList();
    }
    ManifestParser manifestParser = containerParseResult.getManifestParser();
    if (manifestParser == null || !manifestParser.containsManifestFile()) {
      logger.error("Container is missing manifest.xml");
      List<DigiDoc4JException> manifestExceptions = new ArrayList<>();
      manifestExceptions.add(new UnsupportedFormatException("Container does not contain a manifest file"));
      return manifestExceptions;
    }
    List<DigiDoc4JException> manifestExceptions = new ArrayList<>();
    List<DSSDocument> detachedContents = containerParseResult.getDetachedContents();
    List<ManifestErrorMessage> manifestErrorMessageList = new ManifestValidator(manifestParser, detachedContents, signatures).validateDocument();
    for (ManifestErrorMessage manifestErrorMessage : manifestErrorMessageList) {
      manifestExceptions.add(new DigiDoc4JException(manifestErrorMessage.getErrorMessage(), manifestErrorMessage.getSignatureId()));
    }
    return manifestExceptions;
  }

}
