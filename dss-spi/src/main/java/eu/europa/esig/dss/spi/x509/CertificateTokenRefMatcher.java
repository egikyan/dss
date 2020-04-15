package eu.europa.esig.dss.spi.x509;

import java.util.Arrays;

import eu.europa.esig.dss.model.Digest;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.spi.DSSASN1Utils;

public class CertificateTokenRefMatcher {

	public boolean match(CertificateToken certificateToken, CertificateRef certificateRef) {
		// If we only know the public key, the token is null
		if (certificateToken == null) {
			return false;
		}

		Digest certDigest = certificateRef.getCertDigest();
		CertificateIdentifier certificateIdentifier = certificateRef.getCertificateIdentifier();
		ResponderId responderId = certificateRef.getResponderId();
		if (certDigest != null) {
			return matchByDigest(certificateToken, certificateRef);
		} else if (certificateIdentifier != null && certificateIdentifier.isRelatedToCertificate(certificateToken)) {
			return true;
		} else if (responderId != null && responderId.isRelatedToCertificate(certificateToken)) {
			return true;
		}
		return false;
	}

	public boolean matchByDigest(CertificateToken certificateToken, CertificateRef certificateRef) {
		Digest certDigest = certificateRef.getCertDigest();
		if (certDigest != null) {
			byte[] currentDigest = certificateToken.getDigest(certDigest.getAlgorithm());
			return Arrays.equals(currentDigest, certDigest.getValue());
		}
		return false;
	}

	public boolean matchBySerialNumber(CertificateToken certificateToken, CertificateRef certificateRef) {
		CertificateIdentifier certificateIdentifier = certificateRef.getCertificateIdentifier();
		if (certificateIdentifier != null && certificateIdentifier.getSerialNumber() != null) {
			return certificateToken.getSerialNumber().equals(certificateIdentifier.getSerialNumber());
		}
		return false;
	}

	public boolean matchByIssuerName(CertificateToken certificateToken, CertificateRef certificateRef) {
		CertificateIdentifier certificateIdentifier = certificateRef.getCertificateIdentifier();
		if (certificateIdentifier != null && certificateIdentifier.getIssuerName() != null) {
			return DSSASN1Utils.x500PrincipalAreEquals(certificateIdentifier.getIssuerName(), certificateToken.getIssuerX500Principal());
		}
		return false;
	}

}