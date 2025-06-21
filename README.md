# redis-java

## List feature

| Feature              | Supported |
|----------------------|-----------|
| Support TTL (expire) | ✅         |
| Save RDB or AOF      | ✅         |
| Pub/Sub              | ❌         |
| Multi/Transaction    | ❌         |
| Cluster/Replication  | ❌         |
| Redis Modules        | ❌         |

## Config

```yaml
#app.properties

# config save AOF
persistence.mode=AOF

  # config RDB
persistence.mode=RDB
rdb.snapshot.interval=30 # time between save (second)

  # clean key expire after time 
ttl.clean.interval=1000 # milisecond
```

## Testing

```shell
    # connect to redis using redis cli
    redis-cli -h 127.0.0.1 -p 6379

    # set key x with value 100
    set x 100
    
    # set key y with value 100 and expiry 10 seconds 
    set y 100 ex 10
    
    # get value key x 
    get x
    
    # delete key x
    del x
```

## RESP3

### Redis serialization protocol specification (RESP)

RESP is the wire protocol that clients implement.

RESP is a compromise among the following considerations:
    
- Simple to implement
- Fast to parse
- Human readable. 

### RESP3 Protocol



### RESP2 vs RESP3: Key Differences

| Feature | RESP 2 | RESP 3 |
|---------|--------|--------|

| RESP3                   | Supported |
|-------------------------|-----------|
| Simple, Bulk, Verbatim  | ✅         |
| Integer, Double, BigInt | ✅         |
| Null, Boolean           | ✅         |
| Array, Map, Attributes  | ✅         |

## Command

### Redis-Cli

```shell
  redis-cli -h <ip-address> -p 6379 
```

### Syntax

| Command | Syntax    |
|---------|-----------|
| SET     | SET x 100 |
| GET     | GET x     |


