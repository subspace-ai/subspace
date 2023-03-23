(ns app.utils)

(defn created-at []
  (.format (java.time.LocalDateTime/now)
           (java.time.format.DateTimeFormatter/ofPattern
            "yyyy-MM-dd HH:mm:ss")))
