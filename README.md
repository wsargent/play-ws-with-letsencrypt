# Play WS with LetsEncrypt Certificate

This is an example Play Application that talks to https://helloworld.letsencrypt.org using the LetsEncrypt certificate.

## Running

Download and install "sbt" or "activator".  Activator is basically sbt + templates, so don't worry about it if you don't have it.

Type "activator run" or "sbt run" at the prompt.

Then go to http://localhost:9000 and type in the URL that you want to check the certificates against.

## Importing

If you need to import the letsencrypt certificate to your "global" Java trust store, here's the command to do that (you may need sudo access):

```
# Create a JKS keystore that trusts the example CA, with the default password.
sudo keytool -import -v \
  -alias dst-x3-root \
  -file ./conf/dst-x3-root.pem \
  -keystore $JAVA_HOME/jre/lib/security/cacerts \
  -storepass changeit

# List out the details of the store password.
keytool -list -v \
  -keystore $JAVA_HOME/jre/lib/security/cacerts \
  -storepass changeit | grep -i "dst"
```

Please see the Play WS documentation for more details:

https://www.playframework.com/documentation/2.5.x/CertificateGeneration#configuring-a-trust-store
