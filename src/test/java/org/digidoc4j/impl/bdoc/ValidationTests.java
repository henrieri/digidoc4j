/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package org.digidoc4j.impl.bdoc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.digidoc4j.AbstractTest;
import org.digidoc4j.Configuration;
import org.digidoc4j.Container;
import org.digidoc4j.ContainerBuilder;
import org.digidoc4j.ContainerOpener;
import org.digidoc4j.DataToSign;
import org.digidoc4j.Signature;
import org.digidoc4j.SignatureBuilder;
import org.digidoc4j.SignatureProfile;
import org.digidoc4j.TSLCertificateSource;
import org.digidoc4j.ValidationResult;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.digidoc4j.exceptions.DuplicateDataFileException;
import org.digidoc4j.exceptions.InvalidTimestampException;
import org.digidoc4j.exceptions.UnsupportedFormatException;
import org.digidoc4j.exceptions.UntrustedRevocationSourceException;
import org.digidoc4j.impl.asic.tsl.TSLCertificateSourceImpl;
import org.digidoc4j.signers.PKCS12SignatureToken;
import org.digidoc4j.test.TestAssert;
import org.digidoc4j.test.util.TestDataBuilderUtil;
import org.digidoc4j.test.util.TestSigningUtil;
import org.digidoc4j.test.util.TestTSLUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import eu.europa.esig.dss.DSSUtils;

public class ValidationTests extends AbstractTest {

  public static final Configuration PROD_CONFIGURATION = new Configuration(Configuration.Mode.PROD);
  public static final Configuration PROD_CONFIGURATION_WITH_TEST_POLICY = new Configuration(Configuration.Mode.PROD);
  private String containerLocation;

  @BeforeClass
  public static void setUpOnce() throws Exception {
    PROD_CONFIGURATION_WITH_TEST_POLICY.setValidationPolicy("conf/test_constraint.xml");
  }

  @Test
  public void testVerifySignedDocument() throws Exception {
    Container container = this.createNonEmptyContainer();
    Assert.assertTrue(container.validate().isValid());
  }

  @Test
  public void testTestVerifyOnInvalidDocument() throws Exception {
    Container container = TestDataBuilderUtil.open("src/test/resources/testFiles/invalid-containers/invalid_container.bdoc");
    Assert.assertFalse(container.validate().isValid());
  }

  @Test
  public void testValidateEmptyDocument() {
    Container container = this.createEmptyContainerBy(Container.DocumentType.BDOC);
    Assert.assertTrue(container.validate().isValid());
  }

  @Test
  public void testValidate() throws Exception {
    Container container = this.createNonEmptyContainer();
    this.createSignatureBy(container, this.pkcs12SignatureToken);
    Assert.assertEquals(0, container.validate().getErrors().size());
  }

  @Test(expected = UnsupportedFormatException.class)
  public void notBDocThrowsException() {
    TestDataBuilderUtil.open("src/test/resources/testFiles/invalid-containers/notABDoc.bdoc");
  }

  @Test(expected = UnsupportedFormatException.class)
  public void incorrectMimetypeThrowsException() {
    TestDataBuilderUtil.open("src/test/resources/testFiles/invalid-containers/incorrectMimetype.bdoc");
  }

