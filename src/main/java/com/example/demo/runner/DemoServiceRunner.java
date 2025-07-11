package com.example.demo.runner;

import cn.hutool.core.util.StrUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class DemoServiceRunner implements ApplicationRunner {

	@Value("${mybatis.type-aliases-package}")
	private String entityPackage;

	@Value("${spring.datasource.url}")
	private String dbUrl;

	@Value("${spring.datasource.username}")
	private String dbUsername;

	@Value("${spring.datasource.password}")
	private String dbPassword;

	@Autowired
	private ServerProperties serverProperties;

	@Autowired
	private Environment environment;

	@Autowired
	private IPlatformService platformService;

	@Override
	public void run(ApplicationArguments args) throws Exception {
		scannerCustomInfo();
		consoleEnvironment();
//		testInsertAndUpdate();
//		testSdk();
//		testQuickSort();
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

	private void testInsertAndUpdate() throws SQLException {
		int limit = 1000;
		int page = 10;
		String table = "USERS";
		String className = User.class.getName();
		Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);

		String[] construct = constructQuery(conn, String.format("SELECT * from %s;", table));
		String key = construct[0];
		String spliceColumns = Arrays.stream(construct, 0, construct.length).collect(Collectors.joining(","));
		long l = System.currentTimeMillis();
		log.info("查询表[{}]列：[{}]。", table, String.join(",", construct));

		IntStream.range(0, page).parallel().forEach(p -> {
			String columns = Arrays.stream(construct, 1, construct.length).collect(Collectors.joining(",", "(", ")"));
			String values = IntStream.range(0, limit).parallel().mapToObj(r ->
					IntStream.range(1, construct.length).mapToObj(c -> {
						String column = construct[c];
						int pLength = String.valueOf(p).length();
						int rLength = String.valueOf(r).length();
						String value = "131" + ("0000" + p).substring(pLength) + ("0000" + r).substring(rLength);
						boolean isCustomerInfo = EncryptCommon.CUSTOM_PROPERTY_MAP.get(className).contains(StrUtil.toCamelCase(column.toLowerCase()));
						return isCustomerInfo ? EncryptCommon.encrypt(value) : value;
					}).collect(Collectors.joining("','", "('", "')"))
			).collect(Collectors.joining(","));
			execute(conn, String.format("INSERT INTO %s %s VALUES %s;", table, columns, values));
		});
		long l1 = System.currentTimeMillis();
		log.info("插入表[{}]数据{}条, 耗时{}毫秒。", table, limit * page, l1 - l);

		List<String[]> strings = executeQuery(conn, String.format("select count(1),min(%s),max(%s) from %s;", key, key, table));
		int count = Integer.parseInt(strings.get(0)[0]);
		int min = Integer.parseInt(strings.get(0)[1]);
		int max = Integer.parseInt(strings.get(0)[2]);
		int idCount = count == 0 ? 0 : max - min + 1;
		page = (idCount / limit) + (idCount % limit == 0 ? 0 : 1);
		long l2 = System.currentTimeMillis();
		log.info("计数表[{}]数据{}条，分为{}页。", table, count, page);

		int sum1 = IntStream.range(0, page).parallel().map(p -> {
			List<String[]> pageResult = executeQuery(conn, String.format("SELECT %s FROM %s where %s between %s and %s", spliceColumns, table, key, getStart(p, min, limit), getEnd(p, min, limit)));
			String updateSql = pageResult.parallelStream().map(line -> {
				String values = IntStream.range(1, construct.length).mapToObj(c -> {
					String column = construct[c];
					boolean isCustomerInfo = EncryptCommon.CUSTOM_PROPERTY_MAP.get(className).contains(StrUtil.toCamelCase(column.toLowerCase()));
					String value = isCustomerInfo ? EncryptCommon.decrypt(line[c]) : line[c];
					value = value.replaceFirst("3", "4");
					return String.format("%s='%s'", column, isCustomerInfo ? EncryptCommon.encrypt(value) : value);
				}).collect(Collectors.joining(","));
				return String.format("UPDATE %s SET %s WHERE %s=%s;", table, values, key, line[0]);
			}).collect(Collectors.joining());
			execute(conn, updateSql);
			log.info("查询并更新表[{}]第{}页, 包含{}条。", table, p, pageResult.size());
			return pageResult.size();
		}).sum();
		long l3 = System.currentTimeMillis();
		log.info("查询并更新表[{}]数据{}条，耗时{}毫秒。", table, sum1, l3 - l2);

		int sum2 = IntStream.range(0, page).parallel().map(p -> {
			int start = getStart(p, min, limit);
			int end = getEnd(p, min, limit);
			String selectSql = String.format("SELECT %s FROM %s WHERE %s BETWEEN %s AND %s;", spliceColumns, table, key, start, end);
			List<String[]> pageResult = executeQuery(conn, selectSql);
			String deleteSql = String.format("DELETE FROM %s WHERE %s BETWEEN %s AND %s;", table, key, start, end);
			execute(conn, deleteSql);
			String values = pageResult.parallelStream().map(line -> {
				String otherValues = IntStream.range(1, construct.length).mapToObj(c -> {
					String column = construct[c];
					boolean isCustomerInfo = EncryptCommon.CUSTOM_PROPERTY_MAP.get(className).contains(StrUtil.toCamelCase(column.toLowerCase()));
					String value = isCustomerInfo ? EncryptCommon.decrypt(line[c]) : line[c];
					value = value.replaceFirst("4", "5");
					return isCustomerInfo ? EncryptCommon.encrypt(value) : value;
				}).collect(Collectors.joining("','", "'", "'"));
				return String.format("(%s,%s)", line[0], otherValues);
			}).collect(Collectors.joining(","));
			String insertSql = String.format("INSERT INTO %s (%s) VALUES %s;", table, spliceColumns, values);
			execute(conn, insertSql);
			log.info("查询并删除重写表[{}]主键区间[{},{}]，包含{}条。", table, start, end, pageResult.size());
			return pageResult.size();
		}).sum();
		long l4 = System.currentTimeMillis();
		log.info("查询并删除重写表[{}]数据{}条，耗时{}毫秒。", table, sum2, l4 - l3);

		conn.close();
	}

	private static int getEnd(int p, int min, int limit) {
		return min + (p + 1) * limit - 1;
	}

	private static int getStart(int p, int min, int limit) {
		return min + p * limit;
	}

	private String[] constructQuery(Connection connection, String selectSql) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(selectSql);
			ResultSet resultSet = preparedStatement.executeQuery();
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			String[] columnArray = new String[columnCount];
			int arIndex = 1;
			for (int dbIndex = 1; dbIndex <= columnCount; dbIndex++) {
				if (metaData.isAutoIncrement(dbIndex)) {
					columnArray[0] = metaData.getColumnName(dbIndex);
				} else {
					columnArray[arIndex++] = metaData.getColumnName(dbIndex);
				}
			}
			return columnArray;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
    }

	private static List<String[]> executeQuery(Connection connection, String selectSql) {
		ArrayList<String[]> pageResult = new ArrayList<>();
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(selectSql);
			ResultSet resultSet = preparedStatement.executeQuery();
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			while (resultSet.next()) {
				String[] line = new String[columnCount];
				for (int i = 0; i < columnCount; i++) {
					line[i] = resultSet.getString(i + 1);
				}
				pageResult.add(line);
			}
		} catch (SQLException e) {
            throw new RuntimeException(e);
        }
		return pageResult;
    }

	private static void execute(Connection connection, String sql) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.execute();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
