package io.spring.dataflow.sample.usagecostlogger;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import io.spring.dataflow.sample.domain.UsageCostDetail;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UsageCostLoggerApplicationTests {

	@Autowired
	protected Sink sink;

	@Autowired
	protected UsageCostLogger usageCostLogger;

	@Test
	public void contextLoads() {
	}

	@Test
	public void testUsageCostLogger() throws Exception {
		final ArgumentCaptor<UsageCostDetail> captor = ArgumentCaptor.forClass(UsageCostDetail.class);

		this.sink.input().send(MessageBuilder.withPayload("{\"userId\":\"user3\",\"callCost\":10.100000000000001,\"dataCost\":25.1}").build());

		verify(this.usageCostLogger).process(captor.capture());

		assertTrue(captor.getValue().getUserId().compareTo("user3")==0);

	}

	@EnableAutoConfiguration
	@EnableBinding(Sink.class)
	static class TestConfig {

		// Override `UsageCostLogger` bean for spying.
		@Bean
		@Primary
		public UsageCostLogger usageCostLogger() {
			return spy(new UsageCostLogger());
		}
	}


}
