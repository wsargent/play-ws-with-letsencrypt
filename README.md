# Play WS with LetsEncrypt Certificate

This is an example Play Application that talks to https://playframework.com using the LetsEncrypt certificate.

## Running

Type "activator run" at the prompt.

## Configuration

```
play.ws {
  ## WS SSL
  # https://www.playframework.com/documentation/latest/WsSSL
  # ~~~~~
  ssl {
    # Configuring HTTPS with Play WS does not require programming.  You can
    # set up both trustManager and keyManager for mutual authentication, and
    # turn on JSSE debugging in development with a reload.
    # debug.handshake = true
    trustManager = {
      stores = [
        # From https://www.sslshopper.com/certificate-decoder.html
        #
        # Common Name: Let's Encrypt Authority X3
        # Organization: Let's Encrypt
        # Country: US
        # Valid From: March 17, 2016
        # Valid To: March 17, 2021
        # Issuer: DST Root CA X3, Digital Signature Trust Co.
        # Serial Number: 0a0141420000015385736a0b85eca708
        #
        { type = "PEM", path = "./conf/letsencrypt-root-ca-x3.pem" }
      ]
    }
  }
}

```

## Importing

If you need to import the letsencrypt certificate to your "global" Java trust store, here's the command to do that:

```
# Create a JKS keystore that trusts the example CA, with the default password.
keytool -import -v \
  -alias letsencrypt-root-ca-x3 \
  -file ./conf/letsencrypt-root-ca-x3.pem \
  -keystore $JAVA_HOME/jre/lib/security/cacerts \
  -storepass changeit

# List out the details of the store password.
keytool -list -v \
  -keystore $JAVA_HOME/jre/lib/security/cacerts \
  -storepass changeit
```

Please see the Play WS documentation for more details:

https://www.playframework.com/documentation/2.5.x/CertificateGeneration#configuring-a-trust-store