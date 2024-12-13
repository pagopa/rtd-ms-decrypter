package it.gov.pagopa.rtd.ms.rtdmsdecrypter;

import it.gov.pagopa.rtd.ms.rtdmsdecrypter.model.*;
import lombok.SneakyThrows;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Spring Boot application's entry point.
 */
@SpringBootApplication
@ImportRuntimeHints(RtdMsDecrypterApplication.RtdMsDecrypterRuntimeHints.class)
public class RtdMsDecrypterApplication {
    private static final String BUILD_METHOD = "build";

    public static void main(String[] args) {
        SpringApplication.run(RtdMsDecrypterApplication.class, args);
    }

    static class RtdMsDecrypterRuntimeHints implements RuntimeHintsRegistrar {

        @SneakyThrows
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection()
                    .registerConstructor(
                            ContractMethodAttributes.ContractMethodAttributesBuilder.class.getDeclaredConstructor(), ExecutableMode.INVOKE
                    )
                    .registerConstructor(
                            AdeTransactionsAggregate.AdeTransactionsAggregateBuilder.class.getDeclaredConstructor(), ExecutableMode.INVOKE
                    )
                    .registerConstructor(
                            RtdTransaction.RtdTransactionBuilder.class.getDeclaredConstructor(), ExecutableMode.INVOKE
                    )
                    .registerConstructor(
                            WalletContract.WalletContractBuilder.class.getDeclaredConstructor(), ExecutableMode.INVOKE
                    )
                    .registerConstructor(
                            WalletExportHeader.WalletExportHeaderBuilder.class.getDeclaredConstructor(), ExecutableMode.INVOKE
                    )
                    .registerMethod(
                            AdeTransactionsAggregate.AdeTransactionsAggregateBuilder.class.getMethod(BUILD_METHOD), ExecutableMode.INVOKE
                    )
                    .registerMethod(
                            ContractMethodAttributes.ContractMethodAttributesBuilder.class.getMethod(BUILD_METHOD), ExecutableMode.INVOKE
                    )
                    .registerMethod(
                            RtdTransaction.RtdTransactionBuilder.class.getMethod(BUILD_METHOD), ExecutableMode.INVOKE
                    )
                    .registerMethod(
                            WalletContract.WalletContractBuilder.class.getMethod(BUILD_METHOD), ExecutableMode.INVOKE
                    )
                    .registerMethod(
                            WalletExportHeader.WalletExportHeaderBuilder.class.getMethod(BUILD_METHOD), ExecutableMode.INVOKE
                    );
        }
    }

}