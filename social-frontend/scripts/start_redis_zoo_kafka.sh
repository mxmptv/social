nohup ./redis/src/redis-server > redis.log &
nohup kafka/bin/zookeeper-server-start.sh kafka/config/zookeeper.properties > zoo.log &
sleep 1s
nohup kafka/bin/kafka-server-start.sh kafka/config/server.properties > kafka.log &

echo "redis, zookeeper, kafka started"