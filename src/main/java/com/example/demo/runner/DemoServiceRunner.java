package com.example.demo.runner;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.json.JsonData;
import com.example.demo.annotation.CustomerInfo;
import com.example.demo.entity.User;
import com.example.demo.interceptor.EncryptCommon;
import com.example.demo.sdk.IPlatformService;
import com.example.demo.sdk.dto.AuthRequest;
import com.example.demo.sdk.dto.AuthResponse;
import com.example.demo.sdk.dto.PlatformEnum;
import com.example.demo.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DemoServiceRunner implements ApplicationRunner, ApplicationContextAware {

	@Value("${mybatis.type-aliases-package}")
	private String entityPackage;

	@Autowired
	private ServerProperties serverProperties;

	@Autowired
	private IPlatformService platformService;

	@Autowired
	private ElasticsearchClient elasticsearchClient;

	private static ApplicationContext context;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}

	@Bean
	public User user() {
		return new User(3L, "张三", "123456", "12345678901", new Date());
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		scannerCustomInfo();
		consoleEnvironment();
		testRegisterBean();
		testSdk();
		testQuickSort();
		elasticsearchDemo();
	}

	private void testRegisterBean() {
		while (true) {
			String beanName = "user";
			User user = context.getBean(beanName, User.class);
			log.info("注册的用户Bean为：" + user.getUsername());
			if (!"张三".equals(user.getUsername())) {
				break;
			}
			DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
			beanFactory.removeBeanDefinition(beanName);
			User newUser = new User(4L, "李四", "123456", "12345678901", new Date());
			beanFactory.registerSingleton(beanName, newUser);
			beanFactory.autowireBean(newUser);
		}
	}

	private void elasticsearchDemo() throws IOException {
		String indexName = "test_users1";
		// 索引列表
		IndicesResponse indices = elasticsearchClient.cat().indices();
		String indexList = indices.valueBody().stream().map(IndicesRecord::index).collect(Collectors.joining(","));
		log.info("ES索引列表: {}", indexList);
		// 索引是否存在
		boolean exists = elasticsearchClient.indices().exists(a -> a.index(indexName)).value();
		if (exists) {
			// 删除索引
			elasticsearchClient.indices().delete(a -> a.index(indexName));
			log.info("ES索引[{}]已存在, 已删除", indexName);
		}
		// 创建索引
		elasticsearchClient.indices().create(b -> b
				.index(indexName)
				.mappings(m -> m
						.properties("id", p -> p.long_(d -> d))
						.properties("username", p -> p.text(t -> t))
						.properties("password", p -> p.keyword(d -> d))
						.properties("phoneNumber", p -> p.keyword(d -> d))
						.properties("birthday", p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss")))
				)
		);
		log.info("ES创建索引[{}]成功", indexName);
		// 获取索引
		GetIndexResponse getIndexResponse = elasticsearchClient.indices().get(a -> a.index(indexName));
		log.info("ES索引[{}]信息: {}", indexName, JsonUtil.writeValueAsString(getIndexResponse.result().get(indexName)));
		// 创建文档
		User user1 = new User(1L, "张三1", "123456", "12345678901", new Date());
		elasticsearchClient.index(b -> b.index(indexName).id(user1.getId().toString()).document(user1));
		User user2 = new User(2L, "李四黑", "456789", "13245678901", new Date());
		elasticsearchClient.index(b -> b.index(indexName).id(user2.getId().toString()).document(user2));
		User user3 = new User(3L, "王五", "78910JQ", "12435678901", new Date());
		elasticsearchClient.index(b -> b.index(indexName).id(user3.getId().toString()).document(user3));
		User user4 = new User(4L, "赵六质检员", "910JQKA", "98765432101", new Date());
		elasticsearchClient.index(b -> b.index(indexName).id(user4.getId().toString()).document(user4));
		log.info("ES插入数据成功");
		// 修改文档
		user1.setUsername("张三 黑马程序员");
		elasticsearchClient.update(b -> b.index(indexName).id(user1.getId().toString()).doc(user1), User.class);
		log.info("ES修改数据成功");
		// 删除文档
		elasticsearchClient.delete(b -> b.index(indexName).id(user3.getId().toString()));
		log.info("ES删除数据成功");

		// 查询文档
		SearchResponse<User> search = elasticsearchClient
				.search(b -> b.index("test_users")
						.query(q -> q.bool(b1 ->
								b1.must(q1 -> q1.match(m -> m.field("username").query("黑马程序员")))
										.should(q2 -> q2.term(t -> t.field("phoneNumber").value("13245678901")))
										.filter(a -> a.range(t -> t.field("id").gte(JsonData.of(0))))
						)), User.class
				);
		List<User> result = search.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
		log.info("ES查询结果：{}", JsonUtil.writeValueAsString(result));
	}

	private void testSdk() {
		AuthRequest authRequest = new AuthRequest();
		authRequest.setPlatform(PlatformEnum._1688);
		AuthResponse authResponse = platformService.refreshAccessToken(authRequest);
		log.info("refreshAccessToken: {}", JsonUtil.writeValueAsString(authResponse));
	}

	private void consoleEnvironment() throws UnknownHostException {
		String host = InetAddress.getLocalHost().getHostAddress();
		Integer port = serverProperties.getPort();
		String context = serverProperties.getServlet().getContextPath();
		String contextEndpoint = "http://" + host + ":" + port + context;
		log.info("contextEndpoint: {}", contextEndpoint);
	}

	private void scannerCustomInfo() {
		Reflections reflections = new Reflections(entityPackage);
		Set<Class<?>> customClassSet = reflections.getTypesAnnotatedWith(CustomerInfo.class);
		for (Class<?> clazz : customClassSet) {
			HashSet<String> propertySet = new HashSet<>();
			for (Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(CustomerInfo.class)) {
					propertySet.add(field.getName());
				}
			}
			EncryptCommon.CUSTOM_PROPERTY_MAP.put(clazz.getName(), propertySet);
		}
		log.info("Customer property collect result: {}", JsonUtil.writeValueAsString(EncryptCommon.CUSTOM_PROPERTY_MAP));
	}

	private void testQuickSort() {
		int[] a = {1, 6, 8, 4, 3, 7, 2, 9, 5};
		log.info("old: " + Arrays.toString(a));
		quickSort(a, 0, a.length - 1);
		log.info("new: " + Arrays.toString(a));
	}

	private void quickSort(int[] arr, int start, int end) {
		if (start >= end) {
			return;
		}
		int mid = getMid(arr, start, end);
		quickSort(arr, start, mid - 1);
		quickSort(arr, mid + 1, end);
	}

	private int getMid(int[] arr, int start, int end) {
		int p = arr[end];
		int pIndex = start;

		for (int i = start; i < end; i++) {
			if (arr[i] <= p) {
				swap(arr, i, pIndex);
				pIndex++;
			}
		}
		swap(arr, end, pIndex);
		return pIndex;
	}

	private void swap(int[] arr, int i, int j) {
		int temp = arr[i];
		arr[i] = arr[j];
		arr[j] = temp;
	}
}
