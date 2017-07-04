wget http://apache-mirror.rbc.ru/pub/apache/kafka/0.10.2.1/kafka_2.11-0.10.2.1.tgz
tar -zxvf kafka_2.11-0.10.2.1.tgz
rm kafka_2.11-0.10.2.1.tgz
ln -s kafka_2.11-0.10.2.1 kafka

wget http://download.redis.io/releases/redis-3.2.8.tar.gz
tar -zxvf redis-3.2.8.tar.gz 
rm redis-3.2.8.tar.gz 
ln -s redis-3.2.8 redis
cd redis
make