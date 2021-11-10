# Adapted from https://github.com/jboss-dockerfiles/base/blob/master/Dockerfile, https://github.com/jboss-dockerfiles/base-jdk/blob/jdk8/Dockerfile and https://github.com/jboss-dockerfiles/wildfly/blob/8.x/Dockerfile
# Source initially licensed under the MIT License as follows:
# The MIT License (MIT)
#
# Copyright (c) 2014 Red Hat
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

FROM centos:centos7
RUN yum update -y && yum -y install xmlstarlet saxon augeas bsdtar unzip java-1.8.0-openjdk-devel && yum clean all

RUN groupadd -r jboss -g 1000 && useradd -u 1000 -r -g jboss -m -d /opt/jboss -s /sbin/nologin -c "JBoss user" jboss && \
    chmod 755 /opt/jboss

WORKDIR /opt/jboss

USER jboss

ENV JAVA_HOME /usr/lib/jvm/java
ENV WILDFLY_VERSION 8.2.1.Final
ENV WILDFLY_SHA1 77161d682005f26acb9d2df5548c8623ba3a4905
ENV JBOSS_HOME /opt/jboss/wildfly

RUN cd $HOME \
    && curl -O https://download.jboss.org/wildfly/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && sha1sum wildfly-$WILDFLY_VERSION.tar.gz | grep $WILDFLY_SHA1 \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv $HOME/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz

# Copy iev.properties
COPY docker/files/wildfly/iev.properties /etc/chouette/iev/

RUN touch /opt/jboss/wildfly/build.log
RUN chmod a+w /opt/jboss/wildfly/build.log

# Copy EAR
COPY chouette_iev/target/chouette.ear /opt/jboss/wildfly/standalone/deployments/
# Copy customized Wildfly modules and Prometheus agent
COPY target/docker/wildfly /opt/jboss/wildfly/
# Copy customized Wildfly configuration file
COPY docker/files/wildfly/standalone.conf /opt/jboss/wildfly/bin
# Copy Prometheus agent configuration file
COPY docker/files/jmx_exporter_config.yml /opt/jboss/wildfly/prometheus

# From http://stackoverflow.com/questions/20965737/docker-jboss7-war-commit-server-boot-failed-in-an-unrecoverable-manner
RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history \
  && mkdir -p /opt/jboss/data \
  && chown jboss:jboss /opt/jboss/data

EXPOSE 8080

# This argument comes from https://github.com/jboss-dockerfiles/wildfly
# It enables the admin interface.

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "--read-only-server-config=standalone.xml"]
