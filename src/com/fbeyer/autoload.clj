(ns com.fbeyer.autoload
  (:require [clojure.java.io :as io])
  (:import [java.util Properties]))

;; TODO: Error handling, e.g. parse errors
;; We could use dynamic vars and bind context information, e.g. the current
;; resource and line number.  We could also install an error handler this way,
;; and provide a convenient way of installing it, e.g. passing opts to API
;; functions.

(defn- map-reader
  "A transducer that maps inputs with a reader."
  [rf]
  (fn
    ([] (rf))
    ([result] (rf result))
    ([result input]
     (with-open [reader (io/reader input)]
       (rf result reader)))))

(defn- parse-service-line
  "Parses a line in the format of java.util.ServiceLoader."
  [line]
  (when-let [[_ name] (re-find #"^\s*([^#\s]+)\s*(?:#.*)?$" line)]
    (symbol name)))

(defn- read-properties [reader]
  (doto (Properties.)
    (.load reader)))

(defn resources
  "Find all resources with the given name.  Use the context class loader
   if no loader is specified."
  ([name] (resources name (.. Thread currentThread getContextClassLoader)))
  ([^String name ^ClassLoader loader]
   (-> loader (.getResources name) enumeration-seq)))

;; TODO: Support other paths than META-INF/services?
(defn services
  "Reads services defined by files with the given name in META-INF/services/
   directories on the classpath, similar to java.util.ServiceLoader. Returns
   a sequence of symbols."
  [name]
  (sequence (comp map-reader
                  (mapcat line-seq)
                  (keep parse-service-line))
            (resources (str "META-INF/services/" name))))

;; TODO: Support "merge with" and "value readers", e.g. split by comma into vectors
(defn properties
  "Reads all Java properties files with the given name on the classpath."
  [name]
  (into {} (comp map-reader (mapcat read-properties)) (resources name)))

;; TODO: test this!
(defn autoload
  "Automatically requires namespaces defined in `META-INF/services/com.fbeyer.autoload`
   files, or the custom `name` within `META-INF/services`."
  ([] (autoload "com.fbeyer.autoload"))
  ([name]
   (run! require (services name))))
