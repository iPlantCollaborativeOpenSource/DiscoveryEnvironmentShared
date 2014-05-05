(ns authy.core
  (:use [medley.core :only [remove-vals]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.data.codec.base64 :as b64]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.security KeyFactory Signature]
           [java.security.spec PKCS8EncodedKeySpec]))

(def ^:private alg "sha256")
(def ^:private java-sign-alg "SHA256WITHRSA")
(def ^:private oauth-grant-type "urn:ietf:params:oauth:grant-type:jwt-bearer")

(defn- slurp-bytes
  "Slurps file contents into a byte array."
  [path & {:keys [buf-size] :or {buf-size 8196}}]
  (with-open [in (io/input-stream (io/file path))]
    (loop [acc (byte-array 0)]
      (let [buf (byte-array buf-size)
            len (.read in buf)]
        (if (pos? len)
          (recur (into-array Byte/TYPE (concat acc (take len buf))))
          acc)))))

(defn- load-private-key
  "Loads a private key from a DER encoded PKCS8 file."
  [path]
  (.generatePrivate (KeyFactory/getInstance "RSA")
                    (PKCS8EncodedKeySpec. (slurp-bytes path))))

(defn- jwt-sign
  "Signs a JWT assertion."
  [header payload private-key]
  (let [signer (doto (Signature/getInstance java-sign-alg) (.initSign private-key))]
    (.update signer header)
    (.update signer (.getBytes "."))
    (.update signer payload)
    (.sign signer)))

(defn- encode
  "Encodes a JWT assertion."
  [payload private-key]
  (let [header    {:type "JWT" :alg alg}
        b64-json  (fn [m] (b64/encode (.getBytes (cheshire/encode m))))
        header    (b64-json header)
        payload   (b64-json payload)
        signature (b64/encode (jwt-sign header payload private-key))]
    (string/join "." (map #(String. %) [header payload signature]))))

(defn- create-assertion
  "Creates a JWT assertion."
  [{:keys [iss scope aud]
    :or   {scope "admin"
           aud   "api.iplantcollaborative.org"}
    :as   assertion}]
  (let [timestamp (long (/ (System/currentTimeMillis) 1000))]
    (->> (merge assertion {:iss   iss
                           :scope scope
                           :aud   (or aud "")
                           :iat   timestamp
                           :exp   (+ timestamp 3600)})
         (remove-vals nil?))))

(defn- get-token
  "Obtains an OAuth token for a JWT assertion."
  [url assertion]
  (:body (http/post url {:form-params {:assertion  assertion
                                       :grant_type oauth-grant-type}
                         :as          :json})))

(defn- get-token-info
  "Obtains information about an OAuth token."
  [url token]
  (http/get url {:query-params {:access_token token}}))

(gen-class
 :name         org.iplantc.core.authy.OAuthToken
 :prefix       "token-"
 :init         init
 :constructors {[clojure.lang.PersistentArrayMap] []}
 :state        state
 :methods      [[getAccessToken [] String]
                [getTokenType [] String]
                [getExpirationTime [] java.util.Date]])

(defn- determine-expiration-time
  [lifetime]
  (java.util.Date. (+ (System/currentTimeMillis) (* lifetime 1000))))

(defn token-init
  [{access-token :access_token
    token-type   :token_type
    expires-in   :expires_in}]
  [[] {:access-token    access-token
       :token-type      token-type
       :expiration-time (determine-expiration-time expires-in)}])

(defn token-getAccessToken
  [this]
  (:access-token (.state this)))

(defn token-getTokenType
  [this]
  (:token-type (.state this)))

(defn token-getExpirationTime
  [this]
  (:expiration-time (.state this)))

(gen-class
 :name         org.iplantc.core.authy.OAuthTokenRetriever
 :init         init
 :constructors {[String String String] []}
 :state        state
 :methods      [[getToken [] org.iplantc.core.authy.OAuthToken]
                [getToken [String] org.iplantc.core.authy.OAuthToken]])

(defn -init
  [key-file-path base-url issuer]
  [[] {:pk       (load-private-key key-file-path)
       :base-url base-url
       :issuer   issuer}])

(defn retriever-get-token
  [{:keys [pk base-url issuer]} sub]
  (org.iplantc.core.authy.OAuthToken.
   (get-token (str (curl/url base-url "o" "oauth2" "token"))
              (encode (create-assertion {:iss issuer :sub sub}) pk))))

(defn -getToken
  ([this sub]
     (retriever-get-token (.state this) sub))
  ([this]
     (retriever-get-token (.state this) nil)))
