FROM mariadb:10.11

# 시스템 타임존 및 문자셋 설정 (utf8mb4)
ENV TZ=Asia/Seoul
RUN echo "[mysqld]\ncharacter-set-server=utf8mb4\ncollation-server=utf8mb4_unicode_ci" > /etc/mysql/conf.d/charset.cnf