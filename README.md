NLP_Analyzer
============

Purpose
-------

This project consists of a plugin for Pentaho Analyzer that offers a simple interface where the user can introduce queries in Natural Language and a report will be generated

Instructions
---------------

To deploy the plugin in your pentaho environment simply copy the NLPapp folder into pentaho-server/tomcat/webapps/ and the nlp-plugin folder into pentaho-server/pentaho-solutions/system/.

The VisNLP folder contains the source code for NLPapp/WEB-INF/lib/VisNLP-1.0.jar, you may build it by using *mvn package shade:shade*