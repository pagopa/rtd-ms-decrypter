package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;

import java.util.function.Consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import org.springframework.messaging.Message;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.Decrypter;



@Configuration
@Slf4j
public class EventHandler {
  
  @Bean
	public Consumer<Message<List<EventGridEvent>>> blobStorage(Decrypter decrypter) {
		return message -> message.getPayload().forEach((final EventGridEvent e) -> {
			log.info("Intercepted Event. Subject: {}. Type: {}.", e.getSubject(), e.getEventType());
			decrypter.decrypt(e.getSubject());
		});
	}
}