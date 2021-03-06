/* DigiDoc4J library
*
* This software is released under either the GNU Library General Public
* License (see LICENSE.LGPL).
*
* Note that the only valid version of the LGPL license as far as this
* project is concerned is the original GNU Library General Public License
* Version 2.1, February 1999
*/

package org.digidoc4j;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.cert.CertificateEncodingException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.digidoc4j.exceptions.DigiDoc4JException;
import org.digidoc4j.exceptions.InvalidSignatureException;
import org.digidoc4j.exceptions.TslCertificateSourceInitializationException;
import org.digidoc4j.impl.asic.asice.bdoc.BDocContainer;
import org.digidoc4j.impl.ddoc.DDocContainer;
import org.digidoc4j.test.util.TestDataBuilderUtil;
import org.digidoc4j.utils.Helper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

public class ContainerTest extends AbstractTest {

  private static final String EIDAS_POLICY = "src/test/resources/testFiles/constraints/eIDAS_test_constraint.xml";
  private static final String CERTIFICATE =
      "MIIFEzCCA/ugAwIBAgIQSXxaK/qTYahTT77Z9I56EjANBgkqhkiG9w0BAQUFADBsMQswCQYDVQQGEwJFRTEiMCAGA1UECgwZQVMgU2VydGlmaX" +
          "RzZWVyaW1pc2tlc2t1czEfMB0GA1UEAwwWVEVTVCBvZiBFU1RFSUQtU0sgMjAxMTEYMBYGCSqGSIb3DQEJARYJcGtpQHNrLmVlMB4XDTE0" +
          "MDQxNzExNDUyOVoXDTE2MDQxMjIwNTk1OVowgbQxCzAJBgNVBAYTAkVFMQ8wDQYDVQQKDAZFU1RFSUQxGjAYBgNVBAsMEWRpZ2l0YWwgc2" +
          "lnbmF0dXJlMTEwLwYDVQQDDCjFvcOVUklOw5xXxaBLWSxNw4RSw5wtTMOWw5ZaLDExNDA0MTc2ODY1MRcwFQYDVQQEDA7FvcOVUklOw5xX" +
          "xaBLWTEWMBQGA1UEKgwNTcOEUsOcLUzDlsOWWjEUMBIGA1UEBRMLMTE0MDQxNzY4NjUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAo" +
          "IBAQChn9qVaA+x3RkDBrD5ujwfnreK5/Nb+Nvo9Vg5OLMn3JKUoUhFX6A/q5lBUylK/CU/lNRTv/kicqnu1aCyAiW0XVYk8jrOI1wRbHey" +
          "BMq/5gVm/vbbRtMi/XGLkgMZ5UDxY0QZfmu8wlRJ8164zRNocuUJLLXWOB6vda2RRXC3Cix4TDvQwGmPrQQJ8dzDIJEkLS7NCLBTcndm7b" +
          "uQegRc043gKMjUmRhGZEzF4oJa4pMfXqeSa+PUtrNyNNNQaOwTH29R8aFfGU2xorVvxoUieNipyWMEz8BTUGwwIceapWi77loBV/VQfStX" +
          "nQNu/s6BC04ss43O6sK70MB1qlRZAgMBAAGjggFmMIIBYjAJBgNVHRMEAjAAMA4GA1UdDwEB/wQEAwIGQDCBmQYDVR0gBIGRMIGOMIGLBg" +
          "orBgEEAc4fAwEBMH0wWAYIKwYBBQUHAgIwTB5KAEEAaQBuAHUAbAB0ACAAdABlAHMAdABpAG0AaQBzAGUAawBzAC4AIABPAG4AbAB5ACAA" +
          "ZgBvAHIAIAB0AGUAcwB0AGkAbgBnAC4wIQYIKwYBBQUHAgEWFWh0dHA6Ly93d3cuc2suZWUvY3BzLzAdBgNVHQ4EFgQUEjVsOkaNOGG0Gl" +
          "cF4icqxL0u4YcwIgYIKwYBBQUHAQMEFjAUMAgGBgQAjkYBATAIBgYEAI5GAQQwHwYDVR0jBBgwFoAUQbb+xbGxtFMTjPr6YtA0bW0iNAow" +
          "RQYDVR0fBD4wPDA6oDigNoY0aHR0cDovL3d3dy5zay5lZS9yZXBvc2l0b3J5L2NybHMvdGVzdF9lc3RlaWQyMDExLmNybDANBgkqhkiG9w" +
          "0BAQUFAAOCAQEAYTJLbScA3+Xh/s29Qoc0cLjXW3SVkFP/U71/CCIBQ0ygmCAXiQIp/7X7JonY4aDz5uTmq742zZgq5FA3c3b4NtRzoiJX" +
          "FUWQWZOPE6Ep4Y07Lpbn04sypRKbVEN9TZwDy3elVq84BcX/7oQYliTgj5EaUvpe7MIvkK4DWwrk2ffx9GRW+qQzzjn+OLhFJbT/QWi81Q" +
          "2CrX34GmYGrDTC/thqr5WoPELKRg6a0v3mvOCVtfIxJx7NKK4B6PGhuTl83hGzTc+Wwbaxwjqzl/SUwCNd2R8GV8EkhYH8Kay3Ac7Qx3ag" +
          "rJJ6H8j+h+nCKLjIdYImvnznKyR0N2CRc/zQ+g==";

