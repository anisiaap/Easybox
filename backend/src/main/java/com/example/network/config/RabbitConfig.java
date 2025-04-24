package com.example.network.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class RabbitConfig {

    // Define a MessageConverter bean (using Jackson2JsonMessageConverter)
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // Set manual acknowledgment mode so that our consumer can control ack/nack
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // Ensure that failed messages are not requeued (so they can be dead-lettered)
        factory.setDefaultRequeueRejected(false);

        // Create and configure a RetryTemplate with exponential backoff
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(10000); // 10 seconds initial delay
        backOffPolicy.setMultiplier(2.0);         // multiplier
        backOffPolicy.setMaxInterval(30000);        // maximum delay: 30 seconds
        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3); // maximum 3 attempts
        retryTemplate.setRetryPolicy(retryPolicy);

        factory.setRetryTemplate(retryTemplate);
        return factory;
    }
}
