
CP=./classes

for i in libs/*.jar
do
    CP=$CP:./${i}
done

java -Dlog4j.configuration=file:./conf/log4j.properties -classpath $CP org.demo.StockServer $*