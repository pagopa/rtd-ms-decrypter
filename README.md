# rtd-ms-decrypter

# Read Me First

The following was discovered as part of building this project:

* The original package name 'it.gov.pagopa.rtd.ms.rtd-ms-decrypter' is invalid and this project
  uses 'it.gov.pagopa.rtd.ms.rtdmsdecrypter' instead.

# Hyper-parameters of Microservice

| Variable name       | Default    | Accepted      | Description                                                                                                                                    |
|---------------------|------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| CONSUMER_TIMEOUT_MS | 60000 (ms) | > 10000 (ms)  | Define the interval between each consumer `poll` call. This parameter define the max long running time of kafka consumer for a single message. |
| ENABLE_CHUNK_UPLOAD | false      | (true, false) | Enable/disable the upload of chunks to blob storage. The upload triggers other pipelines, so be sure of what happens when it is enabled        |

# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.6.3/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.6.3/maven-plugin/reference/html/#build-image)