  @Test
  public void eIDASConfigurationTest() {
    this.configuration = Configuration.of(Configuration.Mode.TEST);
    this.configuration.setValidationPolicy(EIDAS_POLICY);
    Container container = this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/valid-containers/bdoc-tm-with-large-data-file.bdoc"));
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    List<DigiDoc4JException> warnings = result.getWarnings();
    Assert.assertFalse(result.isValid());
    Assert.assertTrue(errors.size() == 2);
    Assert.assertTrue(warnings.size() == 0);
    Assert.assertTrue(result.getReport().contains("The trusted list is not acceptable"));
    Assert.assertTrue(result.getReport().contains("The trusted list has not the expected version"));
  }

  @Test
  public void defaultConfigurationTest() {
    this.configuration = Configuration.of(Configuration.Mode.TEST);
    Container container = this.openContainerByConfiguration(Paths.get("src/test/resources/testFiles/valid-containers/bdoc-tm-with-large-data-file.bdoc"));
    ValidationResult result = container.validate();
    List<DigiDoc4JException> errors = result.getErrors();
    Assert.assertTrue(errors.size() == 0);
    Assert.assertTrue(result.isValid());
  }

  @Test
  public void createBDocContainersByDefault() {
    Assert.assertTrue(this.createNonEmptyContainer() instanceof BDocContainer);
  }

  @Test
  public void createBDocContainer() {
    Assert.assertTrue(this.createEmptyContainerBy(Container.DocumentType.BDOC) instanceof BDocContainer);
  }

  @Test
  public void createDDocContainer() {
    Assert.assertTrue(this.createEmptyContainerBy(Container.DocumentType.DDOC) instanceof DDocContainer);
  }

  @Test
  public void openBDocContainerWhenTheFileIsAZipAndTheExtensionIsBDoc() {
    Assert.assertTrue(ContainerOpener.open("src/test/resources/testFiles/invalid-containers/zip_file_without_asics_extension.bdoc") instanceof BDocContainer);
  }

  @Test
  public void openDDocContainerForAllOtherFiles() {
    Assert.assertTrue(ContainerOpener.open("src/test/resources/testFiles/invalid-containers/changed_digidoc_test.ddoc") instanceof DDocContainer);
  }

  @Test
  public void testAddOneFileToContainerForBDoc() throws Exception {
    Container container = this.createEmptyContainer();
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    List<DataFile> dataFiles = container.getDataFiles();
    Assert.assertEquals(1, dataFiles.size());
    Assert.assertEquals("test.txt", dataFiles.get(0).getName());
    Assert.assertEquals("text/plain", dataFiles.get(0).getMediaType());
  }

  @Test
  public void testRemovesOneFileFromContainerWhenFileExistsForBDoc() throws Exception {
    Container container = this.createEmptyContainer();
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    container.removeDataFile("test.txt");
    Assert.assertEquals(0, container.getDataFiles().size());
  }

