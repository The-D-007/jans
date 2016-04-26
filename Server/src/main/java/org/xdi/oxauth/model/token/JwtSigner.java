/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.token;

import org.python.jline.internal.Preconditions;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.CryptoProviderFactory;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.jwt.Jwt;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.util.security.StringEncrypter;

/**
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version February 17, 2016
 */

public class JwtSigner {

    private AbstractCryptoProvider cryptoProvider;
    private SignatureAlgorithm signatureAlgorithm;
    private String audience;
    private String hmacSharedSecret;

    private Jwt jwt;

    public JwtSigner(SignatureAlgorithm signatureAlgorithm, String audience, String hmacSharedSecret) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.audience = audience;
        this.hmacSharedSecret = hmacSharedSecret;

        cryptoProvider = CryptoProviderFactory.getCryptoProvider(
                ConfigurationFactory.instance().getConfiguration(),
                ConfigurationFactory.instance().getWebKeys());
    }

    public static JwtSigner newJwtSigner(Client client) throws StringEncrypter.EncryptionException {
        Preconditions.checkNotNull(client);

        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(ConfigurationFactory.instance().getConfiguration().getDefaultSignatureAlgorithm());
        if (client.getIdTokenSignedResponseAlg() != null) {
            signatureAlgorithm = SignatureAlgorithm.fromName(client.getIdTokenSignedResponseAlg());
        }
        return new JwtSigner(signatureAlgorithm, client.getClientId(), client.getClientSecret());
    }

    public Jwt newJwt() throws Exception {
        jwt = new Jwt();

        // Header
        String keyId = cryptoProvider.getKeyId(ConfigurationFactory.instance().getWebKeys(), signatureAlgorithm);
        if (keyId != null) {
            jwt.getHeader().setKeyId(keyId);
        }
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);

        // Claims
        jwt.getClaims().setIssuer(ConfigurationFactory.instance().getConfiguration().getIssuer());
        jwt.getClaims().setAudience(audience);
        return jwt;
    }

    public Jwt sign() throws Exception {
        // Signature
        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), hmacSharedSecret, signatureAlgorithm);
        jwt.setEncodedSignature(signature);

        return jwt;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
}
