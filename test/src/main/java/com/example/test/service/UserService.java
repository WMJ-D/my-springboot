package com.example.test.service;

import com.alibaba.excel.EasyExcel;
import com.example.test.entity.User;
import com.example.test.entity.UserExcel;
import com.example.test.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 查询 id > 10 的用户（使用自定义 SQL）
     */
    public List<User> findAll() {
        return userMapper.selectByIdGreaterThan(10);
    }

    /**
     * 根据条件查询用户（动态 SQL）
     */
    public List<User> findByCondition(String name, Integer minId) {
        return userMapper.selectByCondition(name, minId);
    }

    /**
     * 根据 ID 查询用户
     */
    public User findById(Integer id) {
        return userMapper.selectById(id);
    }

    /**
     * 新增用户
     */
    public User save(User user) {
        userMapper.insert(user);
        return user;
    }

    /**
     * 删除用户
     */
    public void deleteById(Integer id) {
        userMapper.deleteById(id);
    }

    /**
     * 导出用户信息为 Excel
     */
    public void exportExcel(HttpServletResponse response) {
        try {
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("用户信息", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

            // 查询数据
            List<User> userList = userMapper.selectByIdGreaterThan(10);

            // 转换为 Excel 对象
            List<UserExcel> excelList = userList.stream().map(user -> {
                UserExcel excel = new UserExcel();
                BeanUtils.copyProperties(user, excel);
                return excel;
            }).collect(Collectors.toList());

            // 写入 Excel
            EasyExcel.write(response.getOutputStream(), UserExcel.class)
                    .sheet("用户信息")
                    .doWrite(excelList);

        } catch (Exception e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }
}
