package jk.zugsteuerung.zug;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;

import jk.zugsteuerung.zug.WebConfig;

@RunWith(SpringRunner.class)
public class WebConfigTest {

	@Mock
	private AsyncSupportConfigurer mockAsyncSupportConfigurer;
	
	@Test
	public void testConfigureAsyncSupport() {
		WebConfig webConfig = new WebConfig();
		webConfig.configureAsyncSupport(mockAsyncSupportConfigurer);
		ArgumentCaptor<AsyncTaskExecutor> asyncTaskExecutorCaptor = ArgumentCaptor.forClass(AsyncTaskExecutor.class);
		verify(mockAsyncSupportConfigurer).setTaskExecutor(asyncTaskExecutorCaptor.capture());
		assertFalse(asyncTaskExecutorCaptor.getValue() instanceof ThreadPoolTaskExecutor);
	}
	
}
