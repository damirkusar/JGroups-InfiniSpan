
CP=./classes

for i in libs/*.jar
do
    CP=$CP:./${i}
done

CP=./conf:$CP

FLAGS="-Djava.net.preferIPv4Stack=true -Dlog4j.configuration=file:./conf/log4j.properties"

java $FLAGS  -classpath $CP org.demo.ReplicatedStockServer $*