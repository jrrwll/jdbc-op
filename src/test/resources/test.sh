#!/usr/bin/env bash

# mysql
jdbc-op type-table test.t_base \
    -j jdbc:mysql://127.0.0.1:3306 -u root -p root \
    --dp $HOME/.m2/repository/mysql/mysql-connector-java/8.0.27 --dc com.mysql.cj.jdbc.Driver \
    -S mysql -f ./jdbc-op/src/test/resources/mysql-types.txt -y

jdbc-op export-jdbc --databases test \
    --j1 jdbc:mysql://127.0.0.1:3306 --u1 root --p1 root \
    --dp1 $HOME/.m2/repository/mysql/mysql-connector-java/8.0.27 --dc1 com.mysql.cj.jdbc.Driver \
    --j2 jdbc:mysql://127.0.0.1:3307 --u2 root --p2 root \
    -S mysql --verbose -y


# postgres
jdbc-op type-table test.t_base \
    -j jdbc:postgresql://127.0.0.1:5432/testdb -u myuser -p mypassword \
    --dp $HOME/.m2/repository/org/postgresql/postgresql/42.2.19 --dc org.postgresql.Driver \
    -S postgres -f ./jdbc-op/src/test/resources/postgresql-types.txt -y

jdbc-op export-jdbc --databases test --catalog testdb --use-metadata \
    --j1 jdbc:postgresql://127.0.0.1:5432/testdb --u1 myuser --p1 mypassword \
    --dp1 $HOME/.m2/repository/org/postgresql/postgresql/42.2.19 --dc1 org.postgresql.Driver \
    --j2 jdbc:postgresql://127.0.0.1:5433/testdb --u2 myuser --p2 mypassword \
    -S postgres --verbose -y