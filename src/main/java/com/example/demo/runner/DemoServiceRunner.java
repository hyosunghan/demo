package com.example.demo.runner;

import cn.hutool.core.io.FileUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.cat.indices.IndicesRecord;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.GetIndexResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import com.example.demo.annotation.CustomerInfo;
import com.example.demo.entity.Users;
import com.example.demo.interceptor.EncryptCommon;
import com.example.demo.service.TestService;
import com.example.demo.service.TestServiceImpl;
import com.example.demo.utils.BitPermission;
import com.example.demo.utils.JsonUtil;
import com.example.demo.utils.MyInvocationHandler;
import com.example.demo.utils.RestUtil;
import com.example.demo._identity.SnowFlakeIdentity;
import com.example.demo.utils.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DemoServiceRunner implements ApplicationRunner {

	@Value("${mybatis.type-aliases-package}")
	private String entityPackage;

	@Autowired
	private ServerProperties serverProperties;

	@Autowired
	private ManagementServerProperties managementServerProperties;

	@Autowired
	private ElasticsearchClient elasticsearchClient;

	@Autowired
	private TestService testService;

	@Autowired
	private DataSource dataSource;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		scannerCustomInfo();
		consoleEnvironment();
//		testSpringUtil();
//		testBitPermission();
//		testMyInvocationHandler();
//		testQuickSort();
//		testRestUtil();
//		testJsonUtil();
//		testSnowFlakeIdentity();
//		testRedisLock();
//		String filePath = "/Users/hanxiaoxing/mydata/test.txt";
//		testMockTestFile(filePath, 1000001);
//		testWriteFileToTable(filePath);
//		testElasticsearch();
	}


	private void testRedisLock() {
		log.info("-----------------------------------------------------------测试RedisLock");
		Users users = new Users(1L, "张三", "123456", "12345678901", new Date(), 1);
		new Thread(()-> testService.testLock(1L, users)).start();
		testService.testLock(1L, users);
	}

	private void testSnowFlakeIdentity() {
		log.info("-----------------------------------------------------------测试雪花算法");
		long l = SnowFlakeIdentity.getInstance().nextId();
		log.info("生成id为：{}", l);
	}

	private void testJsonUtil() {
		log.info("-----------------------------------------------------------测试JsonUtil");
		HashMap<String, String> stringStringHashMap = new HashMap<>();
		stringStringHashMap.put("name", "张三");
		String s = JsonUtil.writeValueAsString(stringStringHashMap);
		log.info("请求结果为: {}", s);
	}

	private void testRestUtil() {
		log.info("-----------------------------------------------------------测试RestUtil");
		String s = RestUtil.get("https://www.baidu.com", String.class);
		log.info("请求结果为: {}", s);
	}

	private void testMyInvocationHandler() {
		log.info("-----------------------------------------------------------测试动态代理");
		TestService testService = new TestServiceImpl();
		testService.testProxy();

		TestService proxyService = (TestService) MyInvocationHandler.getProxy(testService,
				method -> System.out.println("对象方法执行前" + method.getName()),
				method -> System.out.println("对象方法执行后" + method.getName()));
		proxyService.testProxy();
	}

	private void testBitPermission() {
		log.info("-----------------------------------------------------------测试位运算");
		BitPermission bitPermission = new BitPermission();
		bitPermission.setPermissions(BitPermission.PERMISSION_INSERT | BitPermission.PERMISSION_DELETE
				| BitPermission.PERMISSION_UPDATE | BitPermission.PERMISSION_SELECT);
		log.info("初始化权限增删改查{}", Integer.toBinaryString(bitPermission.getPermissions()));
		bitPermission.disablePermissions(BitPermission.PERMISSION_INSERT | BitPermission.PERMISSION_DELETE);
		log.info("取消权限增删{}", Integer.toBinaryString(bitPermission.getPermissions()));
		bitPermission.enablePermissions(BitPermission.PERMISSION_INSERT);
		log.info("追加权限增{}", Integer.toBinaryString(bitPermission.getPermissions()));
		log.info("是否允许增{}", bitPermission.isAllow(BitPermission.PERMISSION_INSERT));
		log.info("是否不允许增{}", bitPermission.isNotAllow(BitPermission.PERMISSION_INSERT));
	}

	private void testMockTestFile(String filePath, int count) {
		log.info("-----------------------------------------------------------测试模拟生成文件");
		FileUtil.del(filePath);
		long l = System.currentTimeMillis();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String dateTime = formatter.format(LocalDateTime.now());
		ArrayList<String> list = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			StringBuilder line = new StringBuilder();
			UUID uuid = UUID.randomUUID();
			line.append(i).append("|")
					.append(uuid).append("|")
					.append(uuid).append("|")
					.append(uuid).append("|")
					.append(dateTime);
			list.add(line.toString());
			if (i % 100000 == 0 || i == count) {
				FileUtil.writeLines(list, filePath, StandardCharsets.UTF_8, true);
				list.clear();
				log.info("已写入" + i + "行数据");
			}
		}
		log.info("生成文件{}行，用时{}ms", count, System.currentTimeMillis() - l);
	}

	/**
	 * 写入文件到表
	 * 配置
	 * 	1.服务端：set global local_infile = 1;（查看：SHOW VARIABLES LIKE 'local_infile'）
	 * 	2.客户端：url:jdbc:...&allowLoadLocalInfile=true
	 * 测试数据：
	 *  1万：0.7秒
	 * 	10万：2.9秒
	 * 	100万：23.7秒
	 * 	1000万：247.3秒
	 *
	 * @throws SQLException
	 */
	private void testWriteFileToTable(String filePath) throws SQLException {
		log.info("-----------------------------------------------------------测试写文件到数据表");
		long l = System.currentTimeMillis();
		Connection connection = dataSource.getConnection();
		// 下列SQL去掉LOCAL后即为读取数据库本地文件的处理方式，需使用SHOW VARIABLES LIKE 'secure_file_priv'查看数据库允许路径
		String sql = "LOAD DATA LOCAL INFILE ? INTO TABLE users FIELDS TERMINATED BY '|' LINES TERMINATED BY '\n'";
		PreparedStatement pstmt = connection.prepareStatement(sql);
		pstmt.setString(1, filePath);
		pstmt.execute();
		log.info("导入文件到数据库, 用时{}ms", System.currentTimeMillis() - l);
	}

	private void testSpringUtil() {
		log.info("-----------------------------------------------------------测试SpringUtil");
		String beanName = "user";
		Users users = new Users(1L, "张三", "123456", "12345678901", new Date(), 1);
		SpringUtil.registerBean(beanName, users);
		users = SpringUtil.getBean(beanName, Users.class);
		log.info("注册的用户Bean为：" + users.getUsername());
		Users newUsers = new Users(4L, "李四", "123456", "12345678901", new Date(), 1);
		SpringUtil.replaceBean(beanName, newUsers);
		newUsers = SpringUtil.getBean(beanName, Users.class);
		log.info("替换的用户Bean为：" + newUsers.getUsername());
	}

	private void testElasticsearch() throws IOException {
		log.info("-----------------------------------------------------------测试Elasticsearch");
		String indexName = "test_users";
		// 索引列表
		IndicesResponse indices = elasticsearchClient.cat().indices();
		String indexList = indices.valueBody().stream().map(IndicesRecord::index).collect(Collectors.joining(","));
		log.info("ES索引列表: {}", indexList);
		// 索引是否存在
		boolean exists = elasticsearchClient.indices().exists(a -> a.index(indexName)).value();
		if (exists) {
//			// 删除索引
//			elasticsearchClient.indices().delete(a -> a.index(indexName));
//			log.info("ES索引[{}]已存在, 已删除", indexName);
		} else {
			// 创建索引
			elasticsearchClient.indices().create(b -> b
					.index(indexName)
					.mappings(m -> m
							.properties("id", p -> p
									.long_(d -> d)
							)
							.properties("username", p -> p
									.text(t -> t
											.analyzer("ik_smart")
											.searchAnalyzer("ik_max_word")
											.fields("keyword", f -> f
													.keyword(kw -> kw)
											)
									)
							)
							.properties("password", p -> p
									.keyword(d -> d)
							)
							.properties("phoneNumber", p -> p
									.keyword(d -> d)
							)
							.properties("birthday", p -> p
									.date(d -> d
											.format("yyyy-MM-dd HH:mm:ss")
									)
							)
					)
			);
			log.info("ES创建索引[{}]成功", indexName);
			// 获取索引
			GetIndexResponse getIndexResponse = elasticsearchClient.indices().get(a -> a.index(indexName));
			log.info("ES索引[{}]信息: {}", indexName, JsonUtil.writeValueAsString(getIndexResponse.result().get(indexName)));
			// 创建文档
			Users users1 = new Users(1L, "张三1", "123456", "12345678901", new Date(), 1);
			elasticsearchClient.index(b -> b.index(indexName).id(users1.getId().toString()).document(users1));
			Users users2 = new Users(2L, "李四黑", "456789", "13245678901", new Date(), 1);
			elasticsearchClient.index(b -> b.index(indexName).id(users2.getId().toString()).document(users2));
			Users users3 = new Users(3L, "王五", "78910JQ", "12435678901", new Date(), 1);
			elasticsearchClient.index(b -> b.index(indexName).id(users3.getId().toString()).document(users3));
			Users users4 = new Users(4L, "赵六质检员", "910JQKA", "98765432101", new Date(), 1);
			elasticsearchClient.index(b -> b.index(indexName).id(users4.getId().toString()).document(users4));
			log.info("ES插入数据成功");
			// 修改文档
			users1.setUsername("张三 黑马程序员");
			elasticsearchClient.update(b -> b.index(indexName).id(users1.getId().toString()).doc(users1), Users.class);
			log.info("ES修改数据成功");
			// 删除文档
			elasticsearchClient.delete(b -> b.index(indexName).id(users3.getId().toString()));
			log.info("ES删除数据成功");
		}

//		// 查询文档
//		BoolQuery.Builder bool = QueryBuilders.bool();
//		bool.must(q -> q.term(m -> m.field("username").value("张三")));
//		bool.must(q -> q.match(m -> m.field("username").query("张三")));
//		bool.must(q -> q.range(r -> r.field("id").gte(JsonData.of(0))));
//		bool.should(q -> q.match(m -> m.field("username").query("张三")));
//		bool.filter(q -> q.match(m -> m.field("username").query("张三")));
//		BoolQuery query = bool.build();
//		SearchResponse<User> search = elasticsearchClient.search(b -> b.index(indexName).query(query._toQuery()), User.class);

		Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>> function = a -> a
				.index(indexName)
				.query(query -> query
						.bool(b -> b
								.must(q -> q.term(m -> m.field("username").value("张三")))
								.must(q -> q.match(m -> m.field("username").query("张三")))
								.must(q -> q.range(r -> r.field("id").gte(JsonData.of(0))))
								.should(q -> q.match(m -> m.field("username").query("张三")))
								.filter(q -> q.match(m -> m.field("username").query("张三")))
						));
		SearchResponse<Users> search1 = elasticsearchClient.search(function, Users.class);

		List<Users> result = search1.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
		log.info("ES查询结果：{}", JsonUtil.writeValueAsString(result));
	}

	private void consoleEnvironment() throws UnknownHostException {
		String host = InetAddress.getLocalHost().getHostAddress();
		Integer port = serverProperties.getPort();
		Integer managePort = managementServerProperties.getPort();
		String context = serverProperties.getServlet().getContextPath();
		String manageContext = managementServerProperties.getServlet().getContextPath();
		String contextEndpoint = "http://" + host + ":" + port + context;
		log.info("contextEndpoint: {}", contextEndpoint);
		String swaggerEndpoint = "http://" + host + ":" + port + context + "/swagger-ui.html";
		log.info("swaggerEndpoint: {}", swaggerEndpoint);
		String actuatorEndpoint = "http://" + host + ":" + managePort + manageContext + "/actuator";
		System.setProperty("x.actuatorEndpoint", actuatorEndpoint);
		log.info("actuatorEndpoint: {}", actuatorEndpoint);
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
		log.info("-----------------------------------------------------------测试快速排序");
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
