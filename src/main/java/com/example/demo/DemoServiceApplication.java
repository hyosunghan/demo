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
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:test;mode=mysql", "sa", "sa");
		long l = System.currentTimeMillis();

		IntStream.range(0, page).parallel().forEach(p -> {
			String values = IntStream.range(0, limit).parallel().mapToObj(i -> {
				String username = String.valueOf(System.currentTimeMillis());
				return String.format("('%s', '%s')", username, EncryptCommon.encrypt(username));
			}).collect(Collectors.joining(","));
			String insertSql = String.format("INSERT INTO users (username, password) VALUES %s;", values);
			execute(connection, insertSql);
		});
		long l1 = System.currentTimeMillis();
		log.info("新增{}数据，耗时{}毫秒。。。", limit * page, l1 - l); // 0.9s

		IntStream.range(0, 1000).parallel().forEach(p -> {
			String selectSql = String.format("SELECT * FROM users LIMIT %s,%s", p * limit, limit);
			List<String[]> pageResult = executeQuery(connection, selectSql);
			String updateItem = "UPDATE users SET username='%s', password='%s' WHERE id=%s;";
			String updateSql = pageResult.parallelStream().map(line ->
					String.format(updateItem, EncryptCommon.encrypt(line[1]), line[2], line[0]))
					.collect(Collectors.joining());
			execute(connection, updateSql);
			log.info("查询并更新第{}页", p);
		});
		long l2 = System.currentTimeMillis();
		log.info("查询并更新{}数据，耗时{}毫秒。。。", limit * page, l2 - l1); // 77.5s

		List<List<String[]>> pageCollect = IntStream.range(0, page).parallel().mapToObj(p -> {
			String selectSql = String.format("SELECT * FROM users LIMIT %s,%s", p * limit, limit);
			return executeQuery(connection, selectSql);
		}).collect(Collectors.toList());
		pageCollect.parallelStream().forEach(pageResult -> {
			String ids = pageResult.parallelStream().map(line -> line[0])
					.collect(Collectors.joining(",", "(", ")"));
			String deleteSql = String.format("DELETE FROM users WHERE id in %s;", ids);
			execute(connection, deleteSql);
			String values = pageResult.parallelStream().map(line ->
							String.format("(%s, '%s', '%s')", line[0], EncryptCommon.encrypt(line[1]), line[2]))
					.collect(Collectors.joining(","));
			String insertSql = String.format("INSERT INTO users (id, username, password) VALUES %s;", values);
			execute(connection, insertSql);
			log.info("查询并删除并重写");
		});
		long l3 = System.currentTimeMillis();
		log.info("查询并删除并重写{}数据，耗时{}毫秒。。。", limit * page, l3 - l2); // 3.5s

		connection.close();
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