  @Test
  public void testCreateBDocContainerSpecifiedByDocumentTypeForBDoc() throws Exception {
    Container container = this.createEmptyContainerBy(Container.DocumentType.BDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    TestDataBuilderUtil.signContainer(container);
    String file = this.getFileBy("bdoc");
    container.save(file);
    Assert.assertTrue(Helper.isZipFile(new File(file)));
  }

  @Test
  public void testCreateDDocContainer() throws Exception {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    container.sign(this.pkcs12SignatureToken);
    String file = this.getFileBy("ddoc");
    container.save(file);
    Assert.assertTrue(Helper.isXMLFile(new File(file)));
  }

  @Test
  public void testAddOneFileToContainerForDDoc() throws Exception {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    List<DataFile> dataFiles = container.getDataFiles();
    Assert.assertEquals(1, dataFiles.size());
    Assert.assertEquals("test.txt", dataFiles.get(0).getName());
    Assert.assertEquals("text/plain", dataFiles.get(0).getMediaType());
  }

  @Test
  public void testRemovesOneFileFromContainerWhenFileExistsForDDoc() throws Exception {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    String file = this.getFileBy("ddoc");
    container.save(file);
    container = this.openContainerBy(Paths.get(file));
    container.removeDataFile("src/test/resources/testFiles/helper-files/test.txt");
    Assert.assertEquals(0, container.getDataFiles().size());
  }

  @Test
  public void addLargeFileToBDoc() throws Exception {
    DataFile dataFile = new LargeDataFile(new ByteArrayInputStream(new byte[]{0, 1, 2, 3}), "large-doc.txt", "text/plain");
    Container container = this.createEmptyContainerBy(Container.DocumentType.BDOC);
    container.addDataFile(dataFile);
    Assert.assertEquals(1, container.getDataFiles().size());
    String file = this.getFileBy("bdoc");
    container.saveAsFile(file);
    container = this.openContainerBy(Paths.get(file));
    Assert.assertEquals(1, container.getDataFiles().size());
    Assert.assertEquals("large-doc.txt", container.getDataFiles().get(0).getName());
  }

  @Test
  public void addLargeFileToDDoc() throws Exception {
    DataFile dataFile = new DataFile(new ByteArrayInputStream(new byte[]{0, 1, 2, 3}), "large-doc.txt", "text/plain");
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile(dataFile);
    container.sign(this.pkcs12SignatureToken);
    Assert.assertEquals(1, container.getDataFiles().size());
    Assert.assertEquals("large-doc.txt", container.getDataFiles().get(0).getName());
    container.saveAsFile(this.getFileBy("ddoc"));
  }

  @Test
  public void testOpenCreatedDDocFile() throws Exception {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    String file = this.getFileBy("ddoc");
    container.save(file);
    Container containerForReading = ContainerOpener.open(file);
    Assert.assertEquals(Container.DocumentType.DDOC, containerForReading.getDocumentType());
    Assert.assertEquals(1, container.getDataFiles().size());
  }

  @Test(expected = DigiDoc4JException.class)
  public void testOpenInvalidFileReturnsError() {
    ContainerOpener.open("src/test/resources/testFiles/helper-files/test.txt");
  }

  @Test
  public void testValidateDDoc() throws Exception {
    Container dDocContainer = ContainerOpener.open("src/test/resources/testFiles/valid-containers/ddoc_for_testing.ddoc");
    Assert.assertFalse(dDocContainer.validate().hasErrors());
    Assert.assertFalse(dDocContainer.validate().hasWarnings());
  }

  @Test
  public void openDDocContainerFromFile() throws Exception {
    Container container = ContainerBuilder.aContainer(Container.DocumentType.DDOC).
        fromExistingFile("src/test/resources/testFiles/valid-containers/ddoc_wo_x509IssueName_xmlns.ddoc").build();
    ValidationResult validate = container.validate();
    Assert.assertTrue(validate.isValid());
    Assert.assertEquals(0, validate.getErrors().size());
    Assert.assertTrue(validate.getReport().contains("X509IssuerName has none or invalid namespace:"));
    Assert.assertTrue(validate.getReport().contains("X509SerialNumber has none or invalid namespace:"));
  }

  @Test(expected = DigiDoc4JException.class)
  public void testOpenNotExistingFileThrowsException() {
    ContainerOpener.open("noFile.ddoc");
  }

  @Test(expected = DigiDoc4JException.class)
  public void testOpenEmptyFileThrowsException() {
    ContainerOpener.open("src/test/resources/testFiles/invalid-containers/emptyFile.ddoc");
  }

  @Test(expected = DigiDoc4JException.class)
  public void testFileTooShortToVerifyIfItIsZipFileThrowsException() {
    ContainerOpener.open("src/test/resources/testFiles/invalid-containers/tooShortToVerifyIfIsZip.ddoc");
  }

  @Test(expected = DigiDoc4JException.class)
  public void testOpenFromStreamTooShortToVerifyIfIsZip() {
    try (FileInputStream stream = new FileInputStream(new File("src/test/resources/testFiles/invalid-containers/tooShortToVerifyIfIsZip.ddoc"))) {
      ContainerOpener.open(stream, true);
    } catch (DigiDoc4JException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAddFileFromStreamToDDoc() throws IOException {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    try (ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{0x42})) {
      container.addDataFile(is, "testFromStream.txt", "text/plain");
    }
    DataFile dataFile = container.getDataFiles().get(0);
    Assert.assertEquals("testFromStream.txt", dataFile.getName());
  }

  @Test
  public void openContainerFromStreamAsBDoc() throws IOException {
    Container container = this.createEmptyContainer();
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    Signature signature = this.createSignatureBy(container, this.pkcs12SignatureToken);
    String file = this.getFileBy("bdoc");
    container.save(file);
    try (FileInputStream stream = new FileInputStream(file)) {
      Container containerToTest = ContainerOpener.open(stream, false);
      Assert.assertEquals(1, containerToTest.getSignatures().size());
    }
  }

  @Test
  public void openContainerFromStreamAsDDoc() throws IOException {
    try (FileInputStream stream = new FileInputStream("src/test/resources/testFiles/valid-containers/ddoc_for_testing.ddoc")) {
      Container container = ContainerOpener.open(stream, false);
      Assert.assertEquals(1, container.getSignatures().size());
    }
  }

  @Test
  public void testGetSignatureFromDDoc() {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    container.sign(this.pkcs12SignatureToken);
    List<Signature> signatures = container.getSignatures();
    Assert.assertEquals(1, signatures.size());
  }

  @Test(expected = DigiDoc4JException.class)
  public void testAddRawSignatureThrowsException() {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addRawSignature(new byte[]{0x42});
  }

  @Test
  public void testAddRawSignatureAsByteArrayForDDoc() throws CertificateEncodingException, IOException, SAXException {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    container.sign(this.pkcs12SignatureToken);
    byte[] signatureBytes = FileUtils.readFileToByteArray(new File(("src/test/resources/testFiles/xades/test-bdoc-tm.xml")));
    container.addRawSignature(signatureBytes);
    Assert.assertEquals(2, container.getSignatures().size());
    Assert.assertEquals(CERTIFICATE.replaceAll("\\s", ""), Base64.encodeBase64String(this.getSigningCertificateAsBytes(container, 1)));
    XMLAssert.assertXMLEqual(new String(signatureBytes).trim(), new String(container.getSignatures().get(1).getAdESSignature()));
  }

  @Test
  public void throwsErrorWhenCreatesDDOCContainerWithConfiguration() throws Exception {
    Container container = ContainerBuilder.aContainer(Container.DocumentType.DDOC).
        withConfiguration(Configuration.getInstance()).build();
    Assert.assertEquals("DDOC", container.getType());
  }

  @Test
  public void testExtendToForBDOC() {
    Container container = this.createEmptyContainer();
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    Signature signature = SignatureBuilder.aSignature(container).withSignatureProfile(SignatureProfile.B_BES).
        withSignatureToken(this.pkcs12SignatureToken).invokeSigning();
    container.addSignature(signature);
    container.extendTo(SignatureProfile.LT);
    Assert.assertNotNull(container.getSignature(0).getOCSPCertificate());
  }

  @Test
  public void testExtendToForDDOC() {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    container.setSignatureProfile(SignatureProfile.B_BES);
    container.sign(this.pkcs12SignatureToken);
    container.extendTo(SignatureProfile.LT_TM);
    Assert.assertNotNull(container.getSignature(0).getOCSPCertificate());
  }

  @Test
  public void addRawSignatureToBDocContainer() throws Exception {
    Container container = this.createEmptyContainerBy(Container.DocumentType.BDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    byte[] signatureBytes = FileUtils.readFileToByteArray(new File("src/test/resources/testFiles/xades/valid-bdoc-tm.xml"));
    container.addRawSignature(signatureBytes);
    String file = this.getFileBy("bdoc");
    container.saveAsFile(file);
    container = ContainerOpener.open(file);
    Assert.assertEquals(1, container.getSignatures().size());
    Assert.assertTrue(container.validate().isValid());
  }

  @Test
  public void addRawSignatureToExistingBDocContainer() throws Exception {
    Container container = this.createNonEmptyContainerBy(Paths.get("src/test/resources/testFiles/helper-files/test.txt"));
    this.createSignatureBy(container, this.pkcs12SignatureToken);
    byte[] signatureBytes = FileUtils.readFileToByteArray(new File("src/test/resources/testFiles/xades/valid-bdoc-tm.xml"));
    container.addRawSignature(signatureBytes);
    String file = this.getFileBy("bdoc");
    container.saveAsFile(file);
    container = ContainerOpener.open(file);
    Assert.assertEquals(2, container.getSignatures().size());
    Assert.assertTrue(container.validate().isValid());
  }

  @Test(expected = InvalidSignatureException.class)
  public void testAddRawSignatureAsByteArrayForBDoc() throws CertificateEncodingException, IOException, SAXException {
    Container container = this.createEmptyContainerBy(Container.DocumentType.BDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    this.createSignatureBy(container, this.pkcs12SignatureToken);
    container.addRawSignature(Base64.decodeBase64("fo4aA1PVI//1agzBm2Vcxj7sk9pYQJt+9a7xLFSkfF10RocvGjVPBI65RMqyxGIsje" +
        "LoeDERfTcjHdNojoK/gEdKtme4z6kvkZzjMjDuJu7krK/3DHBtW3XZleIaWZSWySahUiPNNIuk5ykACUolh+K/UK2aWL3Nh64EWvC8aznLV0" +
        "M21s7GwTv7+iVXhR/6c3O22saWKWsteGT0/AqfcBRoj13H/NyuZOULqU0PFOhbJtV8RyZgC9n2uYBFsnutt5GPvhP+U93gkmFQ0+iC1a9Ktt" +
        "j4QH5si35YmRIe0fp8tGDo6li63/tybb+kQ96AIaRe1NxpkKVDBGNi+VNVNA=="));
  }

  @Test
  public void testAddRawSignatureAsStreamArray() throws CertificateEncodingException, IOException {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    FileInputStream fileInputStream = new FileInputStream("src/test/resources/testFiles/xades/test-bdoc-tm.xml");
    container.addRawSignature(fileInputStream);
    Assert.assertEquals(1, container.getSignatures().size());
    Assert.assertEquals(CERTIFICATE.replaceAll("\\s", ""), Base64.encodeBase64String(getSigningCertificateAsBytes(container, 0)));
  }

  private byte[] getSigningCertificateAsBytes(Container container, int index) throws CertificateEncodingException {
    Signature signature = container.getSignatures().get(index);
    return signature.getSigningCertificate().getX509Certificate().getEncoded();
  }

  @Test
  @Ignore("jDigidoc fails to save a container after a raw signature has been added")
  public void testRemoveSignature() throws IOException {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    container.sign(this.pkcs12SignatureToken);
    FileInputStream fileInputStream = new FileInputStream("src/test/resources/testFiles/xades/test-bdoc-tm.xml");
    container.addRawSignature(fileInputStream);
    container.save("testRemoveSignature.ddoc");
    Container containerToRemoveSignature = ContainerOpener.open("testRemoveSignature.ddoc");
    containerToRemoveSignature.removeSignature(1);
    Assert.assertEquals(1, containerToRemoveSignature.getSignatures().size());
    //todo check is correct signatureXML removed by signing time?
  }

  @Test(expected = DigiDoc4JException.class)
  public void testRemovingNotExistingSignatureThrowsException() {
    Container container = this.createEmptyContainerBy(Container.DocumentType.DDOC);
    container.removeSignature(0);
  }


  @Test
  public void testSigningWithSignerInfo() throws Exception {
    Container container = this.createEmptyContainer();
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    Signature signature = SignatureBuilder.aSignature(container).withCity("myCity").withStateOrProvince("myStateOrProvince").
        withPostalCode("myPostalCode").withCountry("myCountry").withRoles("myRole / myResolution").
        withSignatureToken(this.pkcs12SignatureToken).invokeSigning();
    container.addSignature(signature);
    Assert.assertEquals("myCity", signature.getCity());
    Assert.assertEquals("myStateOrProvince", signature.getStateOrProvince());
    Assert.assertEquals("myPostalCode", signature.getPostalCode());
    Assert.assertEquals("myCountry", signature.getCountryName());
    Assert.assertEquals(1, signature.getSignerRoles().size());
    Assert.assertEquals("myRole / myResolution", signature.getSignerRoles().get(0));
  }

  @Test(expected = TslCertificateSourceInitializationException.class)
  public void testSetConfigurationForBDoc() throws Exception {
    this.configuration = new Configuration(Configuration.Mode.TEST);
    this.configuration.setTslLocation("pole");
    Container container = ContainerBuilder.aContainer(Container.DocumentType.BDOC).withConfiguration(this.configuration).
        withDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain").build();
    this.createSignatureBy(container, this.pkcs12SignatureToken);
  }

  @Test
  public void mustBePossibleToCreateAndVerifyContainerWhereDigestAlgorithmIsSHA224() throws Exception {
    Container container = this.createEmptyContainer();
    container.addDataFile("src/test/resources/testFiles/helper-files/test.txt", "text/plain");
    Signature signature = SignatureBuilder.aSignature(container).withSignatureDigestAlgorithm(DigestAlgorithm.SHA224).
        withSignatureToken(this.pkcs12SignatureToken).invokeSigning();
    container.addSignature(signature);
    String file = this.getFileBy("bdoc");
    container.saveAsFile(file);
    container = ContainerOpener.open(file);
    Assert.assertEquals("http://www.w3.org/2001/04/xmldsig-more#sha224", container.getSignature(0).getSignatureMethod());
  }

  @Test
  public void constructorWithConfigurationParameter() throws Exception {
    Container container = ContainerBuilder.aContainer().
        withConfiguration(Configuration.getInstance()).build();
    Assert.assertEquals("BDOC", container.getType());
  }

  @Test
  @Ignore // Fails on Jenkins - need to verify
  public void createContainerWhenAttachmentNameContainsEstonianCharacters() throws Exception {
    Container container = this.createEmptyContainer();
    String s = "src/test/resources/testFiles/test_o\u0303a\u0308o\u0308u\u0308.txt";
    container.addDataFile(s, "text/plain");
    container.sign(this.pkcs12SignatureToken);
    Assert.assertEquals(1, container.getDataFiles().size());
    ValidationResult validate = container.validate();
    Assert.assertTrue(validate.isValid());
  }

  @Test
  public void containerTypeStringValueForBDOC() throws Exception {
    Assert.assertEquals("application/vnd.etsi.asic-e+zip", this.createEmptyContainer(Container.class).getDocumentType().toString());
  }

  @Test
  public void containerTypeStringValueForDDOC() throws Exception {
    Assert.assertEquals("DDOC", this.createEmptyContainerBy(Container.DocumentType.DDOC, Container.class).getDocumentType().toString());
  }

  @Test
  public void testSigningMultipleFilesInContainer() throws Exception {
    Container container = this.createEmptyContainer();
    container.addDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), "1.txt", "text/plain");
    container.addDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), "2.txt", "text/plain");
    container.addDataFile(new ByteArrayInputStream(new byte[]{1, 2, 3}), "3.txt", "text/plain");
    TestDataBuilderUtil.signContainer(container);
    String file = this.getFileBy("bdoc");
    container.save(file);
    Assert.assertEquals(3, container.getDataFiles().size());
    assertContainsDataFile("1.txt", container);
    assertContainsDataFile("2.txt", container);
    assertContainsDataFile("3.txt", container);
    Container openedContainer = ContainerOpener.open(file);
    Assert.assertEquals(3, openedContainer.getDataFiles().size());
    assertContainsDataFile("1.txt", openedContainer);
    assertContainsDataFile("2.txt", openedContainer);
    assertContainsDataFile("3.txt", openedContainer);
  }

  /*
   * RESTRICTED METHODS
   */

  private void assertContainsDataFile(String fileName, Container container) {
    for (DataFile file : container.getDataFiles()) {
      if (StringUtils.equals(fileName, file.getName())) {
        return;
      }
    }
    Assert.assertFalse("Data file '" + fileName + "' was not found in the container", true);
  }

}
