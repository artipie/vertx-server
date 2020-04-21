<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/vertx-server)](http://www.rultor.com/p/artipie/vertx-server)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/artipie/vertx-server/master.svg)](https://travis-ci.org/artipie/vertx-server)
[![Javadoc](http://www.javadoc.io/badge/com.artipie/vertx-server.svg)](http://www.javadoc.io/doc/com.artipie/vertx-server)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/vertx-server/blob/master/LICENSE)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/vertx-server)](https://hitsofcode.com/view/github/artipie/vertx-server)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/vertx-server.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/vertx-server)
[![PDD status](http://www.0pdd.com/svg?name=artipie/vertx-server)](http://www.0pdd.com/p?name=artipie/vertx-server)

This is a simple storage, used in a few other projects.

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>vertx-server</artifactId>
  <version>[...]</version>
</dependency>
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/vertx-server)
for more technical details.

## Usage example

```java
final Vertx vertx = Vertx.vertx();
final int port = 8080;
final Slice slice = ...; // some Slice implementation
final VertxSliceServer server = new VertxSliceServer(vertx, slice, port);
server.start();
```

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

