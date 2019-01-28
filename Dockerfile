FROM tomcat:9-jre8
MAINTAINER leon_xi@163.com

RUN rm -Rf $CATALINA_HOME/webapps/ROOT
RUN rm -Rf $CATALINA_HOME/webapps/docs
RUN rm -Rf $CATALINA_HOME/webapps/examples
RUN rm -Rf $CATALINA_HOME/webapps/host-manager
RUN rm -Rf $CATALINA_HOME/webapps/manager

ADD target/ROOT.war $CATALINA_HOME/webapps/
