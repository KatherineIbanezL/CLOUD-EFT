package cl.duoc.cursos.producer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import cl.duoc.cursos.shared.events.ExamenRendidoEvent;
import cl.duoc.cursos.shared.events.InscripcionCreadaEvent;

@Service
public class PlataformaProducerService {

    private static final Logger log = LoggerFactory.getLogger(PlataformaProducerService.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;
    private final String examRoutingKey;

    // Innyección por constructor
    public PlataformaProducerService(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchange,
            @Value("${app.rabbitmq.routing-key}") String routingKey,
            @Value("${app.rabbitmq.exam-routing-key}") String examRoutingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.examRoutingKey = examRoutingKey;
    }

    // Método para despachar las inscripciones
    public void enviarEventoInscripcion(InscripcionCreadaEvent event) {
        log.info("Publicando inscripción en RabbitMQ. InscripcionID={}, Alumno={}, CursoID={}", 
                 event.inscripcionId(), event.estudiante(), event.cursoId());
                 
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

    // Método para despachar las evaluaciones asíncronas
    public void enviarEventoExamen(ExamenRendidoEvent event) {
        log.info("Publicando examen rendido en RabbitMQ. ExamenID={}, InscripcionID={}", 
                 event.examenId(), event.inscripcionId());
        
        rabbitTemplate.convertAndSend(exchange, examRoutingKey, event);
    }
}