 ps aux | grep -v grep | grep 'redis-server\|kafka.Kafka\|QuorumPeerMain' | awk '{print $2}' | xargs kill -9