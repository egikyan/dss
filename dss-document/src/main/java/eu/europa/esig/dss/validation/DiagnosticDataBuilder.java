/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.validation;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.IssuerSerial;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.esig.dss.crl.CRLBinary;
import eu.europa.esig.dss.diagnostic.jaxb.XmlBasicSignature;
import eu.europa.esig.dss.diagnostic.jaxb.XmlCertificate;
import eu.europa.esig.dss.diagnostic.jaxb.XmlCertificatePolicy;
import eu.europa.esig.dss.diagnostic.jaxb.XmlCertificateRef;
import eu.europa.esig.dss.diagnostic.jaxb.XmlCertificateRevocation;
import eu.europa.esig.dss.diagnostic.jaxb.XmlChainItem;
import eu.europa.esig.dss.diagnostic.jaxb.XmlContainerInfo;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDiagnosticData;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestAlgoAndValue;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDigestMatcher;
import eu.europa.esig.dss.diagnostic.jaxb.XmlDistinguishedName;
import eu.europa.esig.dss.diagnostic.jaxb.XmlFoundCertificates;
import eu.europa.esig.dss.diagnostic.jaxb.XmlFoundRevocations;
import eu.europa.esig.dss.diagnostic.jaxb.XmlFoundTimestamp;
import eu.europa.esig.dss.diagnostic.jaxb.XmlLangAndValue;
import eu.europa.esig.dss.diagnostic.jaxb.XmlManifestFile;
import eu.europa.esig.dss.diagnostic.jaxb.XmlOID;
import eu.europa.esig.dss.diagnostic.jaxb.XmlOrphanCertificate;
import eu.europa.esig.dss.diagnostic.jaxb.XmlOrphanCertificateToken;
import eu.europa.esig.dss.diagnostic.jaxb.XmlOrphanRevocation;
import eu.europa.esig.dss.diagnostic.jaxb.XmlOrphanRevocationToken;
import eu.europa.esig.dss.diagnostic.jaxb.XmlOrphanTokens;
import eu.europa.esig.dss.diagnostic.jaxb.XmlPDFRevision;
import eu.europa.esig.dss.diagnostic.jaxb.XmlPDFSignatureDictionary;
import eu.europa.esig.dss.diagnostic.jaxb.XmlPolicy;
import eu.europa.esig.dss.diagnostic.jaxb.XmlRelatedCertificate;
import eu.europa.esig.dss.diagnostic.jaxb.XmlRelatedRevocation;
import eu.europa.esig.dss.diagnostic.jaxb.XmlRevocation;
import eu.europa.esig.dss.diagnostic.jaxb.XmlRevocationRef;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignature;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignatureDigestReference;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignatureProductionPlace;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignatureScope;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignerData;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignerDocumentRepresentations;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignerInfo;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSignerRole;
import eu.europa.esig.dss.diagnostic.jaxb.XmlSigningCertificate;
import eu.europa.esig.dss.diagnostic.jaxb.XmlStructuralValidation;
import eu.europa.esig.dss.diagnostic.jaxb.XmlTimestamp;
import eu.europa.esig.dss.diagnostic.jaxb.XmlTimestampedObject;
import eu.europa.esig.dss.diagnostic.jaxb.XmlTrustedList;
import eu.europa.esig.dss.diagnostic.jaxb.XmlTrustedService;
import eu.europa.esig.dss.diagnostic.jaxb.XmlTrustedServiceProvider;
import eu.europa.esig.dss.enumerations.CertificateOrigin;
import eu.europa.esig.dss.enumerations.CertificateSourceType;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.DigestMatcherType;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import eu.europa.esig.dss.enumerations.RevocationOrigin;
import eu.europa.esig.dss.enumerations.RevocationType;
import eu.europa.esig.dss.enumerations.SignatureAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureValidity;
import eu.europa.esig.dss.enumerations.TimestampedObjectType;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.identifier.EncapsulatedRevocationTokenIdentifier;
import eu.europa.esig.dss.model.identifier.Identifier;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.model.x509.Token;
import eu.europa.esig.dss.model.x509.TokenComparator;
import eu.europa.esig.dss.spi.DSSASN1Utils;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.tsl.Condition;
import eu.europa.esig.dss.spi.tsl.ConditionForQualifiers;
import eu.europa.esig.dss.spi.tsl.DownloadInfoRecord;
import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.ParsingInfoRecord;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.tsl.TrustProperties;
import eu.europa.esig.dss.spi.tsl.TrustServiceProvider;
import eu.europa.esig.dss.spi.tsl.TrustServiceStatusAndInformationExtensions;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.spi.tsl.ValidationInfoRecord;
import eu.europa.esig.dss.spi.util.TimeDependentValues;
import eu.europa.esig.dss.spi.x509.CertificatePolicy;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonCertificateSource;
import eu.europa.esig.dss.spi.x509.revocation.RevocationRef;
import eu.europa.esig.dss.spi.x509.revocation.RevocationToken;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLRef;
import eu.europa.esig.dss.spi.x509.revocation.crl.CRLToken;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPRef;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPResponseBinary;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.OCSPToken;
import eu.europa.esig.dss.spi.x509.revocation.ocsp.ResponderId;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.policy.BasicASNSignaturePolicyValidator;
import eu.europa.esig.dss.validation.policy.SignaturePolicyValidator;
import eu.europa.esig.dss.validation.scope.SignatureScope;
import eu.europa.esig.dss.validation.timestamp.TimestampToken;
import eu.europa.esig.dss.validation.timestamp.TimestampedReference;

/**
 * This class is used to build JAXB objects from the DSS model
 * 
 */
