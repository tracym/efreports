# EF Reports

EF Reports is a web application written in Clojure for pulling data from a traditional RDBMS (using SQL statements) and making that data available to less technical users (read: non sql writing users) for dyamica manipulations. Supported dynamic manipulations include filtering, sorting, joining, totalling, and pivoting. Dynamic manipulations are stored in a mongodb session for later serialization into  "reports" via the Save tab on the Collection manipulation page.

EF Reports uses memoization to limit the execution of SQL on the underlying RDBMS. End users must use a refresh button to force the application to retrieve data from the underlying RDBMS.    

A demo is available at the folling url:
http://efreportsdemo.herokuapp.com

## Usage
   git clone https://github.com/tracym/efreports.git

EF Reports is designed to use any database with a JDBC driver as the underlying RDBMS from wich to pull data. Unfortunately, I haven't had time make this configurable in a nicer way; so, next, you will need to update the db variable in helpers/data.clj to indicate the information for your local RDMS. Also, be sure to install the proper jdbc drivers as indicated on the [clojure jdbc page](https://github.com/clojure/java.jdbc). 

## Usage
   lein trampoline ring server

Once you login using the supplied credentials on the login page, you will need to create a new collection. You will need to give this collection a name and description along with the SQL to retrieve the data from the local RDBMS you setup earlier. You then should be able navigate back to the collections list and click the collection name. Now you should see tabs that enable you to dynamically manipulate the collections data.

## Known Issues

* Data manipulation code needs to be decoupled from data presentation code. Ideally, the presentation layer should communicate with the manipulation layer via an API.  

* Forms that accept direct SQL input should be moved (perhaps to a sepearate application that could live behind a firewall) or removed because security

* Joining or "mapping" data behaves more like a left join than an inner join

* Pivot tables can only be generated from Collections that have not been manipulated

* Nulls and data types other than numbers and strings are not handled reliably. When creating Collections, liberal use of CAST and COAELESCE are your best bet

* Mapping result sets causes noticeable performance degredation

* This was a "Learn Clojure" project and as such there are quite a few warts in the code base that need to be removed, especially in the view code.

* Connections to multiple databases (RDBMS's)  are unsupported at this time but should be in the future

## License

Copyright © 2014 Tracy Malkemes

Distributed under the Eclipse Public License, the same as Clojure.
