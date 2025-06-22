# Spring application for demo redis build on Java

I have 2 api to test

## Get key

```shell
  curl --location 'localhost:8080/api/cache?key=foo'
```

## Set key

```shell
    curl --location --request POST 'localhost:8080/api/cache?key=foo&value=bar'
```