FROM ubuntu:14.04
MAINTAINER Angel Borroy angel.borroy@keensoft.es

RUN apt-get update && apt-get install -y --no-install-recommends ubuntu-desktop wget curl xmlstarlet
RUN wget http://dl.alfresco.com/release/community/201602-build-00005/alfresco-community-installer-201602-linux-x64.bin

RUN chmod +x alfresco-community-installer-201602-linux-x64.bin

RUN ./alfresco-community-installer-201602-linux-x64.bin --prefix /opt/alfresco \
--unattendedmodeui none --mode unattended --debuglevel 0 \
--enable-components javaalfresco,postgres,libreofficecomponent \
--jdbc_url "jdbc:postgresql://localhost/dbname?useUnicode=yes&characterEncoding=UTF-8" \
--jdbc_driver org.postgresql.Driver --jdbc_database dbname --jdbc_username dbuser \
--jdbc_password dbpassword --alfresco_ftp_port 21 \
--alfresco_admin_password admin --baseunixservice_install_as_service 0 \
--alfrescocustomstack_services_startup demand

EXPOSE 8080

CMD /bin/bash -c "/opt/alfresco/alfresco.sh start"