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

(def test-data
	(atom {"zg1jp4q" {:key "zg1jp4q"
				 						:name "Mercy"}

				 "7zxnlkb" {:key "7zxnlkb"
				 						:name "Jack"}}))

(def compiled-schema (atom {}))
(def hook (chan))

(def db-schema
	;; quote map for edn
	'{:objects
		{:human {:fields {:key {:type String}
			 								:name {:type String}}}}
		
		:queries
	  {:hero {:type :human
	          :args {:key {:type String}}
	          :resolve :get-hero}}})

(def db-schema-2
	;; quote map for edn
	'{:objects
		{:human {:fields {:key {:type String}
			 								:name {:type String}
			 								:age {:type Int}}}}
		
		:queries
	  {:hero {:type :human
	          :args {:key {:type String}}
	          :resolve :get-hero}}})

(defn get-compiled-schema [sch]
	(-> sch
	  	prn-str
      edn/read-string
      (attach-resolvers {:get-hero resolve-hero})
      schema/compile))

(reset! compiled-schema (get-compiled-schema db-schema))

(defn swap-schema []
	(swap! test-data assoc "iu9mcoj" {:key "iu9mcoj"
																		:name "Lunafreya"
																		:age 19})
	(reset! compiled-schema (get-compiled-schema db-schema-2)))


(go-loop [sch (<! hook)]
	(if sch
		(do
			;; (<! (timeout 5000))
			(swap-schema)
			(recur (<! hook)))))

(defn do-rebuild []
	(put! hook "hellllooo")
	"Doing it now!")

(defn fetch-for [eid]
	(let [b (->> (.one (.fetch client CDAEntry) eid)
				  		 (.toJson gson))]
		{	:status 200
	 	 	:headers {"Content-Type" "application/json"}
	 	 	:body b }))

(defn resolve-hero
	[context args _value]
	(get @test-data (:key args)))


(defn run-gql []
	(.toJson gson (execute @compiled-schema 
												 "{
												 		hero(key: \"iu9mcoj\") {
												 				name
												 				age
												 			}
												 		}" 
												 nil nil)))


(defroutes app-routes
  (GET "/" [] "Hiya")
  (GET "/eid/:eid" [eid] (fetch-for eid))
  (GET "/rebuild" [] (do-rebuild))
  (GET "/gql" [] (run-gql))

  (route/not-found "Not Found"))


(def app
  (wrap-defaults app-routes site-defaults))




