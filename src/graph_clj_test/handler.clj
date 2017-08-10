(ns graph-clj-test.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [clojure.edn :as edn]
         		[com.walmartlabs.lacinia.util :refer [attach-resolvers]]
         		[com.walmartlabs.lacinia.schema :as schema]
         		[com.walmartlabs.lacinia :refer [execute]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [clojure.core.async :as async :refer [<! >! <!! put! timeout go-loop chan alt! go]]))

(import '(com.contentful.java.cda CDAClient CDAEntry)
				'(com.google.gson Gson GsonBuilder))

(def gson (.create (.setPrettyPrinting (GsonBuilder.))))

(def client
	(-> 
		(CDAClient/builder)
		(.setSpace "nnlhr3cwksjs")
		(.setToken "f74ca89ce0dae36080afe1026708c555912e65803eb59f46a1032677f992505b")
		.build))


(def hook (chan))

(go-loop [sch (<! hook)]
	(if sch
		(do
			(println "waiting...")
			(<! (timeout 3000))
			(println (str "Build new schema " sch))
			(recur (<! hook)))))


(defn fetch-for [eid]
	(let [b (->> (.one (.fetch client CDAEntry) eid)
				  		 (.toJson gson))]
		(put! hook eid)
		{	:status 200
	 	 	:headers {"Content-Type" "application/json"}
	 	 	:body b }))


(def test-data
	{"zg1jp4q" {:key "zg1jp4q"
	 						:name "Mercy"}

	 "7zxnlkb" {:key "7zxnlkb"
	 						:name "Jack"}})


(defn resolve-hero
	[context args _value]
	(get test-data (:key args)))

(def db-schema
	;; quote map for edn
	'{:objects
		{:human {:fields {:key {:type String}
			 								:name {:type String}}}}
		
		:queries
	  {:hero {:type :human
	          :args {:key {:type String :default-value "zg1jp4q"}}
	          :resolve :get-hero}}})


(def compiled-schema
  (-> db-schema
	  	prn-str
      edn/read-string
      (attach-resolvers {:get-hero resolve-hero})
      schema/compile))


(defn run-gql []
	(.toJson gson (execute compiled-schema 
												 "{
												 		hero(key: \"7zxnlkb\") {
												 				name
												 			}
												 		}" 
												 nil nil)))


(defroutes app-routes
  (GET "/" [] "Hiya")
  (GET "/eid/:eid" [eid] (fetch-for eid))

  (GET "/gql" [] (run-gql))

  (route/not-found "Not Found"))


(def app
  (wrap-defaults app-routes site-defaults))




