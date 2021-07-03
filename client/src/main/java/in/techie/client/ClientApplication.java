package in.techie.client;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableRetry
public class ClientApplication {

	public static void main(String[] args) throws BeansException, InterruptedException, URISyntaxException {
		ApplicationContext context = SpringApplication.run(ClientApplication.class, args);
		//context.getBean(ClientService.class).simulateExpontentialAttempt();
		context.getBean(ClientService.class).callServerWithRetry();
	}

	@Bean
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

}

@Service
class ClientService {

	Logger LOGGER = LoggerFactory.getLogger(ClientService.class);

	@Autowired
	RestTemplate _rt;

	public void callServerBruteForce() throws InterruptedException, URISyntaxException {

		int maxRetry = 5;
		int retryAttempts = 0;

		while (retryAttempts < maxRetry) {
			LOGGER.info(String.format("Retry attempt: %d", retryAttempts + 1));
			try {
				ResponseEntity<String> responseFromServer = _rt.exchange(new URI("http://localhost:9001/generate"),
						HttpMethod.GET, null, String.class);
				LOGGER.info(String.format("Response recieved from Server: %s", responseFromServer.getBody()));
				break;
			} catch (RestClientException e) {
				LOGGER.error(String.format("Unable to connect to Server: %s", e.getMessage()));
				retryAttempts++;
				Thread.sleep(2000);
			}
		}

		if (retryAttempts == maxRetry) { // Recover if all retries are exhausted.
			LOGGER.info("Do something else, as client was not able to call Server.");
		}

	}

	@Retryable(recover = "recoverForServer", maxAttempts = 5, value = RestClientException.class, backoff = @Backoff(2000))
	public void callServerWithRetry() throws InterruptedException, URISyntaxException {

		LOGGER.info("Connecting to Server");
		ResponseEntity<String> responseFromServer = _rt.exchange(new URI("http://localhost:9001/generate"),
				HttpMethod.GET, null, String.class);
		LOGGER.info(String.format("Response recieved from Server: %s", responseFromServer.getBody()));

	}

	@Recover
	public void recoverForServer(RestClientException e) {
		LOGGER.info("Do something else, as client was not able to call Server.");
	}

	@Retryable(recover = "recoverSimulateExpontentialAttempt", maxAttempts = 5, value = RuntimeException.class, backoff = @Backoff(delay = 2000, multiplier = 2))
	public void simulateExpontentialAttempt() throws InterruptedException, URISyntaxException {
		LOGGER.info("Simulation for Server");
		throw new RuntimeException();
	}

	@Recover
	public void recoverSimulateExpontentialAttempt(RuntimeException e) {
		LOGGER.info("Simuation Failed, we exit.");
	}

}