  @Test(expected = Exception.class)
  public void testExpiredCertSign() {
    try {
      DataToSign dataToSign = SignatureBuilder.aSignature(this.createNonEmptyContainer()).withSigningCertificate(TestSigningUtil
          .getSigningCertificate("src/test/resources/testFiles/p12/expired_signer.p12", "test")).buildDataToSign();
      dataToSign.finalize(TestSigningUtil.sign(dataToSign.getDataToSign(), dataToSign.getDigestAlgorithm()));
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("not in certificate validity range"));
      throw e;
    }
  }

  @Test
  public void signatureFileContainsIncorrectFileName() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/filename_mismatch_signature.asice", PROD_CONFIGURATION);
    ValidationResult validate = container.validate();
    List<DigiDoc4JException> errors = validate.getErrors();
    Assert.assertEquals(1, errors.size());
    TestAssert.assertContainsError("(Signature ID: S0) The reference data object(s) is not found!", errors);
  }

  @Test
  public void validateContainer_withChangedDataFileContent_isInvalid() throws Exception {
    this.setGlobalMode(Configuration.Mode.TEST);
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/invalid-data-file.bdoc");
    ValidationResult validate = container.validate();
    Assert.assertEquals(1, validate.getErrors().size());
    Assert.assertEquals("(Signature ID: S0) The reference data object(s) is not intact!", validate.getErrors().get(0).toString());
  }

  @Test
  public void secondSignatureFileContainsIncorrectFileName() throws IOException, CertificateException {
    TestTSLUtil.addSkTsaCertificateToTsl(this.configuration);
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/filename_mismatch_second_signature.asice", this.configuration);
    ValidationResult validate = container.validate();
    List<DigiDoc4JException> errors = validate.getErrors();
    Assert.assertEquals(3, errors.size());
    Assert.assertEquals("(Signature ID: S1) The reference data object(s) is not intact!", errors.get(0).toString());
    Assert.assertEquals("(Signature ID: S1) Manifest file has an entry for file test.txt with mimetype text/plain but the signature file for " +
        "signature S1 does not have an entry for this file", errors.get(1).toString());
    Assert.assertEquals("Container contains a file named test.txt which is not found in the signature file",
        errors.get(2).toString());
  }

  @Test
  public void manifestFileContainsIncorrectFileName() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/filename_mismatch_manifest.asice", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult validate = container.validate();
    Assert.assertEquals(2, validate.getErrors().size());
    Assert.assertEquals("(Signature ID: S0) Manifest file has an entry for file incorrect.txt with mimetype text/plain but the signature file " +
        "for signature S0 does not have an entry for this file", validate.getErrors().get(0).toString());
    Assert.assertEquals("(Signature ID: S0) The signature file for signature S0 has an entry for file RELEASE-NOTES.txt with mimetype " +
            "text/plain but the manifest file does not have an entry for this file",
        validate.getErrors().get(1).toString());
  }

  @Test
  public void container_withChangedDataFileName_shouldBeInvalid() throws Exception {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/bdoc-tm-with-changed-data-file-name.bdoc");
    Assert.assertEquals(1, container.validate().getErrors().size());
  }

  @Test
  @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
  public void revocationAndTimeStampDifferenceTooLarge() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/revocation_timestamp_delta_26h.asice", PROD_CONFIGURATION);
    ValidationResult validate = container.validate();
    Assert.assertEquals(1, validate.getErrors().size());
    Assert.assertEquals("(Signature ID: S0) The difference between the OCSP response time and the signature timestamp is too large",
        validate.getErrors().get(0).toString());
  }

  @Test
  public void revocationAndTimeStampDifferenceNotTooLarge() {
    Configuration configuration = new Configuration(Configuration.Mode.PROD);
    int delta27Hours = 27 * 60;
    configuration.setRevocationAndTimestampDeltaInMinutes(delta27Hours);
    ValidationResult result = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/revocation_timestamp_delta_26h.asice", configuration).validate();
    Assert.assertEquals(0, result.getErrors().size());
    Assert.assertEquals(2, result.getWarnings().size());
  }

  @Test
  public void signatureFileAndManifestFileContainDifferentMimeTypeForFile() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/mimetype_mismatch.asice", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult validate = container.validate();
    Assert.assertEquals(1, validate.getErrors().size());
    Assert.assertEquals("(Signature ID: S0) Manifest file has an entry for file RELEASE-NOTES.txt with mimetype application/pdf but the " +
        "signature file for signature S0 indicates the mimetype is text/plain", validate.getErrors().get(0).toString());
  }

  @Test(expected = DuplicateDataFileException.class)
  public void duplicateFileThrowsException() {
    ContainerOpener.open("src/test/resources/testFiles/invalid-containers/22902_data_files_with_same_names.bdoc").validate();
  }

  @Test(expected = DigiDoc4JException.class)
  public void duplicateSignatureFileThrowsException() {
    ContainerOpener.open("src/test/resources/testFiles/invalid-containers/22913_signatures_xml_double.bdoc").validate();
  }

  @Test
  public void missingManifestFile() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/missing_manifest.asice", PROD_CONFIGURATION);
    ValidationResult result = container.validate();
    Assert.assertFalse(result.isValid());
    Assert.assertEquals("Unsupported format: Container does not contain a manifest file", result.getErrors().get(0).getMessage());
  }

  @Test(expected = DigiDoc4JException.class)
  public void missingMimeTypeFile() {
    ContainerOpener.open("src/test/resources/testFiles/invalid-containers/missing_mimetype_file.asice");
  }

  @Test
  public void containerHasFileWhichIsNotInManifestAndNotInSignatureFile() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/extra_file_in_container.asice", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("Container contains a file named AdditionalFile.txt which is not found in the signature file",
        errors.get(0).getMessage());
  }

  @Test
  public void containerMissesFileWhichIsInManifestAndSignatureFile() {
    TestTSLUtil.addSkTsaCertificateToTsl(this.configuration);
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/zip_misses_file_which_is_in_manifest.asice");
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    TestAssert.assertContainsError("(Signature ID: S0) The reference data object(s) is not found!", errors);
  }

  @Test
  public void containerMissingOCSPData() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/TS-06_23634_TS_missing_OCSP_adjusted.asice");
    ValidationResult validate = container.validate();
    List<DigiDoc4JException> errors = validate.getErrors();
    Assert.assertEquals(SignatureProfile.LT, container.getSignatures().get(0).getProfile());
    TestAssert.assertContainsError("(Signature ID: S0) No revocation data for the certificate", errors);
    TestAssert.assertContainsError("(Signature ID: S0) Manifest file has an entry for file test.txt with mimetype text/plain but the signature file for signature S0 indicates the mimetype is application/octet-stream", errors);
  }

  @Ignore("This signature has two OCSP responses: one correct and one is technically corrupted. Opening a container should not throw an exception")
  @Test(expected = DigiDoc4JException.class)
  public void corruptedOCSPDataThrowsException() {
    ContainerOpener.open("src/test/resources/testFiles/invalid-containers/corrupted_ocsp_data.asice");
  }

  @Test
  public void invalidNoncePolicyOid() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/23608_bdoc21-invalid-nonce-policy-oid.bdoc", PROD_CONFIGURATION);
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("(Signature ID: S0) Wrong policy identifier: 1.3.6.1.4.1.10015.1000.3.4.3", errors.get(0).toString());
  }

  @Test
  public void badNonceContent() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/bdoc21-bad-nonce-content.bdoc", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("(Signature ID: S0) Nonce is invalid", errors.get(0).toString());
  }

  @Test
  public void noSignedPropRefTM() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/REF-03_bdoc21-TM-no-signedpropref.bdoc", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(2, errors.size());
    TestAssert.assertContainsError("(Signature ID: S0) Signed properties missing", errors);
    TestAssert.assertContainsError("(Signature ID: S0) The reference data object(s) is not found!", errors);
    Assert.assertEquals(2, container.getSignatures().get(0).validateSignature().getErrors().size());
  }

  @Test
  public void noSignedPropRefTS() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/REF-03_bdoc21-TS-no-signedpropref.asice", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(2, errors.size());
    TestAssert.assertContainsError("(Signature ID: S0) Signed properties missing", errors);
    TestAssert.assertContainsError("(Signature ID: S0) The reference data object(s) is not found!", errors);
  }

  @Test
  public void multipleSignedProperties() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/multiple_signed_properties.asice");
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    TestAssert.assertContainsError("Multiple signed properties", errors);
    TestAssert.assertContainsError("The signature is not intact!", errors);
  }

  @Test
  public void incorrectSignedPropertiesReference() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/signed_properties_reference_not_found.asice", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("(Signature ID: S0) The reference data object(s) is not found!", errors.get(0).toString());
  }

  @Test
  public void nonceIncorrectContent() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/nonce-vale-sisu.bdoc", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(3, errors.size());
    Assert.assertEquals("(Signature ID: S0) Wrong policy identifier: 1.3.6.1.4.1.10015.1000.2.10.10", errors.get(0).toString());
    Assert.assertEquals("(Signature ID: S0) The signature policy is not available!", errors.get(1).toString());
    Assert.assertEquals("(Signature ID: S0) Nonce is invalid", errors.get(2).toString());
  }

  @Test
  public void badNoncePolicyOidQualifier() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/SP-03_bdoc21-bad-nonce-policy-oidasuri.bdoc", PROD_CONFIGURATION_WITH_TEST_POLICY);
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("(Signature ID: S0) Wrong policy identifier qualifier: OIDAsURI", errors.get(0).toString());
    Assert.assertEquals(1, container.getSignatures().get(0).validateSignature().getErrors().size());
  }

  @Test
  public void invalidNonce() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/23200_weakdigest-wrong-nonce.asice");
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("(Signature ID: S0) Nonce is invalid", errors.get(0).toString());
  }

  @Test
  public void brokenTS() {
    Container container = ContainerOpener.open("src/test/resources/testFiles/invalid-containers/TS_broken_TS.asice");
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("(Signature ID: S0) " + InvalidTimestampException.MESSAGE, errors.get(0).toString());
  }

  @Test
  public void asicValidationShouldFail_ifTimeStampHashDoesntMatchSignature() throws Exception {
    this.setGlobalMode(Configuration.Mode.TEST);
    ValidationResult result = this.openContainerBy(Paths.get("src/test/resources/testFiles/invalid-containers/TS-02_23634_TS_wrong_SignatureValue.asice")).validate();
    Assert.assertFalse(result.isValid());
    TestAssert.assertContainsError(InvalidTimestampException.MESSAGE, result.getErrors());
  }

  @Test
  public void containerWithTMProfile_SignedWithExpiredCertificate_shouldBeInvalid() throws Exception {
    Assert.assertFalse(this.openContainerBy(Paths.get("src/test/resources/testFiles/invalid-containers/invalid_bdoc_tm_old-sig-sigat-NOK-prodat-NOK.bdoc")).validate().isValid());
    Assert.assertFalse(this.openContainerBy(Paths.get("src/test/resources/testFiles/invalid-containers/invalid_bdoc_tm_old-sig-sigat-OK-prodat-NOK.bdoc")).validate().isValid());
  }

  @Test
  public void containerWithTSProfile_SignedWithExpiredCertificate_shouldBeInvalid() throws Exception {
    Assert.assertFalse(this.openContainerBy(Paths.get("src/test/resources/testFiles/invalid-containers/invalid_bdoc21-TS-old-cert.bdoc")).validate().isValid());
  }

  @Test
  public void bdocTM_signedWithValidCert_isExpiredByNow_shouldBeValid() throws Exception {
    String containerPath = "src/test/resources/testFiles/valid-containers/valid_bdoc_tm_signed_with_valid_cert_expired_by_now.bdoc";
    Configuration configuration = new Configuration(Configuration.Mode.TEST);
    TestTSLUtil.addCertificateFromFileToTsl(configuration, "src/test/resources/testFiles/certs/ESTEID-SK_2007_prod.pem.crt");
    Container container = ContainerBuilder.aContainer().fromExistingFile(containerPath).
        withConfiguration(configuration).build();
    Assert.assertTrue(container.validate().isValid());
  }

  @Test
  public void signaturesWithCrlShouldBeInvalid() throws Exception {
    ValidationResult result = this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/invalid-containers/asic-with-crl-and-without-ocsp.asice"), PROD_CONFIGURATION).validate();
    Assert.assertFalse(result.isValid());
    Assert.assertTrue(result.getErrors().get(0) instanceof UntrustedRevocationSourceException);
  }

  @Test
  public void bDoc_withoutOcspResponse_shouldBeInvalid() throws Exception {
    Assert.assertFalse(this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/invalid-containers/23608-bdoc21-no-ocsp.bdoc"), PROD_CONFIGURATION).validate().isValid());
  }

  @Test
  public void ocspResponseShouldNotBeTakenFromPreviouslyValidatedSignatures_whenOcspResponseIsMissing() throws Exception {
    Assert.assertFalse(this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/invalid-containers/bdoc-tm-ocsp-revoked.bdoc"), this.configuration).validate().isValid());
    Assert.assertTrue(this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/valid-containers/valid-bdoc-tm.bdoc"), this.configuration).validate().isValid());
    Assert.assertFalse(this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/invalid-containers/invalid-bdoc-tm-missing-revoked-ocsp.bdoc"), this.configuration).validate().isValid());
  }

  @Test
  public void validateContainerWithBomSymbolsInMimeType_shouldBeValid() throws Exception {
    Assert.assertTrue(this.openContainerByConfiguration(Paths.get("src/test/resources/prodFiles/valid-containers/IB-4185_bdoc21_TM_mimetype_with_BOM_PROD.bdoc"), PROD_CONFIGURATION).validate().isValid());
  }

  @Test
  public void havingOnlyCaCertificateInTSL_shouldNotValidateOCSPResponse() throws Exception {
    TSLCertificateSourceImpl tsl = new TSLCertificateSourceImpl();
    this.configuration.setTSL(tsl);
    try (InputStream inputStream = this.getClass().getResourceAsStream("/certs/TEST ESTEID-SK 2011.crt")) {
      tsl.addTSLCertificate(DSSUtils.loadCertificate(inputStream).getCertificate());
    }
    ValidationResult result = this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/valid-containers/valid-bdoc-tm.bdoc"), this.configuration).validate();
    Assert.assertFalse(result.isValid());
    TestAssert.assertContainsError("The certificate chain for revocation data is not trusted, there is no trusted anchor.", result.getErrors());
  }

  @Test
  public void mixTSLCertAndTSLOnlineSources_SignatureTypeLT_valid() throws Exception {
    try (InputStream stream = new FileInputStream("src/test/resources/testFiles/certs/exampleCA.cer")) {
      this.configuration.getTSL().addTSLCertificate(DSSUtils.loadCertificate(stream).getCertificate());
      this.configuration.getTSL().addTSLCertificate(DSSUtils.loadCertificate(new FileInputStream("src/test/resources/testFiles/certs/SK-OCSP-RESPONDER-2011_test.cer")).getCertificate());
    }
    Container container = this.createNonEmptyContainerByConfiguration();
    this.createSignatureBy(container, SignatureProfile.LT, new PKCS12SignatureToken("src/test/resources/testFiles/p12/user_one.p12", "user_one".toCharArray()));
    ValidationResult result = container.validate();
    Assert.assertTrue(result.isValid());
    Assert.assertEquals(0, result.getErrors().size());
  }

  @Test
  public void mixTSLCertAndTSLOnlineSources_SignatureTypeLT_notValid() throws Exception {
    TSLCertificateSource certificateSource = new TSLCertificateSourceImpl();
    try (InputStream inputStream = new FileInputStream("src/test/resources/testFiles/certs/exampleCA.cer")) {
      X509Certificate certificate = DSSUtils.loadCertificate(inputStream).getCertificate();
      certificateSource.addTSLCertificate(certificate);
      certificateSource.addTSLCertificate(DSSUtils.loadCertificate(new FileInputStream("src/test/resources/testFiles/certs/SK-OCSP-RESPONDER-2011_test.cer")).getCertificate());
    }
    this.configuration.setTSL(certificateSource);
    Container container = this.createNonEmptyContainerByConfiguration();
    this.createSignatureBy(container, SignatureProfile.LT, new PKCS12SignatureToken("src/test/resources/testFiles/p12/user_one.p12", "user_one".toCharArray()));
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    List<Signature> signatureList = container.getSignatures();
    Signature signature = signatureList.get(0);
    String signatureId = signature.getId();
    Assert.assertFalse(result.isValid());
    Assert.assertEquals(1, errors.size());
    Assert.assertEquals("(Signature ID: " + signatureId + ") Signature has an invalid timestamp", errors.get(0).toString());
  }

  @Test
  public void validateAsiceContainer_getNotValid() throws Exception {
    Assert.assertFalse(this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/invalid-containers/TM-16_unknown.4.asice"), this.configuration).validate().isValid());
  }

  @Test
  public void validateSpuriElement_UriIsvalid() throws Exception {
    Container container = ContainerOpener.open("src/test/resources/testFiles/valid-containers/valid-bdoc-tm.bdoc", this.configuration);
    Assert.assertTrue(container.validate().isValid());
  }

  @Test
  public void validateSpuriElement_UriIsMissing() throws Exception {
    Container container = ContainerOpener.open("src/test/resources/testFiles/valid-containers/23608_bdoc21-no-nonce-policy.bdoc", this.configuration);
    ValidationResult result = container.validate();
    Assert.assertFalse(container.validate().isValid());
    TestAssert.assertContainsError("Error: The URL in signature policy is empty or not available", result.getErrors());
  }

  @Test
  public void validateSpuriElement_UriIsEmpty() throws Exception {
    Container container = ContainerOpener.open("src/test/resources/testFiles/valid-containers/SP-06_bdoc21-no-uri.bdoc", this.configuration);
    ValidationResult result = container.validate();
    Assert.assertFalse(result.isValid());
    TestAssert.assertContainsError("The URL in signature policy is empty or not available", result.getErrors());
  }

  /*
   * RESTRICTED METHODS
   */

  @Override
  protected void before() {
    this.configuration = Configuration.of(Configuration.Mode.TEST);
    this.containerLocation = this.getFileBy("bdoc");
  }

}
