package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;

import java.util.function.Consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import lombok.Getter;
import java.util.List;
import org.springframework.messaging.Message;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.BlobApplicationAware;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnector;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.Decrypter;

import java.util.stream.Collectors;

@Configuration
@Getter
public class EventHandler {

	@Bean
	public Consumer<Message<List<EventGridEvent>>> blobStorageConsumer(Decrypter decrypter,
			BlobRestConnector blobRestConnector) {

		return message -> message.getPayload().stream()
				.filter(e -> "Microsoft.Storage.BlobCreated".equals(e.getEventType()))
				.map(EventGridEvent::getSubject)
				.map(BlobApplicationAware::new)
				.filter(b -> !BlobApplicationAware.Application.NOAPP.equals(b.getApp()))
				.map(blobRestConnector::get)
				.map(decrypter::decrypt)
				.map(blobRestConnector::put)
				.collect(Collectors.toList());
	}

}