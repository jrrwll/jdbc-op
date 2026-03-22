#!/usr/bin/env bash

mkdir mysql57
docker run -itd --name mysql57 \
 -e MYSQL_ROOT_PASSWORD=root \
 -v $PWD/mysql57:/var/lib/mysql \
 -p 3306:3306 \
 mysql:5.7

mkdir mysql
docker run -itd --name mysql \
 -e MYSQL_ROOT_PASSWORD=root \
 -v $PWD/mysql:/var/lib/mysql \
 -p 3307:3306 \
 mysql

mkdir postgres15
docker run -d -it --name postgres15 \
 -e POSTGRES_PASSWORD=root \
 -e ALLOW_IP_RANGE=0.0.0.0/0 \
 -v $PWD/postgres15:/var/lib/postgresql/data \
 -p 5432:5432 \
 postgres:15

mkdir postgres14
docker run -d -it --name postgres14 \
 -e POSTGRES_PASSWORD=root \
 -e ALLOW_IP_RANGE=0.0.0.0/0 \
 -v $PWD/postgres14:/var/lib/postgresql/data \
 -p 5433:5432 \
 postgres:14

