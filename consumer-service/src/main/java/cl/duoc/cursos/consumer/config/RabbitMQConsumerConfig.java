package cl.duoc.cursos.consumer.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConsumerConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${app.rabbitmq.queue}")
    private String queueName;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    @Value("${app.rabbitmq.dlx-exchange}")
    private String dlxExchangeName;

    @Value("${app.rabbitmq.dlq}")
    private String dlqName;

    @Value("${app.rabbitmq.dlq-routing-key}")
    private String dlqRoutingKey;

    // =========================================================================
    // INFRAESTRUCTURA DE LA COLA ACADÉMICA
    // =========================================================================

    @Bean
    Queue cursosQueue() { 
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", dlxExchangeName)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    DirectExchange cursosExchange() { 
        return new DirectExchange(exchangeName);
    }

    @Bean
    Binding cursosBinding() { 
        return BindingBuilder.bind(cursosQueue()).to(cursosExchange()).with(routingKey);
    }

    // =========================================================================
    // 2. INFRAESTRUCTURA DE LA DEAD LETTER QUEUE (DLQ)
    // =========================================================================

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(dlqName).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(dlxExchangeName);
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(dlqRoutingKey);
    }

    // =========================================================================
    // 3. SERIALIZACIÓN Y CONTENEDOR DE ESCUCHA ASÍNCRONA
    // =========================================================================

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter("cl.duoc.*");
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            final SimpleRabbitListenerContainerFactoryConfigurer configurer,
            final ConnectionFactory connectionFactory,
            final MessageConverter jsonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        
        // Si el Worker falla al procesar, manda el mensaje directo a la DLQ en vez de generar un bucle de reintentos
        factory.setDefaultRequeueRejected(false); 
        return factory;
    }
}