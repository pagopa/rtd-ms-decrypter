package it.gov.pagopa.rtd.ms.rtdmsdecrypter.event;

import java.util.function.Consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import org.springframework.messaging.Message;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.BlobRestConnector;
import it.gov.pagopa.rtd.ms.rtdmsdecrypter.service.Decrypter;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Configuration
@Slf4j
@Getter
public class EventHandler {

	private Pattern eventPattern = Pattern.compile("^.*(ade|rtd)-transactions-[a-z0-9]{44}.*");

	@Bean
	public Consumer<Message<List<EventGridEvent>>> blobStorageConsumer(Decrypter decrypter,
			BlobRestConnector blobRestConnector) {

		return message -> message.getPayload().stream()
				.peek(e -> log.info("Intercepted event {} on {}", e.getEventType(), e.getSubject()))
				.filter(e -> "Microsoft.Storage.BlobCreated".equals(e.getEventType()))
				.map(EventGridEvent::getSubject)
				.filter(s -> this.eventPattern.matcher(s).matches())
				.map(blobRestConnector::get)
				.map(decrypter::decrypt)
				.map(blobRestConnector::put)
				.collect(Collectors.toList());
	}

}