public class DiagnosticDataBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(DiagnosticDataBuilder.class);

	private DSSDocument signedDocument;
	private ContainerInfo containerInfo;
	private List<AdvancedSignature> signatures;
	private Set<CertificateToken> usedCertificates;
	private Map<CertificateToken, Set<CertificateSourceType>> certificateSourceTypes;
	private Set<RevocationToken> usedRevocations;
	private Set<TimestampToken> usedTimestamps;
	private List<CertificateSource> trustedCertSources = new ArrayList<>();
	private Date validationDate;
	
	// Merged validation data sources
	private ListCertificateSource commonCertificateSource = new ListCertificateSource();
	private ListCRLSource commonCRLSource = new ListCRLSource();
	private ListOCSPSource commonOCSPSource = new ListOCSPSource();

	private boolean includeRawCertificateTokens = false;
	private boolean includeRawRevocationData = false;
	private boolean includeRawTimestampTokens = false;
	
	private DigestAlgorithm defaultDigestAlgorithm = DigestAlgorithm.SHA256;

	private Map<String, XmlCertificate> xmlCertsMap = new HashMap<>();
	private Map<String, XmlRevocation> xmlRevocationsMap = new HashMap<>();
	private Map<String, XmlSignature> xmlSignaturesMap = new HashMap<>();
	private Map<String, XmlTimestamp> xmlTimestampsMap = new HashMap<>();
	private Map<String, XmlSignerData> xmlSignedDataMap = new HashMap<>();
	private Map<String, XmlOrphanCertificateToken> xmlOrphanCertificateTokensMap = new HashMap<>();
	private Map<String, XmlOrphanRevocationToken> xmlOrphanRevocationTokensMap = new HashMap<>();
	private Map<String, XmlTrustedList> xmlTrustedListsMap = new HashMap<>();
	
	// A map between references ids and their related token ids (used to map references for timestamped refs)
	private Map<String, String> referenceMap = new HashMap<>();

	/**
	 * This method allows to set the document which is analysed
	 * 
	 * @param signedDocument
	 *                       the document which is analysed
	 * @return the builder
	 */
	public DiagnosticDataBuilder document(DSSDocument signedDocument) {
		this.signedDocument = signedDocument;
		return this;
	}

	/**
	 * This method allows to set the container info (ASiC)
	 * 
	 * @param containerInfo
	 *                      the container information
	 * @return the builder
	 */
	public DiagnosticDataBuilder containerInfo(ContainerInfo containerInfo) {
		this.containerInfo = containerInfo;
		return this;
	}

	/**
	 * This method allows to set the found signatures
	 * 
	 * @param signatures
	 *                   the found signatures
	 * @return the builder
	 */
	public DiagnosticDataBuilder foundSignatures(List<AdvancedSignature> signatures) {
		this.signatures = signatures;
		return this;
	}

	/**
	 * This method allows to set the used certificates
	 * 
	 * @param usedCertificates
	 *                         the used certificates
	 * @return the builder
	 */
	public DiagnosticDataBuilder usedCertificates(Set<CertificateToken> usedCertificates) {
		this.usedCertificates = usedCertificates;
		return this;
	}

	/**
	 * This method allows to set the certificate source types
	 * 
	 * @param certificateSourceTypes
	 *                               the certificate source types
	 * @return the builder
	 */
	public DiagnosticDataBuilder certificateSourceTypes(Map<CertificateToken, Set<CertificateSourceType>> certificateSourceTypes) {
		this.certificateSourceTypes = certificateSourceTypes;
		return this;
	}

	/**
	 * This method allows to set the used revocation data
	 * 
	 * @param usedRevocations
	 *                        the used revocation data
	 * @return the builder
	 */
	public DiagnosticDataBuilder usedRevocations(Set<RevocationToken> usedRevocations) {
		this.usedRevocations = usedRevocations;
		return this;
	}
	
	/**
	 * This method allows to set the timestamps
	 * 
	 * @param usedTimestamps
	 *                       a set of validated {@link TimestampToken}s
	 * @return the builder
	 */
	public DiagnosticDataBuilder usedTimestamps(Set<TimestampToken> usedTimestamps) {
		this.usedTimestamps = usedTimestamps;
		return this;
	}

	/**
	 * This method allows set the behavior to include raw certificate tokens into
	 * the diagnostic report. (default: false)
	 * 
	 * @param includeRawCertificateTokens
	 *                                    true if the certificate tokens need to be
	 *                                    exported in the diagnostic data
	 * @return the builder
	 */
	public DiagnosticDataBuilder includeRawCertificateTokens(boolean includeRawCertificateTokens) {
		this.includeRawCertificateTokens = includeRawCertificateTokens;
		return this;
	}

	/**
	 * This method allows set the behavior to include raw revocation data into the
	 * diagnostic report. (default: false)
	 * 
	 * @param includeRawRevocationData
	 *                                 true if the revocation data need to be
	 *                                 exported in the diagnostic data
	 * @return the builder
	 */
	public DiagnosticDataBuilder includeRawRevocationData(boolean includeRawRevocationData) {
		this.includeRawRevocationData = includeRawRevocationData;
		return this;
	}

	/**
	 * This method allows set the behavior to include raw timestamp tokens into the
	 * diagnostic report. (default: false)
	 * 
	 * @param includeRawTimestampTokens
	 *                                  true if the timestamp tokens need to be
	 *                                  exported in the diagnostic data
	 * @return the builder
	 */
	public DiagnosticDataBuilder includeRawTimestampTokens(boolean includeRawTimestampTokens) {
		this.includeRawTimestampTokens = includeRawTimestampTokens;
		return this;
	}
	
	/**
	 * This method allows to set the default {@link DigestAlgorithm} which will be
	 * used for tokens' DigestAlgoAndValue calculation
	 * 
	 * @param digestAlgorithm
	 *                        {@link DigestAlgorithm} to set as default
	 * @return the builder
	 */
	public DiagnosticDataBuilder setDefaultDigestAlgorithm(DigestAlgorithm digestAlgorithm) {
		this.defaultDigestAlgorithm = digestAlgorithm;
		return this;
	}

	/**
	 * This method allows to set the TrustedListsCertificateSources
	 * 
	 * @param trustedCertSources
	 *                          the list of trusted lists certificate sources
	 * @return the builder
	 */
	public DiagnosticDataBuilder trustedCertificateSources(List<CertificateSource> trustedCertSources) {
		for (CertificateSource trustedSource : trustedCertSources) {
			if (CertificateSourceType.TRUSTED_STORE.equals(trustedSource.getCertificateSourceType()) || 
					CertificateSourceType.TRUSTED_LIST.equals(trustedSource.getCertificateSourceType())) {
				this.trustedCertSources.add(trustedSource);
			} else {
				throw new DSSException("Trusted CertificateSource must be of type TRUSTED_STORE or TRUSTED_LIST!");
			}
		}
		return this;
	}

	/**
	 * This method allows to set the validation date
	 * 
	 * @param validationDate
	 *                       the validation date
	 * @return the builder
	 */
	public DiagnosticDataBuilder validationDate(Date validationDate) {
		this.validationDate = validationDate;
		return this;
	}
	
	/**
	 * Sets a merged Certificate Source
	 * 
	 * @param completeCertificateSource 
	 * 						 {@link ListCertificateSource} computed from existing sources
	 * @return the builder
	 */
	public DiagnosticDataBuilder completeCertificateSource(ListCertificateSource completeCertificateSource) {
		this.commonCertificateSource = completeCertificateSource;
		return this;
	}
	
	/**
	 * Sets a merged CRL Source
	 * 
	 * @param completeCRLSource 
	 * 						 {@link ListCRLSource} computed from existing sources
	 * @return the builder
	 */
	public DiagnosticDataBuilder completeCRLSource(ListCRLSource completeCRLSource) {
		this.commonCRLSource = completeCRLSource;
		return this;
	}
	
	/**
	 * Sets a merged OCSP Source
	 * 
	 * @param completeOCSPSource 
	 * 						 {@link ListOCSPSource} computed from existing sources
	 * @return the builder
	 */
	public DiagnosticDataBuilder completeCRLSource(ListOCSPSource completeOCSPSource) {
		this.commonOCSPSource = completeOCSPSource;
		return this;
	}

	public XmlDiagnosticData build() {
		
		XmlDiagnosticData diagnosticData = new XmlDiagnosticData();
		if (signedDocument != null) {
			diagnosticData.setDocumentName(removeSpecialCharsForXml(signedDocument.getName()));
		}
		diagnosticData.setValidationDate(validationDate);
		diagnosticData.setContainerInfo(getXmlContainerInfo());

		Collection<XmlCertificate> xmlCertificates = buildXmlCertificates(usedCertificates);
		diagnosticData.getUsedCertificates().addAll(xmlCertificates);
		linkSigningCertificateAndChains(usedCertificates);

		Collection<XmlRevocation> xmlRevocations = buildXmlRevocations(usedRevocations);
		diagnosticData.getUsedRevocations().addAll(xmlRevocations);
		
		linkCertificatesAndRevocations(usedCertificates);

		// collect original signer documents
		Collection<XmlSignerData> xmlSignerData = buildXmlSignerDataList(signatures, usedTimestamps);
		diagnosticData.getOriginalDocuments().addAll(xmlSignerData);
		
		populateListSources(); // creates merged sources with certificates and revocation data
		
		if (Utils.isCollectionNotEmpty(signatures)) {
			Collection<XmlSignature> xmlSignatures = buildXmlSignatures(signatures);
			diagnosticData.getSignatures().addAll(xmlSignatures);
			attachCounterSignatures(signatures);
		}

		if (Utils.isCollectionNotEmpty(usedTimestamps)) {
			List<XmlTimestamp> builtTimestamps = buildXmlTimestamps(usedTimestamps);
			diagnosticData.getUsedTimestamps().addAll(builtTimestamps);
			linkSignaturesAndTimestamps(signatures);
			linkTimestampsAndTimestampsObjects(usedTimestamps);
		}

		if (Utils.isMapNotEmpty(xmlOrphanCertificateTokensMap) || Utils.isMapNotEmpty(xmlOrphanRevocationTokensMap)) {
			diagnosticData.setOrphanTokens(buildXmlOrphanTokens());
		}

		if (isUseTrustedLists()) {
			Collection<XmlTrustedList> trustedLists = buildXmlTrustedLists(trustedCertSources);
			diagnosticData.getTrustedLists().addAll(trustedLists);
			linkCertificatesAndTrustServices(usedCertificates);
		}

		return diagnosticData;
	}

	private boolean isUseTrustedLists() {
		if (Utils.isCollectionNotEmpty(trustedCertSources)) {
			for (CertificateSource certificateSource : trustedCertSources) {
				if (certificateSource instanceof TrustedListsCertificateSource) {
					return true;
				}
			}
		}
		return false;
	}

	private void linkTimestampsAndTimestampsObjects(Set<TimestampToken> timestamps) {
		for (TimestampToken timestampToken : timestamps) {
			XmlTimestamp xmlTimestampToken = xmlTimestampsMap.get(timestampToken.getDSSIdAsString());
			xmlTimestampToken.setTimestampedObjects(getXmlTimestampedObjects(timestampToken));
		}
	}

	private Collection<XmlCertificate> buildXmlCertificates(Set<CertificateToken> certificates) {
		List<XmlCertificate> builtCertificates = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(certificates)) {
			List<CertificateToken> tokens = new ArrayList<>(certificates);
			Collections.sort(tokens, new TokenComparator());
			for (CertificateToken certificateToken : tokens) {
				String id = certificateToken.getDSSIdAsString();
				XmlCertificate xmlCertificate = xmlCertsMap.get(id);
				if (xmlCertificate == null) {
					xmlCertificate = buildDetachedXmlCertificate(certificateToken);
					xmlCertsMap.put(id, xmlCertificate);
				}
				builtCertificates.add(xmlCertificate);
			}
		}
		return builtCertificates;
	}
	
	private void linkSigningCertificateAndChains(Set<CertificateToken> certificates) {
		if (Utils.isCollectionNotEmpty(certificates)) {
			for (CertificateToken certificateToken : certificates) {
				XmlCertificate xmlCertificate = xmlCertsMap.get(certificateToken.getDSSIdAsString());
				xmlCertificate.setSigningCertificate(getXmlSigningCertificate(certificateToken.getPublicKeyOfTheSigner()));
				xmlCertificate.setCertificateChain(getXmlForCertificateChain(certificateToken.getPublicKeyOfTheSigner()));
			}
		}
	}

	private void linkCertificatesAndTrustServices(Set<CertificateToken> certificates) {
		if (Utils.isCollectionNotEmpty(certificates)) {
			for (CertificateToken certificateToken : certificates) {
				XmlCertificate xmlCertificate = xmlCertsMap.get(certificateToken.getDSSIdAsString());
				xmlCertificate.setTrustedServiceProviders(getXmlTrustedServiceProviders(certificateToken));
			}
		}
	}

	private Collection<XmlRevocation> buildXmlRevocations(Set<RevocationToken> revocations) {
		List<XmlRevocation> builtRevocations = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(revocations)) {
			List<RevocationToken> tokens = new ArrayList<>(revocations);
			Collections.sort(tokens, new TokenComparator());
			List<String> uniqueIds = new ArrayList<>(); // CRL can contain multiple entries
			for (RevocationToken revocationToken : tokens) {
				String id = revocationToken.getDSSIdAsString();
				if (uniqueIds.contains(id)) {
					continue;
				}
				XmlRevocation xmlRevocation = xmlRevocationsMap.get(id);
				if (xmlRevocation == null) {
					xmlRevocation = buildDetachedXmlRevocation(revocationToken);
					xmlRevocation.setSigningCertificate(getXmlSigningCertificate(revocationToken.getPublicKeyOfTheSigner()));
					xmlRevocationsMap.put(id, xmlRevocation);
					builtRevocations.add(xmlRevocation);
				}
				uniqueIds.add(id);
			}
		}
		return builtRevocations;
	}

	private void linkCertificatesAndRevocations(Set<CertificateToken> certificates) {
		if (Utils.isCollectionNotEmpty(certificates)) {
			for (CertificateToken certificateToken : certificates) {
				XmlCertificate xmlCertificate = xmlCertsMap.get(certificateToken.getDSSIdAsString());
				Set<RevocationToken> revocationsForCert = getRevocationsForCert(certificateToken);
				for (RevocationToken revocationToken : revocationsForCert) {
					XmlRevocation xmlRevocation = xmlRevocationsMap.get(revocationToken.getDSSIdAsString());
					XmlCertificateRevocation xmlCertificateRevocation = new XmlCertificateRevocation();
					xmlCertificateRevocation.setRevocation(xmlRevocation);

					final Boolean revocationTokenStatus = revocationToken.getStatus();
					// revocationTokenStatus can be null when OCSP return Unknown. In this case we
					// set status to false.
					xmlCertificateRevocation.setStatus(revocationTokenStatus == null ? false : revocationTokenStatus);
					xmlCertificateRevocation.setRevocationDate(revocationToken.getRevocationDate());
					xmlCertificateRevocation.setReason(revocationToken.getReason());

					xmlCertificate.getRevocations().add(xmlCertificateRevocation);
				}
			}
		}
	}
	
	private Collection<XmlSignerData> buildXmlSignerDataList(Collection<AdvancedSignature> signatures, Collection<TimestampToken> timestamps) {
		List<String> addedSignedDataIds = new ArrayList<>();
		List<XmlSignerData> signerDataList = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(signatures)) {
			for (AdvancedSignature signature : signatures) {
				if (Utils.isCollectionNotEmpty(signature.getSignatureScopes())) {
					for (SignatureScope signatureScope : signature.getSignatureScopes()) {
						if (!addedSignedDataIds.contains(signatureScope.getDSSIdAsString())) {
							XmlSignerData xmlSignerData = buildXmlSignerData(signatureScope);
							signerDataList.add(xmlSignerData);
							addedSignedDataIds.add(signatureScope.getDSSIdAsString());
						}
					}
				}
			}
		}
		if (Utils.isCollectionNotEmpty(timestamps)) {
			for (TimestampToken timestampToken : timestamps) {
				if (Utils.isCollectionNotEmpty(timestampToken.getTimestampScopes())) {
					for (SignatureScope signatureScope : timestampToken.getTimestampScopes()) {
						if (!addedSignedDataIds.contains(signatureScope.getDSSIdAsString())) {
							XmlSignerData xmlSignerData = buildXmlSignerData(signatureScope);
							signerDataList.add(xmlSignerData);
							addedSignedDataIds.add(signatureScope.getDSSIdAsString());
						}
					}
				}
			}
		}
		return signerDataList;
	}
	
	private XmlSignerData buildXmlSignerData(SignatureScope signatureScope) {
		String id = signatureScope.getDSSIdAsString();
		XmlSignerData xmlSignerData = xmlSignedDataMap.get(id);
		if (xmlSignerData == null) {
			xmlSignerData = getXmlSignerData(signatureScope);
			xmlSignedDataMap.put(id, xmlSignerData);
		}
		return xmlSignerData;
	}
	
	private void populateListSources() {
		// used certificates can contain additional certificates, e.g. a revocation's issuer
		if (Utils.isCollectionNotEmpty(usedCertificates)) {
			CommonCertificateSource usedCertificatesSource = new CommonCertificateSource();
			for (CertificateToken certificateToken : usedCertificates) {
				usedCertificatesSource.addCertificate(certificateToken);
			}
			commonCertificateSource.add(usedCertificatesSource);
		}
		if (Utils.isCollectionNotEmpty(signatures)) {
			for (AdvancedSignature advancedSignature : signatures) {
				commonCertificateSource.add(advancedSignature.getCertificateSource());
				commonCRLSource.add(advancedSignature.getCRLSource());
				commonOCSPSource.add(advancedSignature.getOCSPSource());
			}
		}
		if (Utils.isCollectionNotEmpty(usedTimestamps)) {
			for (TimestampToken timestampToken : usedTimestamps) {
				commonCertificateSource.add(timestampToken.getCertificateSource());
				commonCRLSource.add(timestampToken.getCRLSource());
				commonOCSPSource.add(timestampToken.getOCSPSource());
			}
		}
	}
	
	private Collection<XmlSignature> buildXmlSignatures(List<AdvancedSignature> signatures) {
		List<XmlSignature> builtSignatures = new ArrayList<>();
		for (AdvancedSignature advancedSignature : signatures) {
			String id = advancedSignature.getId();
			XmlSignature xmlSignature = xmlSignaturesMap.get(id);
			if (xmlSignature == null) {
				xmlSignature = buildDetachedXmlSignature(advancedSignature);
				xmlSignaturesMap.put(id, xmlSignature);
				builtSignatures.add(xmlSignature);
			}
		}
		return builtSignatures;
	}
	
	private void attachCounterSignatures(List<AdvancedSignature> signatures) {
		for (AdvancedSignature advancedSignature : signatures) {
			XmlSignature currentSignature = xmlSignaturesMap.get(advancedSignature.getId());
			// attach master
			AdvancedSignature masterSignature = advancedSignature.getMasterSignature();
			if (masterSignature != null) {
				XmlSignature xmlMasterSignature = xmlSignaturesMap.get(masterSignature.getId());
				currentSignature.setCounterSignature(true);
				currentSignature.setParent(xmlMasterSignature);
			}
		}
	}
	
	private void linkSignaturesAndTimestamps(List<AdvancedSignature> signatures) {
		for (AdvancedSignature advancedSignature : signatures) {
			XmlSignature currentSignature = xmlSignaturesMap.get(advancedSignature.getId());
			// attach timestamps
			currentSignature.setFoundTimestamps(getXmlFoundTimestamps(advancedSignature));
		}
	}

	private Collection<XmlTrustedList> buildXmlTrustedLists(List<CertificateSource> certificateSources) {
		List<XmlTrustedList> trustedLists = new ArrayList<>();

		Map<Identifier, XmlTrustedList> mapTrustedLists = new HashMap<>();
		Map<Identifier, XmlTrustedList> mapListOfTrustedLists = new HashMap<>();

		for (CertificateSource certificateSource : certificateSources) {
			if (certificateSource instanceof TrustedListsCertificateSource) {
				TrustedListsCertificateSource tlCertSource = (TrustedListsCertificateSource) certificateSource;
				TLValidationJobSummary summary = tlCertSource.getSummary();
				if (summary != null) {
					Set<Identifier> tlIdentifiers = getTLIdentifiers(tlCertSource);
					for (Identifier tlId : tlIdentifiers) {
						if (!mapTrustedLists.containsKey(tlId)) {
							TLInfo tlInfoById = summary.getTLInfoById(tlId);
							if (tlInfoById != null) {
								mapTrustedLists.put(tlId, getXmlTrustedList(tlInfoById));
							}
						}
					}

					Set<Identifier> lotlIdentifiers = getLOTLIdentifiers(tlCertSource);
					for (Identifier lotlId : lotlIdentifiers) {
						if (!mapListOfTrustedLists.containsKey(lotlId)) {
							LOTLInfo lotlInfoById = summary.getLOTLInfoById(lotlId);
							if (lotlInfoById != null) {
								mapTrustedLists.put(lotlId, getXmlTrustedList(lotlInfoById));
							}
						}
					}

				} else {
					LOG.warn("The TrustedListsCertificateSource does not contain TLValidationJobSummary. TLValidationJob is not performed!");
				}
			}
		}

		trustedLists.addAll(mapTrustedLists.values());
		trustedLists.addAll(mapListOfTrustedLists.values());
		return trustedLists;
	}

	private Set<Identifier> getTLIdentifiers(TrustedListsCertificateSource tlCS) {
		Set<Identifier> tlIdentifiers = new HashSet<>();
		for (CertificateToken certificateToken : usedCertificates) {
			List<TrustProperties> trustServices = tlCS.getTrustServices(certificateToken);
			for (TrustProperties trustProperties : trustServices) {
				tlIdentifiers.add(trustProperties.getTLIdentifier());
			}
		}
		return tlIdentifiers;
	}

	private Set<Identifier> getLOTLIdentifiers(TrustedListsCertificateSource tlCS) {
		Set<Identifier> lotlIdentifiers = new HashSet<>();
		for (CertificateToken certificateToken : usedCertificates) {
			List<TrustProperties> trustServices = tlCS.getTrustServices(certificateToken);
			for (TrustProperties trustProperties : trustServices) {
				Identifier lotlUrl = trustProperties.getLOTLIdentifier();
				if (lotlUrl != null) {
					lotlIdentifiers.add(lotlUrl);
				}
			}
		}
		return lotlIdentifiers;
	}

	private XmlTrustedList getXmlTrustedList(TLInfo tlInfo) {
		String id = tlInfo.getIdentifier().asXmlId();
		XmlTrustedList result = xmlTrustedListsMap.get(id);
		if (result == null) {
			result = new XmlTrustedList();
			if (tlInfo instanceof LOTLInfo) {
				result.setLOTL(true);
			}
			result.setId(id);
			result.setUrl(tlInfo.getUrl());
			ParsingInfoRecord parsingCacheInfo = tlInfo.getParsingCacheInfo();
			if (parsingCacheInfo != null) {
				result.setCountryCode(parsingCacheInfo.getTerritory());
				result.setIssueDate(parsingCacheInfo.getIssueDate());
				result.setNextUpdate(parsingCacheInfo.getNextUpdateDate());
				result.setSequenceNumber(parsingCacheInfo.getSequenceNumber());
				result.setVersion(parsingCacheInfo.getVersion());
			}
			DownloadInfoRecord downloadCacheInfo = tlInfo.getDownloadCacheInfo();
			if (downloadCacheInfo != null) {
				result.setLastLoading(downloadCacheInfo.getLastSuccessDownloadTime());
			}
			ValidationInfoRecord validationCacheInfo = tlInfo.getValidationCacheInfo();
			if (validationCacheInfo != null) {
				result.setWellSigned(validationCacheInfo.isValid());
			}
			xmlTrustedListsMap.put(id, result);
		}
		return result;
	}

	private XmlContainerInfo getXmlContainerInfo() {
		if (containerInfo != null) {
			XmlContainerInfo xmlContainerInfo = new XmlContainerInfo();
			xmlContainerInfo.setContainerType(containerInfo.getContainerType().getReadable());
			String zipComment = containerInfo.getZipComment();
			if (Utils.isStringNotBlank(zipComment)) {
				xmlContainerInfo.setZipComment(zipComment);
			}
			xmlContainerInfo.setMimeTypeFilePresent(containerInfo.isMimeTypeFilePresent());
			xmlContainerInfo.setMimeTypeContent(containerInfo.getMimeTypeContent());
			xmlContainerInfo.setContentFiles(containerInfo.getSignedDocumentFilenames());
			xmlContainerInfo.setManifestFiles(getXmlManifests(containerInfo.getManifestFiles()));
			return xmlContainerInfo;
		}
		return null;
	}

	private List<XmlManifestFile> getXmlManifests(List<ManifestFile> manifestFiles) {
		if (Utils.isCollectionNotEmpty(manifestFiles)) {
			List<XmlManifestFile> xmlManifests = new ArrayList<>();
			for (ManifestFile manifestFile : manifestFiles) {
				XmlManifestFile xmlManifest = new XmlManifestFile();
				xmlManifest.setFilename(manifestFile.getFilename());
				xmlManifest.setSignatureFilename(manifestFile.getSignatureFilename());
				for (ManifestEntry entry : manifestFile.getEntries()) {
					xmlManifest.getEntries().add(entry.getFileName());
				}
				xmlManifests.add(xmlManifest);
			}
			return xmlManifests;
		}
		return null;
	}

	private XmlSignature buildDetachedXmlSignature(AdvancedSignature signature) {
		XmlSignature xmlSignature = new XmlSignature();
		if (hasDuplicate(signature)) {
			xmlSignature.setDuplicated(true);
		}
		xmlSignature.setSignatureFilename(removeSpecialCharsForXml(signature.getSignatureFilename()));

		xmlSignature.setId(signature.getId());
		xmlSignature.setDAIdentifier(signature.getDAIdentifier());
		xmlSignature.setClaimedSigningTime(signature.getSigningTime());
		xmlSignature.setStructuralValidation(getXmlStructuralValidation(signature));
		xmlSignature.setSignatureFormat(signature.getDataFoundUpToLevel());

		xmlSignature.setSignatureProductionPlace(getXmlSignatureProductionPlace(signature.getSignatureProductionPlace()));
		xmlSignature.setCommitmentTypeIndication(getXmlCommitmentTypeIndication(signature.getCommitmentTypeIndication()));
		xmlSignature.getSignerRole().addAll(getXmlSignerRoles(signature.getSignerRoles()));

		xmlSignature.setContentType(signature.getContentType());
		xmlSignature.setMimeType(signature.getMimeType());
		xmlSignature.setContentIdentifier(signature.getContentIdentifier());
		xmlSignature.setContentHints(signature.getContentHints());

		final CandidatesForSigningCertificate candidatesForSigningCertificate = signature.getCandidatesForSigningCertificate();
		final CertificateValidity theCertificateValidity = candidatesForSigningCertificate.getTheCertificateValidity();
		PublicKey signingCertificatePublicKey = null;
		if (theCertificateValidity != null) {
			xmlSignature.setSigningCertificate(getXmlSigningCertificate(theCertificateValidity));
			signingCertificatePublicKey = theCertificateValidity.getPublicKey();
			xmlSignature.setCertificateChain(getXmlForCertificateChain(signingCertificatePublicKey));
		}
		xmlSignature.setBasicSignature(getXmlBasicSignature(signature, signingCertificatePublicKey));
		xmlSignature.setDigestMatchers(getXmlDigestMatchers(signature));

		xmlSignature.setPolicy(getXmlPolicy(signature));
		xmlSignature.setPDFRevision(getXmlPDFRevision(signature.getPdfRevision()));
		xmlSignature.setSignatureDigestReference(getXmlSignatureDigestReference(signature));
		
		xmlSignature.setSignerDocumentRepresentations(getXmlSignerDocumentRepresentations(signature));

		xmlSignature.setFoundCertificates(getXmlFoundCertificates(signature.getCertificateSource()));
		xmlSignature.setFoundRevocations(getXmlFoundRevocations(signature.getCRLSource(), signature.getOCSPSource()));
		
		xmlSignature.setSignatureScopes(getXmlSignatureScopes(signature.getSignatureScopes()));
		
		xmlSignature.setSignatureValue(signature.getSignatureValue());

		return xmlSignature;
	}
	
	private boolean hasDuplicate(AdvancedSignature currentSignature) {
		for (AdvancedSignature signature : signatures) {
			if (currentSignature != signature && currentSignature.getId().equals(signature.getId())) {
				return true;
			}
		}
		return false;
	}
	
	private XmlPDFRevision getXmlPDFRevision(PdfRevision pdfRevision) {
		if (pdfRevision != null) {
			XmlPDFRevision xmlPDFRevision = new XmlPDFRevision();
			xmlPDFRevision.getSignatureFieldName().addAll(pdfRevision.getFieldNames());
			// TODO : refactor when will divide DDB to submodules (XAdES, CAdES, PAdES ...)
			xmlPDFRevision.setSignerInformationStore(getXmlSignerInformationStore(pdfRevision));
			xmlPDFRevision.setPDFSignatureDictionary(getXmlPDFSignatureDictionary(pdfRevision.getPdfSigDictInfo()));
			return xmlPDFRevision;
		}
		return null;
	}
	
	private List<XmlSignerInfo> getXmlSignerInformationStore(PdfRevision pdfRevision) {
		Collection<SignerInfo> signerInformationStore = pdfRevision.getSignatureInformationStore();
		if (Utils.isCollectionNotEmpty(signerInformationStore)) {
			List<XmlSignerInfo> signerInfos = new ArrayList<>();
			for (SignerInfo signerInfo : signerInformationStore) {
				XmlSignerInfo xmlSignerInfo = new XmlSignerInfo();
				xmlSignerInfo.setIssuer(signerInfo.getIssuer());
				xmlSignerInfo.setSerialNumber(signerInfo.getSerialNumber());
				xmlSignerInfo.setProcessed(signerInfo.isValidated());
				signerInfos.add(xmlSignerInfo);
			}
			return signerInfos;
		}
		return null;
	}

	private XmlPDFSignatureDictionary getXmlPDFSignatureDictionary(PdfSignatureDictionary pdfSigDict) {
		if (pdfSigDict != null) {
			XmlPDFSignatureDictionary pdfSignatureDictionary = new XmlPDFSignatureDictionary();
			pdfSignatureDictionary.setSignerName(emptyToNull(pdfSigDict.getSignerName()));
			pdfSignatureDictionary.setType(emptyToNull(pdfSigDict.getType()));
			pdfSignatureDictionary.setFilter(emptyToNull(pdfSigDict.getFilter()));
			pdfSignatureDictionary.setSubFilter(emptyToNull(pdfSigDict.getSubFilter()));
			pdfSignatureDictionary.setContactInfo(emptyToNull(pdfSigDict.getContactInfo()));
			pdfSignatureDictionary.setReason(emptyToNull(pdfSigDict.getReason()));
			pdfSignatureDictionary.getSignatureByteRange().addAll(pdfSigDict.getByteRange().toBigIntegerList());
			return pdfSignatureDictionary;
		}
		return null;
	}

	private XmlSignatureDigestReference getXmlSignatureDigestReference(AdvancedSignature signature) {
		SignatureDigestReference signatureDigestReference = signature.getSignatureDigestReference(defaultDigestAlgorithm);
		if (signatureDigestReference != null) {
			XmlSignatureDigestReference xmlDigestReference = new XmlSignatureDigestReference();
			xmlDigestReference.setCanonicalizationMethod(signatureDigestReference.getCanonicalizationMethod());
			xmlDigestReference.setDigestMethod(signatureDigestReference.getDigestAlgorithm());
			xmlDigestReference.setDigestValue(signatureDigestReference.getDigestValue());
			return xmlDigestReference;
		}
		return null;
	}
	
	private XmlSignerDocumentRepresentations getXmlSignerDocumentRepresentations(AdvancedSignature signature) {
		if (signature.getDetachedContents() == null) {
			return null;
		}
		XmlSignerDocumentRepresentations signerDocumentRepresentation = new XmlSignerDocumentRepresentations();
		signerDocumentRepresentation.setDocHashOnly(signature.isDocHashOnlyValidation());
		signerDocumentRepresentation.setHashOnly(signature.isHashOnlyValidation());
		return signerDocumentRepresentation;
	}
	
	private XmlSignerData getXmlSignerData(SignatureScope signatureScope) {
		XmlSignerData xmlSignedData = new XmlSignerData();
		xmlSignedData.setId(signatureScope.getDSSIdAsString());
		xmlSignedData.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(signatureScope.getDigest().getAlgorithm(), 
				signatureScope.getDigest().getValue()));
		xmlSignedData.setReferencedName(signatureScope.getName());
		return xmlSignedData;
	}	

	private XmlStructuralValidation getXmlStructuralValidation(AdvancedSignature signature) {
		String structureValidationResult = signature.getStructureValidationResult();
		final XmlStructuralValidation xmlStructuralValidation = new XmlStructuralValidation();
		xmlStructuralValidation.setValid(Utils.isStringEmpty(structureValidationResult));
		if (Utils.isStringNotEmpty(structureValidationResult)) {
			xmlStructuralValidation.setMessage(structureValidationResult);
		}
		return xmlStructuralValidation;
	}

	/**
	 * Escape special characters which cause problems with jaxb or
	 * documentbuilderfactory and namespace aware mode
	 */
	private String removeSpecialCharsForXml(String text) {
		if (Utils.isStringNotEmpty(text)) {
			return text.replaceAll("&", "");
		}
		return Utils.EMPTY_STRING;
	}
	
	private List<XmlTimestamp> buildXmlTimestamps(Set<TimestampToken> timestamps) {
		List<XmlTimestamp> xmlTimestampsList = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(timestamps)) {
			List<TimestampToken> tokens = new ArrayList<>(timestamps);
			tokens.sort(Comparator.comparing(TimestampToken::getGenerationTime));
			for (TimestampToken timestampToken : tokens) {
				String id = timestampToken.getDSSIdAsString();
				XmlTimestamp xmlTimestamp = xmlTimestampsMap.get(id);
				if (xmlTimestamp == null) {
					xmlTimestamp = buildDetachedXmlTimestamp(timestampToken);
					xmlTimestampsMap.put(id, xmlTimestamp);
				}
				xmlTimestampsList.add(xmlTimestamp);
			}
		}
		return xmlTimestampsList;
	}
	
	private XmlOrphanTokens buildXmlOrphanTokens() {
		XmlOrphanTokens xmlOrphanTokens = new XmlOrphanTokens();
		if (Utils.isMapNotEmpty(xmlOrphanCertificateTokensMap)) {
			xmlOrphanTokens.getOrphanCertificates().addAll(xmlOrphanCertificateTokensMap.values());
		}
		if (Utils.isMapNotEmpty(xmlOrphanRevocationTokensMap)) {
			xmlOrphanTokens.getOrphanRevocations().addAll(xmlOrphanRevocationTokensMap.values());
		}
		return xmlOrphanTokens;
	}
	
	private XmlRevocation buildDetachedXmlRevocation(RevocationToken revocationToken) {

		final XmlRevocation xmlRevocation = new XmlRevocation();
		xmlRevocation.setId(revocationToken.getDSSIdAsString());
		
		if (isInternalOrigin(revocationToken)) {
			xmlRevocation.setOrigin(RevocationOrigin.INPUT_DOCUMENT);
		} else {
			xmlRevocation.setOrigin(revocationToken.getFirstOrigin());
		}
		xmlRevocation.setType(revocationToken.getRevocationType());

		xmlRevocation.setProductionDate(revocationToken.getProductionDate());
		xmlRevocation.setThisUpdate(revocationToken.getThisUpdate());
		xmlRevocation.setNextUpdate(revocationToken.getNextUpdate());
		xmlRevocation.setExpiredCertsOnCRL(revocationToken.getExpiredCertsOnCRL());
		xmlRevocation.setArchiveCutOff(revocationToken.getArchiveCutOff());

		String sourceURL = revocationToken.getSourceURL();
		if (Utils.isStringNotEmpty(sourceURL)) { // not empty = online
			xmlRevocation.setSourceAddress(sourceURL);
		}

		xmlRevocation.setBasicSignature(getXmlBasicSignature(revocationToken));

		xmlRevocation.setSigningCertificate(getXmlSigningCertificate(revocationToken.getPublicKeyOfTheSigner()));
		xmlRevocation.setCertificateChain(getXmlForCertificateChain(revocationToken.getPublicKeyOfTheSigner()));

		xmlRevocation.setCertHashExtensionPresent(revocationToken.isCertHashPresent());
		xmlRevocation.setCertHashExtensionMatch(revocationToken.isCertHashMatch());

		if (includeRawRevocationData) {
			xmlRevocation.setBase64Encoded(revocationToken.getEncoded());
		} else {
			byte[] revocationDigest = revocationToken.getDigest(defaultDigestAlgorithm);
			xmlRevocation.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(defaultDigestAlgorithm, revocationDigest));
		}

		return xmlRevocation;
	}
	
	private boolean isInternalOrigin(RevocationToken revocationToken) {
		for (RevocationOrigin origin : revocationToken.getOrigins()) {
			if (origin.isInternalOrigin()) {
				return true;
			}
		}
		return false;
	}

	private List<XmlChainItem> getXmlForCertificateChain(PublicKey certPubKey) {
		if (certPubKey != null) {
			final List<XmlChainItem> certChainTokens = new ArrayList<>();
			Set<CertificateToken> processedTokens = new HashSet<>();
			CertificateToken issuerToken = getCertificateByPubKey(certPubKey);
			while (issuerToken != null) {
				certChainTokens.add(getXmlChainItem(issuerToken));
				if (issuerToken.isSelfSigned() || processedTokens.contains(issuerToken)) {
					break;
				}
				processedTokens.add(issuerToken);
				issuerToken = getCertificateByPubKey(issuerToken.getPublicKeyOfTheSigner());
			}
			return certChainTokens;
		}
		return null;
	}

	private boolean isTrusted(CertificateToken cert) {
		if (Utils.isCollectionNotEmpty(trustedCertSources)) {
			for (CertificateSource trustedSource : trustedCertSources) {
				if (trustedSource.isTrusted(cert))
					return true;
			}
		}
		return false;
	}

	private XmlChainItem getXmlChainItem(final CertificateToken token) {
		final XmlChainItem chainItem = new XmlChainItem();
		chainItem.setCertificate(xmlCertsMap.get(token.getDSSIdAsString()));
		return chainItem;
	}

	/**
	 * This method creates the SigningCertificate element for the current token.
	 *
	 * @param token
	 *              the token
	 * @return
	 */
	private XmlSigningCertificate getXmlSigningCertificate(final PublicKey certPubKey) {
		final XmlSigningCertificate xmlSignCertType = new XmlSigningCertificate();
		final CertificateToken certificateByPubKey = getCertificateByPubKey(certPubKey);
		if (certificateByPubKey != null) {
			xmlSignCertType.setCertificate(xmlCertsMap.get(certificateByPubKey.getDSSIdAsString()));
		} else if (certPubKey != null) {
			xmlSignCertType.setPublicKey(certPubKey.getEncoded());
		} else {
			return null;
		}
		return xmlSignCertType;
	}

	private CertificateToken getCertificateByPubKey(final PublicKey certPubKey) {
		if (certPubKey == null) {
			return null;
		}

		List<CertificateToken> founds = new ArrayList<>();
		for (CertificateToken cert : usedCertificates) {
			if (certPubKey.equals(cert.getPublicKey())) {
				founds.add(cert);
				if (isTrusted(cert)) {
					return cert;
				}
			}
		}

		if (Utils.isCollectionNotEmpty(founds)) {
			return founds.iterator().next();
		}
		return null;
	}
	
	private XmlSigningCertificate getXmlSigningCertificate(final SignerInfo signerInfo) {
		final XmlSigningCertificate xmlSignCertType = new XmlSigningCertificate();
		final CertificateToken certificateBySignerInfo = getCertificateBySignerInfo(signerInfo);
		if (certificateBySignerInfo != null) {
			xmlSignCertType.setCertificate(xmlCertsMap.get(certificateBySignerInfo.getDSSIdAsString()));
		} else if (signerInfo != null) {
			// TODO: add info to xsd
		} else {
			return null;
		}
		return xmlSignCertType;
	}

	private CertificateToken getCertificateBySignerInfo(final SignerInfo signerInfo) {
		if (signerInfo == null) {
			return null;
		}

		List<CertificateToken> founds = new ArrayList<>();
		for (CertificateToken cert : usedCertificates) {
			if (signerInfo.getIssuer().equals(cert.getIssuerX500Principal().toString()) &&
					signerInfo.getSerialNumber().equals(cert.getSerialNumber())) {
				founds.add(cert);
				if (isTrusted(cert)) {
					return cert;
				}
			}
		}

		if (Utils.isCollectionNotEmpty(founds)) {
			return founds.iterator().next();
		}
		return null;
	}

	private XmlSigningCertificate getXmlSigningCertificate(CertificateValidity certificateValidity) {
		XmlSigningCertificate xmlSignCertType = new XmlSigningCertificate();
		CertificateToken signingCertificateToken = certificateValidity.getCertificateToken();
		if (signingCertificateToken != null) {
			xmlSignCertType.setCertificate(xmlCertsMap.get(signingCertificateToken.getDSSIdAsString()));
		} else if (certificateValidity.getPublicKey() != null) {
			XmlSigningCertificate xmlSignCert = getXmlSigningCertificate(certificateValidity.getPublicKey());
			if (xmlSignCert != null) {
				xmlSignCertType = xmlSignCert;
			}
		} else if (certificateValidity.getSignerInfo() != null) {
			XmlSigningCertificate xmlSignCertBySignInfo = getXmlSigningCertificate(certificateValidity.getSignerInfo());
			if (xmlSignCertBySignInfo != null) {
				xmlSignCertType = xmlSignCertBySignInfo;
			}
		}
		xmlSignCertType.setAttributePresent(certificateValidity.isAttributePresent());
		xmlSignCertType.setDigestValuePresent(certificateValidity.isDigestPresent());
		xmlSignCertType.setDigestValueMatch(certificateValidity.isDigestEqual());
		final boolean issuerSerialMatch = certificateValidity.isSerialNumberEqual() && certificateValidity.isDistinguishedNameEqual();
		xmlSignCertType.setIssuerSerialMatch(issuerSerialMatch);
		return xmlSignCertType;
	}

	private XmlSignatureProductionPlace getXmlSignatureProductionPlace(SignatureProductionPlace signatureProductionPlace) {
		if (signatureProductionPlace != null) {
			final XmlSignatureProductionPlace xmlSignatureProductionPlace = new XmlSignatureProductionPlace();
			xmlSignatureProductionPlace.setCountryName(emptyToNull(signatureProductionPlace.getCountryName()));
			xmlSignatureProductionPlace.setStateOrProvince(emptyToNull(signatureProductionPlace.getStateOrProvince()));
			xmlSignatureProductionPlace.setPostalCode(emptyToNull(signatureProductionPlace.getPostalCode()));
			xmlSignatureProductionPlace.setAddress(emptyToNull(signatureProductionPlace.getStreetAddress()));
			xmlSignatureProductionPlace.setCity(emptyToNull(signatureProductionPlace.getCity()));
			return xmlSignatureProductionPlace;
		}
		return null;
	}
	
	private List<XmlSignerRole> getXmlSignerRoles(Collection<SignerRole> signerRoles) {
		List<XmlSignerRole> xmlSignerRoles = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(signerRoles)) {
			for (SignerRole signerRole : signerRoles) {
				XmlSignerRole xmlSignerRole = new XmlSignerRole();
				xmlSignerRole.setRole(signerRole.getRole());
				xmlSignerRole.setCategory(signerRole.getCategory());
				xmlSignerRole.setNotBefore(signerRole.getNotBefore());
				xmlSignerRole.setNotAfter(signerRole.getNotAfter());
				xmlSignerRoles.add(xmlSignerRole);
			}
		}
		return xmlSignerRoles;
	}

	private List<String> getXmlCommitmentTypeIndication(CommitmentType commitmentTypeIndication) {
		if (commitmentTypeIndication != null) {
			return commitmentTypeIndication.getIdentifiers();
		}
		return Collections.emptyList();
	}

	private XmlDistinguishedName getXmlDistinguishedName(final String x500PrincipalFormat, final X500Principal X500PrincipalName) {
		final XmlDistinguishedName xmlDistinguishedName = new XmlDistinguishedName();
		xmlDistinguishedName.setFormat(x500PrincipalFormat);
		xmlDistinguishedName.setValue(X500PrincipalName.getName(x500PrincipalFormat));
		return xmlDistinguishedName;
	}

	private XmlFoundCertificates getXmlFoundCertificates(SignatureCertificateSource certificateSource) {
		XmlFoundCertificates xmlFoundCertificates = new XmlFoundCertificates();
		xmlFoundCertificates.getRelatedCertificates().addAll(getXmlRelatedCertificates(certificateSource));
		xmlFoundCertificates.getRelatedCertificates().addAll(getXmlRelatedCertificateForOrphanReferences(certificateSource));
		xmlFoundCertificates.getOrphanCertificates().addAll(getOrphanCertificates(certificateSource));
		return xmlFoundCertificates;
	}
	
	private List<XmlRelatedCertificate> getXmlRelatedCertificates(SignatureCertificateSource certificateSource) {
		Map<String, XmlRelatedCertificate> relatedCertificatesMap = new HashMap<>();
		
		populateCertificateOriginMap(relatedCertificatesMap, CertificateOrigin.KEY_INFO, 
				certificateSource.getKeyInfoCertificates(), certificateSource);
		populateCertificateOriginMap(relatedCertificatesMap, CertificateOrigin.CMS_SIGNED_DATA, 
				certificateSource.getCMSSignedDataCertificates(), certificateSource);
		populateCertificateOriginMap(relatedCertificatesMap, CertificateOrigin.CERTIFICATE_VALUES, 
				certificateSource.getCertificateValues(), certificateSource);
		populateCertificateOriginMap(relatedCertificatesMap, CertificateOrigin.ATTR_AUTORITIES_CERT_VALUES, 
				certificateSource.getAttrAuthoritiesCertValues(), certificateSource);
		populateCertificateOriginMap(relatedCertificatesMap, CertificateOrigin.TIMESTAMP_VALIDATION_DATA, 
				certificateSource.getTimeStampValidationDataCertValues(), certificateSource);
		populateCertificateOriginMap(relatedCertificatesMap, CertificateOrigin.DSS_DICTIONARY, 
				certificateSource.getDSSDictionaryCertValues(), certificateSource);
		populateCertificateOriginMap(relatedCertificatesMap, CertificateOrigin.VRI_DICTIONARY, 
				certificateSource.getVRIDictionaryCertValues(), certificateSource);
		
		return new ArrayList<>(relatedCertificatesMap.values());
	}
	
	private void populateCertificateOriginMap(Map<String, XmlRelatedCertificate> relatedCertificatesMap, CertificateOrigin origin,
			List<CertificateToken> certificateTokens, SignatureCertificateSource certificateSource) {
		for (CertificateToken certificateToken : certificateTokens) {
			if (!relatedCertificatesMap.containsKey(certificateToken.getDSSIdAsString())) {
				XmlRelatedCertificate xmlFoundCertificate = getXmlRelatedCertificate(origin, certificateToken, certificateSource);
				relatedCertificatesMap.put(certificateToken.getDSSIdAsString(), xmlFoundCertificate);
			} else {
				XmlRelatedCertificate storedFoundCertificate = relatedCertificatesMap.get(certificateToken.getDSSIdAsString());
				if (!storedFoundCertificate.getOrigins().contains(origin)) {
					storedFoundCertificate.getOrigins().add(origin);
				}
			}
		}
	}
	
	private XmlRelatedCertificate getXmlRelatedCertificate(CertificateOrigin origin, CertificateToken cert, SignatureCertificateSource certificateSource) {
		XmlRelatedCertificate xrc = new XmlRelatedCertificate();
		xrc.getOrigins().add(origin);
		xrc.setCertificate(xmlCertsMap.get(cert.getDSSIdAsString()));
		List<CertificateRef> referencesForCertificateToken = certificateSource.getReferencesForCertificateToken(cert);
		for (CertificateRef certificateRef : referencesForCertificateToken) {
			xrc.getCertificateRefs().add(getXmlCertificateRef(certificateRef));
			referenceMap.put(certificateRef.getDSSIdAsString(), cert.getDSSIdAsString());
		}		
		return xrc;
	}
	
	private XmlCertificateRef getXmlCertificateRef(CertificateRef ref) {
		XmlCertificateRef certificateRef = new XmlCertificateRef();
		IssuerSerialInfo serialInfo = ref.getIssuerInfo();
		if (serialInfo != null && serialInfo.getIssuerName() != null && serialInfo.getSerialNumber() != null) {
			IssuerSerial issuerSerial = DSSASN1Utils.getIssuerSerial(serialInfo.getIssuerName(), serialInfo.getSerialNumber());
			certificateRef.setIssuerSerial(DSSASN1Utils.getDEREncoded(issuerSerial));
		}
		Digest refDigest = ref.getCertDigest();
		if (refDigest != null) {
			certificateRef.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(refDigest.getAlgorithm(), refDigest.getValue()));
		}
		certificateRef.setOrigin(ref.getOrigin());
		return certificateRef;
	}
	
	private List<XmlRelatedCertificate> getXmlRelatedCertificateForOrphanReferences(SignatureCertificateSource certificateSource) {
		List<XmlRelatedCertificate> relatedCertificates = new ArrayList<>();
		for (CertificateRef certificateRef : certificateSource.getOrphanCertificateRefs()) {
			Digest certRefDigest = certificateRef.getCertDigest();
			CertificateToken certificateToken = commonCertificateSource.getCertificateTokenByDigest(certRefDigest);
			if (certificateToken != null) {
				relatedCertificates.add(getXmlRelatedCertificate(certificateToken, certificateRef));
			}
		}
		return relatedCertificates;
	}
	
	private XmlRelatedCertificate getXmlRelatedCertificate(CertificateToken cert, CertificateRef certificateRef) {
		XmlRelatedCertificate xrc = new XmlRelatedCertificate();
		xrc.setCertificate(xmlCertsMap.get(cert.getDSSIdAsString()));
		if (getXmlCertificateSources(cert).contains(CertificateSourceType.TIMESTAMP)) {
			xrc.getOrigins().add(CertificateOrigin.TIMESTAMP_CERTIFICATE_VALUES);
		}
		xrc.getCertificateRefs().add(getXmlCertificateRef(certificateRef));
		referenceMap.put(certificateRef.getDSSIdAsString(), cert.getDSSIdAsString());
		return xrc;
	}
	
	private List<XmlOrphanCertificate> getOrphanCertificates(SignatureCertificateSource certificateSource) {
		List<XmlOrphanCertificate> orphanCertificates = new ArrayList<>();
		
		// Orphan Certificate Tokens
		for (CertificateToken certificateToken : certificateSource.getCertificates()) {
			if (!usedCertificates.contains(certificateToken)) {
				orphanCertificates.add(createXmlOrphanCertificate(certificateToken, false));
			}
		}

		// Orphan Certificate References
		List<CertificateRef> orphanCertificateRefs = certificateSource.getOrphanCertificateRefs();
		for (CertificateRef orphanCertificateRef : orphanCertificateRefs) {
			if (commonCertificateSource.getCertificateTokenByDigest(orphanCertificateRef.getCertDigest()) == null) {
				orphanCertificates.add(createXmlOrphanCertificate(orphanCertificateRef));
			}
		}
		
		return orphanCertificates;
	}
	
	private XmlOrphanCertificate createXmlOrphanCertificate(CertificateToken certificateToken, boolean foundInTimestamp) {
		XmlOrphanCertificate orphanCertificate = new XmlOrphanCertificate();
		if (foundInTimestamp || getXmlCertificateSources(certificateToken).contains(CertificateSourceType.TIMESTAMP)) {
			orphanCertificate.getOrigins().add(CertificateOrigin.TIMESTAMP_CERTIFICATE_VALUES);
		}
		orphanCertificate.setToken(createXmlOrphanCertificateToken(certificateToken));
		return orphanCertificate;
	}
	
	private XmlOrphanCertificate createXmlOrphanCertificate(CertificateRef orphanCertificateRef) {
		XmlOrphanCertificate orphanCertificate = new XmlOrphanCertificate();
		orphanCertificate.setToken(createXmlOrphanCertificateToken(orphanCertificateRef));
		orphanCertificate.getCertificateRefs().add(getXmlCertificateRef(orphanCertificateRef));
		return orphanCertificate;
	}
	
	private XmlOrphanCertificateToken createXmlOrphanCertificateToken(CertificateToken certificateToken) {
		XmlOrphanCertificateToken orphanToken = new XmlOrphanCertificateToken();
		orphanToken.setId(certificateToken.getDSSIdAsString());
		orphanToken.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(defaultDigestAlgorithm, certificateToken.getDigest(defaultDigestAlgorithm)));
		xmlOrphanCertificateTokensMap.put(certificateToken.getDSSIdAsString(), orphanToken);
		return orphanToken;
	}
	
	private XmlOrphanCertificateToken createXmlOrphanCertificateToken(CertificateRef orphanCertificateRef) {
		XmlOrphanCertificateToken orphanToken = new XmlOrphanCertificateToken();
		orphanToken.setId(orphanCertificateRef.getDSSIdAsString());
		orphanToken.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(orphanCertificateRef.getCertDigest()));
		xmlOrphanCertificateTokensMap.put(orphanCertificateRef.getDSSIdAsString(), orphanToken);
		return orphanToken;
	}

	private List<XmlFoundTimestamp> getXmlFoundTimestamps(AdvancedSignature signature) {
		List<XmlFoundTimestamp> foundTimestamps = new ArrayList<>();
		for (TimestampToken timestampToken : signature.getAllTimestamps()) {
			XmlFoundTimestamp foundTimestamp = new XmlFoundTimestamp();
			foundTimestamp.setTimestamp(xmlTimestampsMap.get(timestampToken.getDSSIdAsString()));
			foundTimestamp.setLocation(timestampToken.getTimestampLocation());
			foundTimestamps.add(foundTimestamp);
		}
		return foundTimestamps;
	}
	
	private XmlFoundRevocations getXmlFoundRevocations(SignatureCRLSource crlSource, SignatureOCSPSource ocspSource) {
		XmlFoundRevocations foundRevocations = new XmlFoundRevocations();
		// revocation refs processed inside
		foundRevocations.getRelatedRevocations().addAll(getXmlRelatedRevocations(crlSource, ocspSource));

		List<EncapsulatedRevocationTokenIdentifier> orphanRevocations = getOrphanRevocations(crlSource, ocspSource);
		
		if (Utils.isCollectionNotEmpty(orphanRevocations)) {
			foundRevocations.getOrphanRevocations().addAll(getXmlOrphanRevocations(crlSource, ocspSource, orphanRevocations));
		}
		
		List<RevocationRef> orphanRevocationRefs = getOrphanRevocationRefs(crlSource, ocspSource);
		for (RevocationRef leftRevocationRef : orphanRevocationRefs) {
			XmlOrphanRevocation revocationFromRef = createOrphanRevocationFromRef(leftRevocationRef);
			foundRevocations.getOrphanRevocations().add(revocationFromRef);
		}
		return foundRevocations;
	}
	
	private List<EncapsulatedRevocationTokenIdentifier> getOrphanRevocations(SignatureCRLSource crlSource, SignatureOCSPSource ocspSource) {
		List<EncapsulatedRevocationTokenIdentifier> orphanIdentifiers = new ArrayList<>();
		
		List<EncapsulatedRevocationTokenIdentifier> revocationIdentifiers = new ArrayList<>();
		revocationIdentifiers.addAll(crlSource.getCRLBinaryList());
		revocationIdentifiers.addAll(ocspSource.getOCSPResponsesList());
		
		for (EncapsulatedRevocationTokenIdentifier revocationIdentifier : revocationIdentifiers) {
			if (!xmlRevocationsMap.containsKey(revocationIdentifier.asXmlId())) {
				orphanIdentifiers.add(revocationIdentifier);
			}
		}
		return orphanIdentifiers;
	}
	
	private List<RevocationRef> getOrphanRevocationRefs(SignatureCRLSource crlSource, SignatureOCSPSource ocspSource) {
		List<RevocationRef> orphanRevocationRefs = new ArrayList<>();
		for (CRLRef crlRef : crlSource.getOrphanCrlRefs()) {
			if (commonCRLSource.getIdentifier(crlRef.getDigest()) == null) {
				orphanRevocationRefs.add(crlRef);
			}
		}
		for (OCSPRef ocspRef : ocspSource.getOrphanOCSPRefs()) {
			if (commonOCSPSource.getIdentifier(ocspRef.getDigest()) == null) {
				orphanRevocationRefs.add(ocspRef);
			}
		}
		return orphanRevocationRefs;
	}

	private List<XmlRelatedRevocation> getXmlRelatedRevocations(SignatureCRLSource crlSource, SignatureOCSPSource ocspSource) {
		List<RevocationToken> revocationTokens = new ArrayList<>();
		revocationTokens.addAll(crlSource.getAllCRLTokens());
		revocationTokens.addAll(ocspSource.getAllOCSPTokens());
		
		return getXmlRevocationsByType(crlSource, ocspSource, revocationTokens);
	}

	private List<XmlRelatedRevocation> getXmlRevocationsByType(SignatureCRLSource crlSource, SignatureOCSPSource ocspSource, 
			List<RevocationToken> revocationTokens) {
		
		List<XmlRelatedRevocation> xmlRelatedRevocations = new ArrayList<>();
		Set<String> revocationKeys = new HashSet<>();
		for (RevocationToken revocationToken : revocationTokens) {
			String id = revocationToken.getDSSIdAsString();
			if (!revocationKeys.contains(id)) {
				XmlRevocation xmlRevocation = xmlRevocationsMap.get(id);
				if (xmlRevocation != null) {
					XmlRelatedRevocation xmlRelatedRevocation = new XmlRelatedRevocation();
					xmlRelatedRevocation.setRevocation(xmlRevocation);
					xmlRelatedRevocation.setType(revocationToken.getRevocationType());
					xmlRelatedRevocation.getOrigins().addAll(revocationToken.getOrigins());
					List<RevocationRef> revocationRefs = findRefsForRevocationToken(crlSource, ocspSource, revocationToken);
					if (Utils.isCollectionNotEmpty(revocationRefs)) {
						xmlRelatedRevocation.getRevocationRefs().addAll(getXmlRevocationRefs(revocationRefs));
						for (RevocationRef ref : revocationRefs) {
							referenceMap.put(ref.getDSSIdAsString(), revocationToken.getDSSIdAsString());
						}
					}

					xmlRelatedRevocations.add(xmlRelatedRevocation);
					revocationKeys.add(id);
				}
			}
		}
		return xmlRelatedRevocations;
	}
	
	private List<RevocationRef> findRefsForRevocationToken(SignatureCRLSource listCRLSource, SignatureOCSPSource listOCSPSource, RevocationToken revocationToken) {
		List<RevocationRef> revocationRefs = new ArrayList<>();
		if (RevocationType.CRL.equals(revocationToken.getRevocationType())) {
			revocationRefs.addAll(listCRLSource.findRefsForRevocationToken((CRLToken) revocationToken));
		} else {
			revocationRefs.addAll(listOCSPSource.findRefsForRevocationToken((OCSPToken) revocationToken));
		}
		return revocationRefs;
	}

	private List<XmlRevocationRef> getXmlRevocationRefs(List<RevocationRef> revocationRefs) {
		List<XmlRevocationRef> xmlRevocationRefs = new ArrayList<>();
		for (RevocationRef ref : revocationRefs) {
			XmlRevocationRef revocationRef;
			if (ref instanceof CRLRef) {
				revocationRef = getXmlCRLRevocationRef((CRLRef) ref);
			} else {
				revocationRef = getXmlOCSPRevocationRef((OCSPRef) ref);
			}
			xmlRevocationRefs.add(revocationRef);
		}
		return xmlRevocationRefs;
	}
	
	private XmlRevocationRef getXmlCRLRevocationRef(CRLRef crlRef) {
		XmlRevocationRef xmlRevocationRef = new XmlRevocationRef();
		xmlRevocationRef.getOrigins().addAll(crlRef.getOrigins());
		if (crlRef.getDigest() != null) {
			xmlRevocationRef.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(crlRef.getDigest()));
		}
		return xmlRevocationRef;
	}
	
	private XmlRevocationRef getXmlOCSPRevocationRef(OCSPRef ocspRef) {
		XmlRevocationRef xmlRevocationRef = new XmlRevocationRef();
		xmlRevocationRef.getOrigins().addAll(ocspRef.getOrigins());
		if (ocspRef.getDigest() != null) {
			xmlRevocationRef.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(ocspRef.getDigest()));
		}
		xmlRevocationRef.setProducedAt(ocspRef.getProducedAt());
		ResponderId responderId = ocspRef.getResponderId();
		if (responderId != null) {
			String name = responderId.getName();
			if (Utils.isStringNotEmpty(name)) {
				xmlRevocationRef.setResponderIdName(name);
			}
			byte[] key = responderId.getKey();
			if (Utils.isArrayNotEmpty(key)) {
				xmlRevocationRef.setResponderIdKey(key);
			}
		}
		return xmlRevocationRef;
	}
	
	private List<XmlOrphanRevocation> getXmlOrphanRevocations(SignatureCRLSource crlSource, SignatureOCSPSource ocspSource, 
			Collection<EncapsulatedRevocationTokenIdentifier> orphanRevocations) {
		List<XmlOrphanRevocation> xmlOrphanRevocations = new ArrayList<>();
		for (EncapsulatedRevocationTokenIdentifier revocationIdentifier : orphanRevocations) {
			xmlOrphanRevocations.add(getXmlOrphanRevocation(crlSource, ocspSource, revocationIdentifier));
		}
		return xmlOrphanRevocations;
	}
	
	private XmlOrphanRevocation getXmlOrphanRevocation(SignatureCRLSource crlSource, SignatureOCSPSource ocspSource, 
			EncapsulatedRevocationTokenIdentifier revocationIdentifier) {
		XmlOrphanRevocation xmlOrphanRevocation = new XmlOrphanRevocation();
		XmlOrphanRevocationToken orphanRevocationToken = createOrphanTokenFromRevocationIdentifier(revocationIdentifier);
		xmlOrphanRevocation.setToken(orphanRevocationToken);
		if (revocationIdentifier instanceof CRLBinary) {
			orphanRevocationToken.setType(RevocationType.CRL);
			xmlOrphanRevocation.setType(RevocationType.CRL);
			for (RevocationOrigin origin : crlSource.getRevocationOrigins((CRLBinary) revocationIdentifier)) {
				xmlOrphanRevocation.getOrigins().add(origin);
			}
		} else {
			orphanRevocationToken.setType(RevocationType.OCSP);
			xmlOrphanRevocation.setType(RevocationType.OCSP);
			for (RevocationOrigin origin : ocspSource.getRevocationOrigins((OCSPResponseBinary) revocationIdentifier)) {
				xmlOrphanRevocation.getOrigins().add(origin);
			}
		}
		List<RevocationRef> refsForRevocationToken = findRefsForRevocationIdentifier(crlSource, ocspSource, revocationIdentifier);
		for (RevocationRef revocationRef : refsForRevocationToken) {
			xmlOrphanRevocation.getRevocationRefs().add(revocationRefToXml(revocationRef));
			referenceMap.put(revocationRef.getDSSIdAsString(), revocationIdentifier.asXmlId());
		}
		
		return xmlOrphanRevocation;
	}
	
	private List<RevocationRef> findRefsForRevocationIdentifier(SignatureCRLSource listCRLSource, SignatureOCSPSource listOCSPSource,
			EncapsulatedRevocationTokenIdentifier revocationIdentifier) {
		List<RevocationRef> revocationRefs = new ArrayList<>();
		if (revocationIdentifier instanceof CRLBinary) {
			revocationRefs.addAll(listCRLSource.getReferencesForCRLIdentifier((CRLBinary) revocationIdentifier));
		} else {
			revocationRefs.addAll(listOCSPSource.getReferencesForOCSPIdentifier((OCSPResponseBinary) revocationIdentifier));
		}
		return revocationRefs;
	}
	
	private XmlRevocationRef revocationRefToXml(RevocationRef ref) {
		XmlRevocationRef xmlRevocationRef;
		if (ref instanceof CRLRef) {
			xmlRevocationRef = getXmlCRLRevocationRef((CRLRef) ref);
		} else {
			xmlRevocationRef = getXmlOCSPRevocationRef((OCSPRef) ref);
		}
		return xmlRevocationRef;
	}
	
	private XmlOrphanRevocationToken createOrphanTokenFromRevocationIdentifier(EncapsulatedRevocationTokenIdentifier revocationIdentifier) {
		XmlOrphanRevocationToken orphanToken = new XmlOrphanRevocationToken();
		String tokenId = revocationIdentifier.asXmlId();
		orphanToken.setId(tokenId);
		if (includeRawRevocationData) {
			orphanToken.setBase64Encoded(revocationIdentifier.getBinaries());
		} else {
			byte[] digestValue = revocationIdentifier.getDigestValue(defaultDigestAlgorithm);
			orphanToken.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(defaultDigestAlgorithm, digestValue));
		}
		xmlOrphanRevocationTokensMap.put(tokenId, orphanToken);
		return orphanToken;
	}
	
	private XmlOrphanRevocation createOrphanRevocationFromRef(RevocationRef ref) {
		XmlOrphanRevocation xmlOrphanRevocation = new XmlOrphanRevocation();
		
		XmlOrphanRevocationToken orphanToken = new XmlOrphanRevocationToken();
		orphanToken.setId(ref.getDSSIdAsString());
		orphanToken.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(ref.getDigest()));
		xmlOrphanRevocationTokensMap.put(ref.getDSSIdAsString(), orphanToken);
		
		xmlOrphanRevocation.setToken(orphanToken);
		if (ref instanceof CRLRef) {
			orphanToken.setType(RevocationType.CRL);
			xmlOrphanRevocation.setType(RevocationType.CRL);
		} else {
			orphanToken.setType(RevocationType.OCSP);
			xmlOrphanRevocation.setType(RevocationType.OCSP);
		}
		xmlOrphanRevocation.getRevocationRefs().add(revocationRefToXml(ref));
		return xmlOrphanRevocation;
	}

	/**
	 * This method deals with the signature policy. The retrieved information is
	 * transformed to the JAXB object.
	 *
	 * @param signaturePolicy
	 *                        The Signature Policy
	 * 
	 */
	private XmlPolicy getXmlPolicy(AdvancedSignature signature) {
		SignaturePolicy signaturePolicy = signature.getPolicyId();
		if (signaturePolicy == null) {
			return null;
		}

		final XmlPolicy xmlPolicy = new XmlPolicy();

		xmlPolicy.setId(signaturePolicy.getIdentifier());
		xmlPolicy.setUrl(signaturePolicy.getUrl());
		xmlPolicy.setDescription(signaturePolicy.getDescription());
		xmlPolicy.setNotice(signaturePolicy.getNotice());
		xmlPolicy.setZeroHash(signaturePolicy.isZeroHash());

		final Digest digest = signaturePolicy.getDigest();
		if (digest != null) {
			xmlPolicy.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(digest));
		}

		try {
			SignaturePolicyValidator validator = null;
			ServiceLoader<SignaturePolicyValidator> loader = ServiceLoader.load(SignaturePolicyValidator.class);
			Iterator<SignaturePolicyValidator> validatorOptions = loader.iterator();

			if (validatorOptions.hasNext()) {
				for (SignaturePolicyValidator signaturePolicyValidator : loader) {
					signaturePolicyValidator.setSignature(signature);
					if (signaturePolicyValidator.canValidate()) {
						validator = signaturePolicyValidator;
						break;
					}
				}
			}

			if (validator == null) {
				// if not empty and no other implementation is found for ASN1 signature policies
				validator = new BasicASNSignaturePolicyValidator();
				validator.setSignature(signature);
			}

			validator.validate();
			xmlPolicy.setAsn1Processable(validator.isAsn1Processable());
			if (!signaturePolicy.isZeroHash()) {
				xmlPolicy.setDigestAlgorithmsEqual(validator.isDigestAlgorithmsEqual());
			}
			xmlPolicy.setIdentified(validator.isIdentified());
			xmlPolicy.setStatus(validator.isStatus());
			if (Utils.isStringNotBlank(validator.getProcessingErrors())) {
				xmlPolicy.setProcessingError(validator.getProcessingErrors());
			}
		} catch (Exception e) {
			// When any error (communication) we just set the status to false
			xmlPolicy.setStatus(false);
			xmlPolicy.setProcessingError(e.getMessage());
			// Do nothing
			String errorMessage = "An error occurred during validation a signature policy with id '{}'. Reason : [{}]";
			if (LOG.isDebugEnabled()) {
				LOG.error(errorMessage, signaturePolicy.getIdentifier(), e.getMessage(), e);
			} else {
				LOG.error(errorMessage, signaturePolicy.getIdentifier(), e.getMessage());
			}
		}
		return xmlPolicy;
	}

	private XmlTimestamp buildDetachedXmlTimestamp(final TimestampToken timestampToken) {

		final XmlTimestamp xmlTimestampToken = new XmlTimestamp();

		xmlTimestampToken.setId(timestampToken.getDSSIdAsString());
		xmlTimestampToken.setType(timestampToken.getTimeStampType());
		xmlTimestampToken.setArchiveTimestampType(timestampToken.getArchiveTimestampType()); // property is defined only for archival timestamps
		xmlTimestampToken.setProductionTime(timestampToken.getGenerationTime());
		xmlTimestampToken.setTimestampFilename(timestampToken.getFileName());
		xmlTimestampToken.getDigestMatchers().addAll(getXmlDigestMatchers(timestampToken));
		xmlTimestampToken.setBasicSignature(getXmlBasicSignature(timestampToken));
		xmlTimestampToken.setPDFRevision(getXmlPDFRevision(timestampToken.getPdfRevision())); // used only for PAdES RFC 3161 timestamps
		
		xmlTimestampToken.setFoundCertificates(getXmlFoundCertificates(timestampToken.getCertificateSource()));
		xmlTimestampToken.setFoundRevocations(getXmlFoundRevocations(timestampToken.getCRLSource(), timestampToken.getOCSPSource()));

		xmlTimestampToken.setSigningCertificate(getXmlSigningCertificate(timestampToken.getPublicKeyOfTheSigner()));
		xmlTimestampToken.setCertificateChain(getXmlForCertificateChain(timestampToken.getPublicKeyOfTheSigner()));

		if (includeRawTimestampTokens) {
			xmlTimestampToken.setBase64Encoded(timestampToken.getEncoded());
		} else {
			byte[] certDigest = timestampToken.getDigest(defaultDigestAlgorithm);
			xmlTimestampToken.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(defaultDigestAlgorithm, certDigest));
		}

		return xmlTimestampToken;
	}
	
	private List<XmlDigestMatcher> getXmlDigestMatchers(TimestampToken timestampToken) {
		List<XmlDigestMatcher> digestMatchers = new ArrayList<>();
		digestMatchers.add(getImprintDigestMatcher(timestampToken));
		digestMatchers.addAll(getManifestEntriesDigestMatchers(timestampToken.getManifestFile()));
		return digestMatchers;
	}

	private XmlDigestMatcher getImprintDigestMatcher(TimestampToken timestampToken) {
		XmlDigestMatcher digestMatcher = new XmlDigestMatcher();
		digestMatcher.setType(DigestMatcherType.MESSAGE_IMPRINT);
		Digest messageImprint = timestampToken.getMessageImprint();
		if (messageImprint != null) {
			digestMatcher.setDigestMethod(messageImprint.getAlgorithm());
			digestMatcher.setDigestValue(messageImprint.getValue());
		}
		digestMatcher.setDataFound(timestampToken.isMessageImprintDataFound());
		digestMatcher.setDataIntact(timestampToken.isMessageImprintDataIntact());
		ManifestFile manifestFile = timestampToken.getManifestFile();
		if (manifestFile != null) {
			digestMatcher.setName(manifestFile.getFilename());
		}
		return digestMatcher;
	}
	
	private List<XmlDigestMatcher> getManifestEntriesDigestMatchers(ManifestFile manifestFile) {
		List<XmlDigestMatcher> digestMatchers = new ArrayList<>();
		if (manifestFile != null && Utils.isCollectionNotEmpty(manifestFile.getEntries())) {
			for (ManifestEntry entry : manifestFile.getEntries()) {
				XmlDigestMatcher digestMatcher = new XmlDigestMatcher();
				digestMatcher.setType(DigestMatcherType.MANIFEST_ENTRY);
				Digest digest = entry.getDigest();
				if (digest != null) {
					digestMatcher.setDigestMethod(digest.getAlgorithm());
					digestMatcher.setDigestValue(digest.getValue());
				}
				digestMatcher.setDataFound(entry.isFound());
				digestMatcher.setDataIntact(entry.isIntact());
				digestMatcher.setName(entry.getFileName());
				
				digestMatchers.add(digestMatcher);
			}
		}
		return digestMatchers;
	}

	private List<XmlTimestampedObject> getXmlTimestampedObjects(TimestampToken timestampToken) {
		List<TimestampedReference> timestampReferences = timestampToken.getTimestampedReferences();
		if (Utils.isCollectionNotEmpty(timestampReferences)) {
			List<XmlTimestampedObject> objects = new ArrayList<>();
			Set<String> addedTokenIds = new HashSet<>();
			for (final TimestampedReference timestampReference : timestampReferences) {
				String id = timestampReference.getObjectId();
				
				XmlTimestampedObject timestampedObject = createXmlTimestampedObject(timestampReference);
				if (timestampedObject.getToken() == null) {
					throw new DSSException(String.format("Token with Id '%s' not found", id));
				}
				id = timestampedObject.getToken().getId(); // can change in case of ref
				if (addedTokenIds.contains(id)) {
					// skip the ref if it was added before
					continue;
				}
				addedTokenIds.add(id);
				
				objects.add(timestampedObject);
			}
			return objects;
		}
		return null;
	}

	private XmlTimestampedObject createXmlTimestampedObject(final TimestampedReference timestampReference) {
		XmlTimestampedObject timestampedObj = new XmlTimestampedObject();
		timestampedObj.setCategory(timestampReference.getCategory());

		String objectId = timestampReference.getObjectId();
		
		switch (timestampReference.getCategory()) {
			case SIGNATURE:
				timestampedObj.setToken(xmlSignaturesMap.get(objectId));
				return timestampedObj;
				
			case CERTIFICATE:
				if (!isUsedToken(objectId, usedCertificates)) {
					String relatedCertificateId = referenceMap.get(objectId);
					if (relatedCertificateId != null) {
						objectId = relatedCertificateId;
						if (!isUsedToken(objectId, usedCertificates)) {
							break; // break to create an orphan token
						}
					} else {
						break;
					}
				}
				timestampedObj.setToken(xmlCertsMap.get(objectId));
				return timestampedObj;
				
			case REVOCATION:
				if (!isUsedToken(objectId, usedRevocations)) {
					String relatedRevocationId = referenceMap.get(objectId);
					if (relatedRevocationId != null) {
						objectId = relatedRevocationId;
						if (!isUsedToken(objectId, usedRevocations)) {
							break; // break to create an orphan token
						}
					} else {
						break;
					}
				}
				timestampedObj.setToken(xmlRevocationsMap.get(objectId));
				return timestampedObj;
				
			case TIMESTAMP:
				timestampedObj.setToken(xmlTimestampsMap.get(objectId));
				return timestampedObj;
				
			case SIGNED_DATA:
				timestampedObj.setToken(xmlSignedDataMap.get(objectId));
				return timestampedObj;
				
			default:
				throw new DSSException("Unsupported category " + timestampReference.getCategory());
				
		}
		
		if (TimestampedObjectType.CERTIFICATE.equals(timestampedObj.getCategory())) {
			timestampedObj.setToken(xmlOrphanCertificateTokensMap.get(objectId));
			timestampedObj.setCategory(TimestampedObjectType.ORPHAN_CERTIFICATE);
			
		} else if (TimestampedObjectType.REVOCATION.equals(timestampedObj.getCategory())) {
			timestampedObj.setToken(xmlOrphanRevocationTokensMap.get(objectId));
			timestampedObj.setCategory(TimestampedObjectType.ORPHAN_REVOCATION);
			
		} else {
			throw new DSSException(String.format("The type of object [%s] is not supported for Orphan Tokens!", timestampedObj.getCategory()));
			
		}
		
		return timestampedObj;
	}
	
	private <T extends Token> boolean isUsedToken(String tokenId, Collection<T> usedTokens) {
		for (Token token : usedTokens) {
			if (token.getDSSIdAsString().equals(tokenId)) {
				return true;
			}
		}
		return false;
	}

	private XmlBasicSignature getXmlBasicSignature(final Token token) {
		final XmlBasicSignature xmlBasicSignatureType = new XmlBasicSignature();

		SignatureAlgorithm signatureAlgorithm = token.getSignatureAlgorithm();
		if (signatureAlgorithm != null) {
			xmlBasicSignatureType.setEncryptionAlgoUsedToSignThisToken(signatureAlgorithm.getEncryptionAlgorithm());
			xmlBasicSignatureType.setDigestAlgoUsedToSignThisToken(signatureAlgorithm.getDigestAlgorithm());
			xmlBasicSignatureType.setMaskGenerationFunctionUsedToSignThisToken(signatureAlgorithm.getMaskGenerationFunction());
		}
		xmlBasicSignatureType.setKeyLengthUsedToSignThisToken(DSSPKUtils.getPublicKeySize(token));

		SignatureValidity signatureValidity = token.getSignatureValidity();
		if (SignatureValidity.NOT_EVALUATED != signatureValidity) {
			final boolean signatureValid = SignatureValidity.VALID == token.getSignatureValidity();
			xmlBasicSignatureType.setSignatureIntact(signatureValid);
			xmlBasicSignatureType.setSignatureValid(signatureValid);
		}
		return xmlBasicSignatureType;
	}

	private XmlBasicSignature getXmlBasicSignature(AdvancedSignature signature, PublicKey signingCertificatePublicKey) {
		XmlBasicSignature xmlBasicSignature = new XmlBasicSignature();
		xmlBasicSignature.setEncryptionAlgoUsedToSignThisToken(signature.getEncryptionAlgorithm());

		final int keyLength = signingCertificatePublicKey == null ? 0 : DSSPKUtils.getPublicKeySize(signingCertificatePublicKey);
		xmlBasicSignature.setKeyLengthUsedToSignThisToken(String.valueOf(keyLength));
		xmlBasicSignature.setDigestAlgoUsedToSignThisToken(signature.getDigestAlgorithm());
		xmlBasicSignature.setMaskGenerationFunctionUsedToSignThisToken(signature.getMaskGenerationFunction());

		SignatureCryptographicVerification scv = signature.getSignatureCryptographicVerification();
		xmlBasicSignature.setSignatureIntact(scv.isSignatureIntact());
		xmlBasicSignature.setSignatureValid(scv.isSignatureValid());
		return xmlBasicSignature;
	}

	private List<XmlDigestMatcher> getXmlDigestMatchers(AdvancedSignature signature) {
		List<XmlDigestMatcher> refs = new ArrayList<>();
		List<ReferenceValidation> refValidations = signature.getReferenceValidations();
		if (Utils.isCollectionNotEmpty(refValidations)) {
			for (ReferenceValidation referenceValidation : refValidations) {
				refs.add(getXmlDigestMatcher(referenceValidation));
				List<ReferenceValidation> dependentValidations = referenceValidation.getDependentValidations();
				if (Utils.isCollectionNotEmpty(dependentValidations) && 
						(Utils.isCollectionNotEmpty(signature.getDetachedContents()) || isAtLeastOneFound(dependentValidations))) {
					for (ReferenceValidation dependentValidation : referenceValidation.getDependentValidations()) {
						refs.add(getXmlDigestMatcher(dependentValidation));
					}
				}
			}
		}
		return refs;
	}
	
	/**
	 * Checks if at least one Manifest entry was found
	 * @return TRUE if at least one ManifestEntry was found, FALSE otherwise
	 */
	public boolean isAtLeastOneFound(List<ReferenceValidation> referenceValidations) {
		for (ReferenceValidation referenceValidation : referenceValidations) {
			if (referenceValidation.isFound()) {
				return true;
			}
		}
		return false;
	}

	private XmlDigestMatcher getXmlDigestMatcher(ReferenceValidation referenceValidation) {
		XmlDigestMatcher ref = new XmlDigestMatcher();
		ref.setType(referenceValidation.getType());
		ref.setName(referenceValidation.getName());
		Digest digest = referenceValidation.getDigest();
		if (digest != null) {
			ref.setDigestValue(digest.getValue());
			ref.setDigestMethod(digest.getAlgorithm());
		}
		ref.setDataFound(referenceValidation.isFound());
		ref.setDataIntact(referenceValidation.isIntact());
		return ref;
	}

	private List<XmlSignatureScope> getXmlSignatureScopes(List<SignatureScope> scopes) {
		List<XmlSignatureScope> xmlScopes = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(scopes)) {
			for (SignatureScope xmlSignatureScope : scopes) {
				xmlScopes.add(getXmlSignatureScope(xmlSignatureScope));
			}
		}
		return xmlScopes;
	}

	private XmlSignatureScope getXmlSignatureScope(SignatureScope scope) {
		final XmlSignatureScope xmlSignatureScope = new XmlSignatureScope();
		xmlSignatureScope.setName(scope.getName());
		xmlSignatureScope.setScope(scope.getType());
		xmlSignatureScope.setDescription(scope.getDescription());
		xmlSignatureScope.setTransformations(scope.getTransformations());
		xmlSignatureScope.setSignerData(xmlSignedDataMap.get(scope.getDSSIdAsString()));
		return xmlSignatureScope;
	}

	private XmlCertificate buildDetachedXmlCertificate(CertificateToken certToken) {

		final XmlCertificate xmlCert = new XmlCertificate();

		xmlCert.setId(certToken.getDSSIdAsString());

		xmlCert.getSubjectDistinguishedName().add(getXmlDistinguishedName(X500Principal.CANONICAL, certToken.getSubjectX500Principal()));
		xmlCert.getSubjectDistinguishedName().add(getXmlDistinguishedName(X500Principal.RFC2253, certToken.getSubjectX500Principal()));

		xmlCert.getIssuerDistinguishedName().add(getXmlDistinguishedName(X500Principal.CANONICAL, certToken.getIssuerX500Principal()));
		xmlCert.getIssuerDistinguishedName().add(getXmlDistinguishedName(X500Principal.RFC2253, certToken.getIssuerX500Principal()));

		xmlCert.setSerialNumber(certToken.getSerialNumber());

		X500Principal x500Principal = certToken.getSubjectX500Principal();
		xmlCert.setCommonName(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.CN, x500Principal));
		xmlCert.setLocality(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.L, x500Principal));
		xmlCert.setState(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.ST, x500Principal));
		xmlCert.setCountryName(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.C, x500Principal));
		xmlCert.setOrganizationName(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.O, x500Principal));
		xmlCert.setGivenName(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.GIVENNAME, x500Principal));
		xmlCert.setOrganizationalUnit(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.OU, x500Principal));
		xmlCert.setSurname(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.SURNAME, x500Principal));
		xmlCert.setPseudonym(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.PSEUDONYM, x500Principal));
		xmlCert.setEmail(DSSASN1Utils.extractAttributeFromX500Principal(BCStyle.E, x500Principal));

		xmlCert.setAuthorityInformationAccessUrls(DSSASN1Utils.getCAAccessLocations(certToken));
		xmlCert.setOCSPAccessUrls(DSSASN1Utils.getOCSPAccessLocations(certToken));
		xmlCert.setCRLDistributionPoints(DSSASN1Utils.getCrlUrls(certToken));
		
		xmlCert.setSources(getXmlCertificateSources(certToken));

		xmlCert.setNotAfter(certToken.getNotAfter());
		xmlCert.setNotBefore(certToken.getNotBefore());
		final PublicKey publicKey = certToken.getPublicKey();
		xmlCert.setPublicKeySize(DSSPKUtils.getPublicKeySize(publicKey));
		xmlCert.setPublicKeyEncryptionAlgo(EncryptionAlgorithm.forKey(publicKey));

		xmlCert.setKeyUsageBits(certToken.getKeyUsageBits());
		xmlCert.setExtendedKeyUsages(getXmlOids(DSSASN1Utils.getExtendedKeyUsage(certToken)));

		xmlCert.setIdPkixOcspNoCheck(DSSASN1Utils.hasIdPkixOcspNoCheckExtension(certToken));

		xmlCert.setBasicSignature(getXmlBasicSignature(certToken));

		xmlCert.setQCStatementIds(getXmlOids(DSSASN1Utils.getQCStatementsIdList(certToken)));
		xmlCert.setQCTypes(getXmlOids(DSSASN1Utils.getQCTypesIdList(certToken)));
		xmlCert.setCertificatePolicies(getXmlCertificatePolicies(DSSASN1Utils.getCertificatePolicies(certToken)));

		xmlCert.setSelfSigned(certToken.isSelfSigned());
		xmlCert.setTrusted(isTrusted(certToken));

		if (includeRawCertificateTokens) {
			xmlCert.setBase64Encoded(certToken.getEncoded());
		} else {
			byte[] certDigest = certToken.getDigest(defaultDigestAlgorithm);
			xmlCert.setDigestAlgoAndValue(getXmlDigestAlgoAndValue(defaultDigestAlgorithm, certDigest));
		}

		return xmlCert;
	}

	private List<CertificateSourceType> getXmlCertificateSources(final CertificateToken token) {
		List<CertificateSourceType> certificateSources = new ArrayList<>();
		if (certificateSourceTypes != null) {
			Set<CertificateSourceType> sourceTypes = certificateSourceTypes.get(token);
			if (sourceTypes != null) {
				certificateSources.addAll(sourceTypes);
			}
		}
		if (Utils.isCollectionEmpty(certificateSources)) {
			certificateSources.add(CertificateSourceType.UNKNOWN);
		}
		return certificateSources;
	}

	private Set<RevocationToken> getRevocationsForCert(CertificateToken certToken) {
		Set<RevocationToken> revocations = new HashSet<>();
		if (Utils.isCollectionNotEmpty(usedRevocations)) {
			for (RevocationToken revocationToken : usedRevocations) {
				if (Utils.areStringsEqual(certToken.getDSSIdAsString(), revocationToken.getRelatedCertificateID())) {
					revocations.add(revocationToken);
				}
			}
		}
		return revocations;
	}

	private List<XmlCertificatePolicy> getXmlCertificatePolicies(List<CertificatePolicy> certificatePolicies) {
		List<XmlCertificatePolicy> result = new ArrayList<>();
		for (CertificatePolicy cp : certificatePolicies) {
			XmlCertificatePolicy xmlCP = new XmlCertificatePolicy();
			xmlCP.setValue(cp.getOid());
			xmlCP.setDescription(OidRepository.getDescription(cp.getOid()));
			xmlCP.setCpsUrl(cp.getCpsUrl());
			result.add(xmlCP);
		}
		return result;
	}

	private List<XmlOID> getXmlOids(List<String> oidList) {
		List<XmlOID> result = new ArrayList<>();
		if (Utils.isCollectionNotEmpty(oidList)) {
			for (String oid : oidList) {
				XmlOID xmlOID = new XmlOID();
				xmlOID.setValue(oid);
				xmlOID.setDescription(OidRepository.getDescription(oid));
				result.add(xmlOID);
			}
		}
		return result;
	}

	private List<XmlTrustedServiceProvider> getXmlTrustedServiceProviders(CertificateToken certToken) {
		List<XmlTrustedServiceProvider> result = new ArrayList<>();
		Map<CertificateToken, List<TrustProperties>> servicesByTrustedCert = getRelatedTrustServices(certToken);
		for (Entry<CertificateToken, List<TrustProperties>> entry : servicesByTrustedCert.entrySet()) {
			CertificateToken trustedCert = entry.getKey();
			List<TrustProperties> services = entry.getValue();

			Map<TrustServiceProvider, List<TrustProperties>> servicesByProviders = classifyByServiceProvider(
					services);

			for (Entry<TrustServiceProvider, List<TrustProperties>> servicesByProvider : servicesByProviders
					.entrySet()) {

				List<TrustProperties> trustServices = servicesByProvider.getValue();
				XmlTrustedServiceProvider serviceProvider = buildXmlTrustedServiceProvider(trustServices.iterator().next());
				serviceProvider.setTrustedServices(buildXmlTrustedServices(trustServices, certToken, trustedCert));
				result.add(serviceProvider);
			}

		}
		return Collections.unmodifiableList(result);
	}

	private XmlTrustedServiceProvider buildXmlTrustedServiceProvider(TrustProperties trustProperties) {
		XmlTrustedServiceProvider result = new XmlTrustedServiceProvider();
		if (trustProperties.getLOTLIdentifier() != null) {
			result.setLOTL(xmlTrustedListsMap.get(trustProperties.getLOTLIdentifier().asXmlId()));
		}
		if (trustProperties.getTLIdentifier() != null) {
			result.setTL(xmlTrustedListsMap.get(trustProperties.getTLIdentifier().asXmlId()));
		}
		TrustServiceProvider tsp = trustProperties.getTrustServiceProvider();
		result.setTSPNames(getLangAndValues(tsp.getNames()));
		result.setTSPRegistrationIdentifiers(tsp.getRegistrationIdentifiers());
		return result;
	}

	private List<XmlLangAndValue> getLangAndValues(Map<String, List<String>> map) {
		if (Utils.isMapNotEmpty(map)) {
			List<XmlLangAndValue> result = new ArrayList<>();
			for (Entry<String, List<String>> entry : map.entrySet()) {
				for (String value : entry.getValue()) {
					XmlLangAndValue langAndValue = new XmlLangAndValue();
					langAndValue.setLang(entry.getKey());
					langAndValue.setValue(value);
					result.add(langAndValue);
				}
			}
			return result;
		}
		return null;
	}

	private Map<CertificateToken, List<TrustProperties>> getRelatedTrustServices(CertificateToken certToken) {
		Map<CertificateToken, List<TrustProperties>> result = new HashMap<>();
		Set<CertificateToken> processedTokens = new HashSet<>();
		for (CertificateSource trustedSource : trustedCertSources) {
			if (trustedSource instanceof TrustedListsCertificateSource) {
				TrustedListsCertificateSource trustedCertSource = (TrustedListsCertificateSource) trustedSource;
				while (certToken != null) {
					List<TrustProperties> trustServices = trustedCertSource.getTrustServices(certToken);
					if (!trustServices.isEmpty()) {
						result.put(certToken, trustServices);
					}
					if (certToken.isSelfSigned() || processedTokens.contains(certToken)) {
						break;
					}
					processedTokens.add(certToken);
					certToken = getCertificateByPubKey(certToken.getPublicKeyOfTheSigner());
				}
			}
		}
		return result;
	}

	private List<XmlTrustedService> buildXmlTrustedServices(List<TrustProperties> trustPropertiesList,
			CertificateToken certToken, CertificateToken trustedCert) {
		List<XmlTrustedService> result = new ArrayList<>();

		for (TrustProperties trustProperties : trustPropertiesList) {
			TimeDependentValues<TrustServiceStatusAndInformationExtensions> trustService = trustProperties.getTrustService();
			List<TrustServiceStatusAndInformationExtensions> serviceStatusAfterOfEqualsCertIssuance = trustService.getAfter(certToken.getNotBefore());
			if (Utils.isCollectionNotEmpty(serviceStatusAfterOfEqualsCertIssuance)) {
				for (TrustServiceStatusAndInformationExtensions serviceInfoStatus : serviceStatusAfterOfEqualsCertIssuance) {
					XmlTrustedService trustedService = new XmlTrustedService();

					trustedService.setServiceDigitalIdentifier(xmlCertsMap.get(trustedCert.getDSSIdAsString()));
					trustedService.setServiceNames(getLangAndValues(serviceInfoStatus.getNames()));
					trustedService.setServiceType(serviceInfoStatus.getType());
					trustedService.setStatus(serviceInfoStatus.getStatus());
					trustedService.setStartDate(serviceInfoStatus.getStartDate());
					trustedService.setEndDate(serviceInfoStatus.getEndDate());

					List<String> qualifiers = getQualifiers(serviceInfoStatus, certToken);
					if (Utils.isCollectionNotEmpty(qualifiers)) {
						trustedService.setCapturedQualifiers(qualifiers);
					}

					List<String> additionalServiceInfoUris = serviceInfoStatus.getAdditionalServiceInfoUris();
					if (Utils.isCollectionNotEmpty(additionalServiceInfoUris)) {
						trustedService.setAdditionalServiceInfoUris(additionalServiceInfoUris);
					}

					List<String> serviceSupplyPoints = serviceInfoStatus.getServiceSupplyPoints();
					if (Utils.isCollectionNotEmpty(serviceSupplyPoints)) {
						trustedService.setServiceSupplyPoints(serviceSupplyPoints);
					}

					trustedService.setExpiredCertsRevocationInfo(serviceInfoStatus.getExpiredCertsRevocationInfo());

					result.add(trustedService);
				}
			}
		}
		return Collections.unmodifiableList(result);
	}

	private Map<TrustServiceProvider, List<TrustProperties>> classifyByServiceProvider(
			List<TrustProperties> trustPropertiesList) {
		Map<TrustServiceProvider, List<TrustProperties>> servicesByProviders = new HashMap<>();
		if (Utils.isCollectionNotEmpty(trustPropertiesList)) {
			for (TrustProperties trustProperties : trustPropertiesList) {
				TrustServiceProvider currentTrustServiceProvider = trustProperties.getTrustServiceProvider();
				List<TrustProperties> list = servicesByProviders.get(currentTrustServiceProvider);
				if (list == null) {
					list = new ArrayList<>();
					servicesByProviders.put(currentTrustServiceProvider, list);
				}
				list.add(trustProperties);
			}
		}
		return servicesByProviders;
	}

	/**
	 * Retrieves all the qualifiers for which the corresponding conditionEntry is
	 * true.
	 *
	 * @param certificateToken
	 * @return
	 */
	private List<String> getQualifiers(TrustServiceStatusAndInformationExtensions serviceInfoStatus, CertificateToken certificateToken) {
		LOG.trace("--> GET_QUALIFIERS()");
		List<String> list = new ArrayList<>();
		final List<ConditionForQualifiers> conditionsForQualifiers = serviceInfoStatus.getConditionsForQualifiers();
		if (Utils.isCollectionNotEmpty(conditionsForQualifiers)) {
			for (ConditionForQualifiers conditionForQualifiers : conditionsForQualifiers) {
				Condition condition = conditionForQualifiers.getCondition();
				if (condition.check(certificateToken)) {
					list.addAll(conditionForQualifiers.getQualifiers());
				}
			}
		}
		return list;
	}
	
	private XmlDigestAlgoAndValue getXmlDigestAlgoAndValue(Digest digest) {
		if (digest == null) {
			return getXmlDigestAlgoAndValue(null, null);
		} else {
			return getXmlDigestAlgoAndValue(digest.getAlgorithm(), digest.getValue());
		}
	}

	private XmlDigestAlgoAndValue getXmlDigestAlgoAndValue(DigestAlgorithm digestAlgo, byte[] digestValue) {
		XmlDigestAlgoAndValue xmlDigestAlgAndValue = new XmlDigestAlgoAndValue();
		xmlDigestAlgAndValue.setDigestMethod(digestAlgo);
		xmlDigestAlgAndValue.setDigestValue(digestValue == null ? DSSUtils.EMPTY_BYTE_ARRAY : digestValue);
		return xmlDigestAlgAndValue;
	}

	private String emptyToNull(String text) {
		if (Utils.isStringEmpty(text)) {
			return null;
		}
		return text;
	}

}
