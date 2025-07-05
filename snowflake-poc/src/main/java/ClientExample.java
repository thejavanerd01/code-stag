
import java.lang.System;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.*;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.*;

public class ClientExample {
  public static void main(String[] args) {
    try {
      String topic = "trade_topic_0";
      final Properties config = readConfig("client.properties");

      produce(topic, config);
      //consume(topic, config);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static Properties readConfig(final String configFile) throws IOException {
    // reads the client configuration from client.properties
    // and returns it as a Properties object
    if (!Files.exists(Paths.get(configFile))) {
      throw new IOException(configFile + " not found.");
    }

    final Properties config = new Properties();
    try (InputStream inputStream = new FileInputStream(configFile)) {
      config.load(inputStream);
    }

    return config;
  }

  public static void produce(String topic, Properties config) {
    // sets the message serializers
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

    // creates a new producer instance and sends a sample message to the topic
    String key = "key";
    String json = """
{
  "asset_id": "A1002",
  "symbol": "Apple Inc.",
  "exchange": "NSE",
  "currency": "INR",
  "price": 2785.50,
  "price_time": "2025-06-16T14:30:00Z",
  "source": "Reuters",
  "created_by": "system",
  "created_at": "2025-06-16T14:30:05Z"
}
""";

    Producer<String, String> producer = new KafkaProducer<>(config);
    producer.send(new ProducerRecord<>(topic, json));
    System.out.println(
        String.format(
            "Produced message to topic %s: key = %s value = %s", topic, key, json));

    // closes the producer connection
   producer.close();
   producer.close();
  }

  public static void consume(String topic, Properties config) {
    // sets the group ID, offset and message deserializers
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "java-group-1");
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

    // creates a new consumer instance and subscribes to messages from the topic
    KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config);
    consumer.subscribe(Arrays.asList(topic));

    while (true) {
      // polls the consumer for new messages and prints them
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
      for (ConsumerRecord<String, String> record : records) {
        System.out.println(
            String.format(
                "Consumed message from topic %s: key = %s value = %s", topic, record.key(), record.value()));
      }
    }
  }
}
