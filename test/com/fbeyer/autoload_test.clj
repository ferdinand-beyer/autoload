(ns com.fbeyer.autoload-test
  (:require [clojure.test :refer [deftest is]]
            [com.fbeyer.autoload :as autoload]))

(deftest services-test
  (is (= '[first-provider second-provider third-provider]
         (autoload/services "com.fbeyer.autoload.test"))))

(deftest properties-test
  (is (= {"simple-property" "simple-value"
          "clojure.qualified/prop" "one, two, three"}
         (autoload/properties "META-INF/autoload.test.properties"))))
