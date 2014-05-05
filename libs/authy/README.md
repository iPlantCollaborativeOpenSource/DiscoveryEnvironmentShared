# authy

A Clojure library designed to allow Java and Clojure programs to interact with
the OAuth Authorization server in Groupy.

## Usage

### From Java

```java
import java.util.Date;
import org.iplantc.core.authy.OAuthTokenInfo;
import org.iplantc.core.authy.OAuthTokenRetriever;

// Retrieving an access token.
OAuthTokenRetriever retriever = new OAuthTokenRetriever(keyFilePath, baseUrl, issuer);
OAuthTokenInfo tokenInfo = retriever.getToken(username);
String token = tokenInfo.getAccessToken();
String tokenType = tokenInfo.getTokenType();
Date expirationTime = tokenInfo.getExpirationTime();
```

### From Clojure

```clojure
(import '[org.iplantc.core.authy OAuthTokenInfo OAuthTokenRetriever])

(def retriever (OAuthTokenRetriever key-file-path base-url issuer))

(def token-info (.getToken retriever username))
(def token (.getAccessToken token-info))
(def token-type (.getTokenType token-info))
(def expiration-time (.getExpirationTime token-info))
```

## License

http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt
