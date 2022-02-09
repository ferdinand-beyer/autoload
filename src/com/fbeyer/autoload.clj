(ns com.fbeyer.autoload
  (:require [clojure.java.io :as io]))

;; TODO: Error handling, e.g. parse errors
;; We could use dynamic vars and bind context information, e.g. the current
;; resource and line number.

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

(defn resources
  "Find all resources with the given name.  Use the context class loader
   if no loader is specified."
  ([name] (resources name (.. Thread currentThread getContextClassLoader)))
  ([^String name ^ClassLoader loader]
   (-> loader (.getResources name) enumeration-seq)))

(defn services
  "Reads services defined by files with the given name in META-INF/services/
   directories on the classpath, similar to java.util.ServiceLoader. Returns
   a sequence of symbols."
  [name]
  (sequence (comp map-reader
                  (mapcat line-seq)
                  (keep parse-service-line))
            (resources (str "META-INF/services/" name))))

(defn autoload
  "Automatically requires namespaces defined in META-INF/services/com.fbeyer.autoload
   files."
  []
  (doseq [sym (services "com.fbeyer.autoload")]
    (require sym)))

