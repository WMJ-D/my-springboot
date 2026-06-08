# my-springboot

一个基于 Spring Boot 3 的用户管理示例服务，包含 REST 接口、MyBatis-Plus 数据访问、MySQL 连接和 EasyExcel 用户信息导出能力。

## 功能概览

- 健康示例接口：访问 `/hello` 返回 Spring Boot 启动验证信息。
- 用户列表：查询 `id > 10` 的用户列表。
- 条件查询：按姓名关键字和最小 ID 动态筛选用户。
- 用户详情：根据用户 ID 查询单个用户。
- 新增用户：通过 JSON 请求体新增用户。
- 删除用户：根据 ID 删除用户。
- Excel 导出：将用户信息导出为 `.xlsx` 文件。

## 技术栈

- Java 17
- Spring Boot 3.2.5
- Spring Web
- MyBatis-Plus 3.5.5
- MySQL Connector/J
- EasyExcel 3.3.4
- Lombok
- Maven Wrapper

## 目录结构

```text
test/
  pom.xml                                      Maven 项目配置
  mvnw, mvnw.cmd                              Maven Wrapper
  init.sql                                    数据库初始化脚本
  src/main/java/com/example/test/
    TestApplication.java                      应用启动类
    HelloController.java                      hello 示例接口
    controller/UserController.java            用户 REST 接口
    service/UserService.java                  用户业务逻辑和 Excel 导出
    mapper/UserMapper.java                    MyBatis-Plus Mapper
    entity/User.java                          用户实体
    entity/UserExcel.java                     Excel 导出对象
  src/main/resources/
    application.properties                    服务端口、数据库和 MyBatis-Plus 配置
    mapper/UserMapper.xml                     自定义 SQL 和动态查询
  src/test/java/com/example/test/
    TestApplicationTests.java                 Spring Boot 上下文测试
```

## 快速开始

### 1. 准备数据库

默认连接配置位于 `test/src/main/resources/application.properties`：

```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456
```

请先在本地启动 MySQL，并根据实际账号密码调整配置。

当前 Java 实体和 Mapper 使用的用户字段为：

```sql
CREATE TABLE user (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  value VARCHAR(255)
);
```

项目内的 `init.sql` 可作为初始化参考；如果直接执行，请确保表字段与当前实体 `User(id, name, value)` 保持一致。

### 2. 启动服务

在仓库根目录执行：

```powershell
cd test
.\mvnw.cmd spring-boot:run
```

服务启动后默认访问地址为：

```text
http://localhost:8080
```

### 3. 运行测试

```powershell
cd test
.\mvnw.cmd test
```

## 接口说明

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/hello` | 返回 `Hello, Spring Boot!` |
| GET | `/api/user/list` | 查询 id 大于 10 的用户 |
| GET | `/api/user/search?name=张&minId=5` | 按姓名和最小 ID 动态查询 |
| GET | `/api/user/{id}` | 根据 ID 查询用户 |
| POST | `/api/user` | 新增用户 |
| DELETE | `/api/user/{id}` | 删除用户 |
| GET | `/api/user/export` | 导出用户 Excel |

新增用户示例：

```http
POST /api/user
Content-Type: application/json

{
  "name": "张三",
  "value": "示例值"
}
```

## 注意事项

- 当前项目是学习和示例性质的单体服务，尚未加入统一异常处理、参数校验、鉴权和接口文档。
- Excel 导出接口会直接写入 HTTP 响应流，适合浏览器或接口调试工具下载文件。
- 如果修改数据库字段，请同步调整 `User.java`、`UserExcel.java`、`UserMapper.xml` 和初始化 SQL。
