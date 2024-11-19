package com.example.demo.runner;

import com.example.demo.annotation.CustomerInfo;
import com.example.demo.interceptor.EncryptCommon;
import com.example.demo.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
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

	@Override
	public void run(ApplicationArguments args) throws Exception {
		scannerCustomInfo();
//		testInsertAndUpdate();
	}

	private void scannerCustomInfo() {
		String packageName = "com.example.demo.entity";
		Reflections reflections = new Reflections(packageName);
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

	private void testInsertAndUpdate() throws SQLException {
		int limit = 1000;
		int page = 10;
		String table = "USERS";
		Connection conn = DriverManager.getConnection("jdbc:h2:mem:test;mode=mysql", "sa", "sa");

		String[] construct = constructQuery(conn, String.format("SELECT * from %s;", table));
		String spliceColumns = Arrays.stream(construct, 0, construct.length).collect(Collectors.joining(","));
		long l = System.currentTimeMillis();
		log.info("查询表[{}]列：[{}]。", table, String.join(",", construct));

		IntStream.range(0, page).parallel().forEach(p -> {
			String columns = Arrays.stream(construct, 1, construct.length).collect(Collectors.joining(",", "(", ")"));
			String values = IntStream.range(0, limit).parallel().mapToObj(r ->
					IntStream.range(1, construct.length).mapToObj(c -> EncryptCommon.encrypt("0000")).collect(Collectors.joining("','", "('", "')"))
			).collect(Collectors.joining(","));
			execute(conn, String.format("INSERT INTO %s %s VALUES %s;", table, columns, values));
		});
		long l1 = System.currentTimeMillis();
		log.info("插入表[{}]数据{}条, 耗时{}毫秒。", table, limit * page, l1 - l);

		List<String[]> strings = executeQuery(conn, String.format("select count(1) from %s;", table));
		int count = Integer.parseInt(strings.get(0)[0]);
		page = (count / limit) + (count % limit == 0 ? 0 : 1);
		long l2 = System.currentTimeMillis();
		log.info("计数表[{}]数据{}条，分为{}页。", table, count, page);

		int sum1 = IntStream.range(0, page).parallel().map(p -> {
			List<String[]> pageResult = executeQuery(conn, String.format("SELECT %s FROM %s LIMIT %s,%s", spliceColumns, table, p * limit, limit));
			String updateSql = pageResult.parallelStream().map(line -> {
				String values = IntStream.range(1, construct.length).mapToObj(i -> String.format("%s='%s'", construct[i], EncryptCommon.encrypt("1111"))).collect(Collectors.joining(","));
				return String.format("UPDATE %s SET %s WHERE %s=%s;", table, values, construct[0], line[0]);
			}).collect(Collectors.joining());
			execute(conn, updateSql);
			log.info("查询并更新表[{}]第{}页, 包含{}条。", table, p, pageResult.size());
			return pageResult.size();
		}).sum();
		long l3 = System.currentTimeMillis();
		log.info("查询并更新表[{}]数据{}条，耗时{}毫秒。", table, sum1, l3 - l2);

		int sum2 = IntStream.range(0, page).parallel().mapToObj(p -> {
			List<String[]> pageIds = executeQuery(conn, String.format("SELECT %s FROM %s ORDER BY %s ASC LIMIT %s,%s;", construct[0], table, construct[0], p * limit, limit));
			return new Long[]{Long.valueOf(pageIds.get(0)[0]), Long.valueOf(pageIds.get(pageIds.size() - 1)[0])};
		}).collect(Collectors.toList()).parallelStream().mapToInt(range -> {
			String selectSql = String.format("SELECT %s FROM %s WHERE %s BETWEEN %s AND %s;", spliceColumns, table, construct[0], range[0], range[1]);
			List<String[]> pageResult = executeQuery(conn, selectSql);
			String deleteSql = String.format("DELETE FROM %s WHERE %s BETWEEN %s AND %s;", table, construct[0], range[0], range[1]);
			execute(conn, deleteSql);
			String values = pageResult.parallelStream().map(r -> {
				String otherValues = IntStream.range(1, construct.length).mapToObj(c -> EncryptCommon.encrypt("2222")).collect(Collectors.joining("','", "'", "'"));
				return String.format("(%s,%s)", r[0], otherValues);
			}).collect(Collectors.joining(","));
			String insertSql = String.format("INSERT INTO %s (%s) VALUES %s;", table, spliceColumns, values);
			execute(conn, insertSql);
			log.info("查询并删除重写表[{}]主键区间[{},{}]，包含{}条。", table, range[0], range[1], pageResult.size());
			return pageResult.size();
		}).sum();
		long l4 = System.currentTimeMillis();
		log.info("查询并删除重写表[{}]数据{}条，耗时{}毫秒。", table, sum2, l4 - l3);

		conn.close();
	}

	private String[] constructQuery(Connection connection, String selectSql) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement(selectSql);
			ResultSet resultSet = preparedStatement.executeQuery();
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			String[] columnArray = new String[columnCount];
			for (int dbIndex = 0, index = 1; dbIndex < columnCount; dbIndex++) {
				if (metaData.isAutoIncrement(dbIndex + 1)) {
					columnArray[0] = metaData.getColumnName(dbIndex + 1);
				} else {
					columnArray[index++] = metaData.getColumnName(dbIndex + 1);
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
