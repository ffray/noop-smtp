package biz.itcf.nsmtp;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Created with IntelliJ IDEA.
 * User: Florian
 * Date: 23.06.13
 * Time: 19:32
 * To change this template use File | Settings | File Templates.
 */
public class SSLContextBuilder {

    private String protocol;

    private String providerName;
    private Provider provider;

    private String keyManagerAlgorithm;
    private Provider keyManagerProvider;
    private String keyManagerProviderName;
    private Provider keyStoreProvider;
    private String keyStoreProviderName;
    private String keyStoreType;
    private String keyStoreFile;
    private char[] keyStorePassword;
    
    private String secureRandomAlgorithm;
    private Provider secureRandomProvider;
    private String secureRandomProviderName;
    
    private String trustManagerAlgorithm;
    private Provider trustManagerProvider;
    private String trustManagerProviderName;
    private Provider trustStoreProvider;
    private String trustStoreProviderName;
    private String trustStoreType;
    private String trustStoreFile;
    private char[] trustStorePassword;

    public SSLContextBuilder() {
    }

    public SSLContextBuilder protocol(String protocol) {
        this.protocol = protocol;

        return this;
    }

    public SSLContextBuilder provider(Provider provider) {
        this.provider = provider;
        this.providerName = null;

        return this;
    }

    public SSLContextBuilder provider(String providerName) {
        this.provider = null;
        this.providerName = providerName;

        return this;
    }

    public SSLContextBuilder keyStore(String fileName, char[] password)
    {
        return keyStore(null, null, null, null, fileName, password);
    }

    public SSLContextBuilder keyStore(String keyStoreAlgorithm, String keyStoreProvider, String type, String provider, String fileName, char[] password) {
        this.keyManagerAlgorithm = keyStoreAlgorithm;
        this.keyManagerProvider = null;
        this.keyManagerProviderName = keyStoreProvider;
        this.keyStoreType = type;
        this.keyStoreProvider = null;
        this.keyStoreProviderName = provider;
        this.keyStoreFile = fileName;
        this.keyStorePassword = password;

        return this;
    }

    public SSLContextBuilder trustStore(String fileName, char[] password)
    {
        return trustStore(null, null, null, null, fileName, password);
    }

    public SSLContextBuilder trustStore(String trustStoreAlgorithm, String trustStoreProvider, String type, String provider, String fileName, char[] password) {
        this.trustManagerAlgorithm = trustStoreAlgorithm;
        this.trustManagerProvider = null;
        this.trustManagerProviderName = trustStoreProvider;
        this.trustStoreType = type;
        this.trustStoreProvider = null;
        this.trustStoreProviderName = provider;
        this.trustStoreFile = fileName;
        this.trustStorePassword = password;

        return this;
    }

    public SSLContext build() throws GeneralSecurityException, IOException {
        SSLContext result;

        if (protocol == null)
        {
            if (provider != null) {
                throw new IllegalStateException("protocol must be specified if provider is specified");
            } else {
                result = SSLContext.getDefault();
            }
        } else {
            if (provider != null) {
                result = SSLContext.getInstance(protocol, provider);
            } else if (providerName != null) {
                result = SSLContext.getInstance(protocol, providerName);
            } else {
                result = SSLContext.getInstance(protocol);
            }

            result.init(makeKeyManagers(), makeTrustManagers(), makeSecureRandom());
        }

        return result;
    }
    
    private TrustManager[] makeTrustManagers() throws GeneralSecurityException, IOException {
        String trustManagerAlgorithm;
        if (this.trustManagerAlgorithm != null) {
            trustManagerAlgorithm = this.trustManagerAlgorithm;
        } else {
            trustManagerAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        }
        
        TrustManagerFactory tmf;
        if (trustManagerProvider != null) {
            tmf = TrustManagerFactory.getInstance(trustManagerAlgorithm, trustManagerProvider);
        } else if (trustManagerProviderName != null) {
            tmf = TrustManagerFactory.getInstance(trustManagerAlgorithm, trustManagerProviderName);
        } else {
            tmf = TrustManagerFactory.getInstance(trustManagerAlgorithm);
        }

        KeyStore keyStore = makeKeyStore(trustStoreProvider, trustStoreProviderName, trustStoreType, trustStoreFile, trustStorePassword);
        
        tmf.init(keyStore);
        
        return tmf.getTrustManagers();
    }

    private KeyManager[] makeKeyManagers() throws GeneralSecurityException, IOException {
        String keyManagerAlgorithm;
        if (this.keyManagerAlgorithm != null) {
            keyManagerAlgorithm = this.keyManagerAlgorithm;
        } else {
            keyManagerAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
        }

        KeyManagerFactory kmf;
        if (keyManagerProvider != null) {
            kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm, keyManagerProvider);
        } else if (keyManagerProviderName != null) {
            kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm, keyManagerProviderName);
        } else {
            kmf = KeyManagerFactory.getInstance(keyManagerAlgorithm);
        }

        KeyStore keyStore = makeKeyStore(keyStoreProvider, keyStoreProviderName, keyStoreType, keyStoreFile, keyStorePassword);

        kmf.init(keyStore, keyStorePassword);

        return kmf.getKeyManagers();
    }
    
    private KeyStore makeKeyStore(Provider keyStoreProvider, String keyStoreProviderName, String keyStoreType, String keyStoreFile, char[] keyStorePassword) throws GeneralSecurityException, IOException {
        if (keyStoreType == null) {
            keyStoreType = KeyStore.getDefaultType();
        }
        
        KeyStore keyStore;
        if (keyStoreProvider != null) {
            keyStore = KeyStore.getInstance(keyStoreType, keyStoreProvider);
        } else if (keyStoreProviderName != null) {
            keyStore = KeyStore.getInstance(keyStoreType, keyStoreProviderName);
        } else {
            keyStore = KeyStore.getInstance(keyStoreType);
        }

        keyStore.load(new FileInputStream(keyStoreFile), keyStorePassword);
        
        return keyStore;
    }
        

    private SecureRandom makeSecureRandom() throws GeneralSecurityException {
        SecureRandom result;

        if (secureRandomProvider != null) {
            result = SecureRandom.getInstance(secureRandomAlgorithm, secureRandomProvider);
        } else if (secureRandomProviderName != null) {
            result = SecureRandom.getInstance(secureRandomAlgorithm, secureRandomProviderName);
        } else {
            result = SecureRandom.getInstance(secureRandomAlgorithm);
        }

        return result;
    }

}
