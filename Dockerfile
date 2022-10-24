# Adapted from https://github.com/jboss-dockerfiles/base/blob/master/Dockerfile
FROM redhat/ubi8:8.6

RUN yum update -y && yum clean all

# Create a user and group used to launch processes
# The user ID 1000 is the default for the first "regular" user on Fedora/RHEL,
# so there is a high chance that this ID will be equal to the current user
# making it easier to use volumes (no permission issues)
RUN groupadd -r jboss -g 1000 && useradd -u 1000 -r -g jboss -m -d /opt/jboss -s /sbin/nologin -c "JBoss user" jboss && \
    chmod 755 /opt/jboss

# Set the working directory to jboss' user home directory
WORKDIR /opt/jboss

# Switch back to jboss user
USER jboss

# Set the JAVA_HOME variable to make it clear where Java is located
ENV JAVA_HOME /usr/lib/jvm/java

# Adapted from https://github.com/jboss-dockerfiles/wildfly/blob/26.1.0.Final/Dockerfile

# Set the WILDFLY_VERSION env variable
ENV WILDFLY_VERSION 26.1.2.Final
ENV WILDFLY_SHA1 3dda0f3795c00cedf8b14c83f8c341244e7cad4
ENV JBOSS_HOME /opt/jboss/wildfly

USER root

# Add the WildFly distribution to /opt, and make wildfly the owner of the extracted tar content
# Make sure the distribution is available from a well-known place
RUN cd $HOME \
    && curl -L -O https://github.com/wildfly/wildfly/releases/download/$WILDFLY_VERSION/wildfly-$WILDFLY_VERSION.tar.gz \
    && sha1sum wildfly-$WILDFLY_VERSION.tar.gz | grep $WILDFLY_SHA1 \
    && tar xf wildfly-$WILDFLY_VERSION.tar.gz \
    && mv $HOME/wildfly-$WILDFLY_VERSION $JBOSS_HOME \
    && rm wildfly-$WILDFLY_VERSION.tar.gz \
    && chown -R jboss:0 ${JBOSS_HOME} \
    && chmod -R g+rw ${JBOSS_HOME}

# Ensure signals are forwarded to the JVM process correctly for graceful shutdown
ENV LAUNCH_JBOSS_IN_BACKGROUND true

USER jboss

# Expose the ports in which we're interested
EXPOSE 8080


USER root
RUN yum -y update && yum -y install java-17-openjdk java-17-openjdk-devel  && yum clean all

USER jboss
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

# From http://stackoverflow.com/questions/20965737/docker-jboss7-war-commit-server-boot-failed-in-an-unrecoverable-manner
RUN rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history \
  && mkdir -p /opt/jboss/data \
  && chown jboss:jboss /opt/jboss/data

# This argument comes from https://github.com/jboss-dockerfiles/wildfly
# It enables the admin interface.

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "--read-only-server-config=standalone.xml"]
