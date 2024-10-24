package com.example.demo;

import com.example.demo.interceptor.EncryptCommon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@SpringBootApplication
public class DemoServiceApplication implements ApplicationRunner {

	public static void main(String[] args) {
		SpringApplication.run(DemoServiceApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		int limit = 1000;
		int page = 100;
		Connection conn = DriverManager.getConnection("jdbc:h2:mem:test;mode=mysql", "sa", "sa");
		long l = System.currentTimeMillis();

		IntStream.range(0, page).parallel().forEach(p -> {
			String values = IntStream.range(0, limit).parallel().mapToObj(i -> {
				String username = String.valueOf(System.currentTimeMillis());
				return String.format("('%s', '%s')", username, EncryptCommon.encrypt(username));
			}).collect(Collectors.joining(","));
			String insertSql = String.format("INSERT INTO users (username, password) VALUES %s;", values);
			execute(conn, insertSql);
		});
		long l1 = System.currentTimeMillis();
		log.info("新增{}数据，耗时{}毫秒。。。", limit * page, l1 - l);

		List<String[]> strings = executeQuery(conn, "select count(1) from users");
		int count = Integer.parseInt(strings.get(0)[0]);
		page = (count / limit) + (count % limit == 0 ? 0 : 1);
		long l2 = System.currentTimeMillis();
		log.info("总数{}条, {}页", count, page);

		int sum1 = IntStream.range(0, page).parallel().map(p -> {
			String selectSql = String.format("SELECT * FROM users LIMIT %s,%s", p * limit, limit);
			List<String[]> pageResult = executeQuery(conn, selectSql);
			String updateItem = "UPDATE users SET username='%s', password='%s' WHERE id=%s;";
			String updateSql = pageResult.parallelStream().map(line ->
							String.format(updateItem, EncryptCommon.encrypt(line[1]), line[2], line[0]))
					.collect(Collectors.joining());
			execute(conn, updateSql);
			log.info("查询并更新第{}页{}条", p, pageResult.size());
			return pageResult.size();
		}).sum();
		long l3 = System.currentTimeMillis();
		log.info("查询并更新{}条数据，耗时{}毫秒。。。", sum1, l3 - l2);

		int sum2 = IntStream.range(0, page).parallel().mapToObj(p -> {
			String selectSql = String.format("SELECT id FROM users ORDER BY id ASC LIMIT %s,%s;", p * limit, limit);
			List<String[]> pageIds = executeQuery(conn, selectSql);
			return new Long[]{Long.valueOf(pageIds.get(0)[0]), Long.valueOf(pageIds.get(pageIds.size() - 1)[0])};
		}).collect(Collectors.toList()).parallelStream().mapToInt(range -> {
			String selectSql = String.format("SELECT * FROM users WHERE id BETWEEN %s AND %s;", range[0], range[1]);
			List<String[]> pageResult = executeQuery(conn, selectSql);
			String deleteSql = String.format("DELETE FROM users WHERE id BETWEEN %s AND %s;", range[0], range[1]);
			execute(conn, deleteSql);
			String values = pageResult.parallelStream().map(line ->
							String.format("(%s, '%s', '%s')", line[0], EncryptCommon.encrypt(line[1]), line[2]))
					.collect(Collectors.joining(","));
			String insertSql = String.format("INSERT INTO users (id, username, password) VALUES %s;", values);
			execute(conn, insertSql);
			log.info("查询并删除并重写{}-{}区间{}条", range[0], range[1], pageResult.size());
			return pageResult.size();
		}).sum();
		long l4 = System.currentTimeMillis();
		log.info("查询并删除重写{}条数据，耗时{}毫秒。。。", sum2, l4 - l3);

		conn.close();
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
