package it.gov.pagopa.rtd.ms.rtdmsdecrypter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;
import org.springframework.messaging.Message;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.EventGridEvent;

@SpringBootApplication
@Slf4j
public class RtdMsDecrypterApplication {

	public static void main(String[] args) {
		SpringApplication.run(RtdMsDecrypterApplication.class, args);

	}

	@Bean
	public Consumer<Message<List<EventGridEvent>>> decrypt() {
		return message -> message.getPayload().forEach((final EventGridEvent e) -> log.info("\n{}\n", e.getTopic()));
	}

}